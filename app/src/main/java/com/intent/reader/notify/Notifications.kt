package com.intent.reader.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.intent.reader.IntentApplication
import com.intent.reader.MainActivity
import com.intent.reader.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object NotificationChannels {
    const val SUMMARY = "daily_summary"
    const val LIVE_UPDATE = "live_update"

    fun ensure(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                SUMMARY, context.getString(R.string.channel_summary),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.channel_summary_desc) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                LIVE_UPDATE, context.getString(R.string.channel_live_update),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = context.getString(R.string.channel_live_update_desc) }
        )
    }

    fun canPostNotifications(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < 33
}

/**
 * Daily summary notification: articles today / unread / today's quote.
 * Scheduled daily at the user-configured time; respects quiet hours.
 */
class DailySummaryWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as IntentApplication).container
        val context = applicationContext
        val settings = container.settingsRepository.settings.first()
        NotificationChannels.ensure(context)

        if (!settings.dailySummaryEnabled || !NotificationChannels.canPostNotifications(context)) {
            reschedule(settings.dailySummaryHour, settings.dailySummaryMinute)
            return Result.success()
        }
        if (isQuietTime(settings.quietStartHour, settings.quietEndHour)) {
            reschedule(settings.dailySummaryHour, settings.dailySummaryMinute)
            return Result.success()
        }

        val db = container.database
        val dayStart = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val todayArticles = db.articleDao().getSince(dayStart)
        val todayCount = todayArticles.size
        val unread = todayArticles.count { !it.isRead }
        val quote = container.quoteRepository.getToday()

        val title = context.getString(R.string.daily_summary_title)
        val quoteLine = quote?.text?.take(60) ?: ""
        val text = context.getString(R.string.daily_summary_body, todayCount, unread) +
            if (quoteLine.isNotEmpty()) "\n“$quoteLine”" else ""

        if (NotificationChannels.canPostNotifications(context)) {
            val intent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, NotificationChannels.SUMMARY)
                .setSmallIcon(R.drawable.ic_stat_intent)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(1001, notification)
            } catch (_: SecurityException) {
            }
        }

        reschedule(settings.dailySummaryHour, settings.dailySummaryMinute)
        return Result.success()
    }

    private fun isQuietTime(startHour: Int, endHour: Int): Boolean {
        val now = LocalTime.now()
        val start = LocalTime.of(startHour, 0)
        val end = LocalTime.of(endHour, 0)
        return if (start <= end) now >= start && now < end
        else now >= start || now < end // overnight window e.g. 22:00-8:00
    }

    private fun reschedule(hour: Int, minute: Int) {
        scheduleDaily(applicationContext, hour, minute)
    }

    companion object {
        const val WORK_NAME = "daily_summary"

        fun scheduleDaily(context: Context, hour: Int, minute: Int) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(hour, minute)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next)
            val request = OneTimeWorkRequestBuilder<DailySummaryWorker>()
                .setInitialDelay(delay)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME, ExistingWorkPolicy.REPLACE, request
            )
        }
    }
}

/** User dismissed the live update; remember the date to not re-post today. */
class LiveUpdateDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as IntentApplication
                app.container.settingsRepository.setLiveUpdateDismissedDate(LocalDate.now().toString())
                LiveUpdate.cancel(context)
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * Live Update (promoted ongoing) notification for daily reading progress.
 * Android 16 (API 36) only — all APIs are no-ops below 36 (doc 4.3.3.2).
 * Progress bar: goal minutes = full bar.
 */
object LiveUpdate {
    private const val NOTIFICATION_ID = 2001

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= 36

    fun canPost(context: Context): Boolean {
        if (!isSupported()) return false
        return NotificationManagerCompat.from(context)
            .canPostPromotedNotifications()
    }

    /** Posts/updates the reading progress notification. */
    fun update(context: Context, minutesToday: Int, goalMinutes: Int) {
        if (!isSupported() || !canPost(context)) return
        if (goalMinutes <= 0) return

        val achieved = minutesToday >= goalMinutes
        val title = if (achieved) {
            context.getString(R.string.live_update_achieved)
        } else {
            context.getString(R.string.live_update_reading, minutesToday)
        }
        val percent = (minutesToday * 100 / goalMinutes).coerceIn(0, 100)

        val progressStyle = NotificationCompat.ProgressStyle()
            .setProgress(percent)

        val contentIntent = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissIntent = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, LiveUpdateDismissReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.LIVE_UPDATE)
            .setSmallIcon(R.drawable.ic_stat_intent)
            .setContentTitle(title)
            .setContentText(
                context.getString(R.string.live_update_progress, minutesToday, goalMinutes)
            )
            .setStyle(progressStyle)
            .setContentIntent(contentIntent)
            .setOngoing(!achieved)
            .setRequestPromotedOngoing(true)
            .setOnlyAlertOnce(true)
            .setDeleteIntent(dismissIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
        }
    }

    /** Completion state: stops ongoing once the goal is reached. */
    fun complete(context: Context, minutesToday: Int, goalMinutes: Int) {
        update(context, minutesToday, goalMinutes)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
