package com.intent.reader.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import android.graphics.drawable.BitmapDrawable
import com.intent.reader.IntentApplication
import com.intent.reader.widget.IntentWidgets
import com.intent.reader.widget.WidgetLoader
import com.intent.reader.widget.WidgetArtworkStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodic + on-demand feed sync. Fetches every source with conditional GET,
 * upserts articles, trims retention, computes today's quote.
 */
class SyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as IntentApplication).container
        val db = container.database
        val sources = db.sourceDao().getAll()
        if (sources.isEmpty()) return Result.success()

        // Fetch, parse and upsert each feed; lastError/lastFetchedAt live in FeedSyncer.
        sources.forEach { container.feedSyncer.sync(it) }

        // Retention cleanup
        val settings = container.settingsRepository.settings.first()
        val retainDays = settings.retainDays
        val perSourceKeep = settings.perSourceKeep
        db.articleDao().trimOlderThan(
            System.currentTimeMillis() - retainDays.toLong() * 24 * 3600 * 1000
        )
        db.articleDao().distinctSourceIds().forEach { sourceId ->
            db.articleDao().trimSourceKeep(sourceId, perSourceKeep)
        }

        // Today's quote (skip when no articles to save work)
        try {
            val builtInIds = db.sourceDao().getAll().filter { it.isBuiltIn }.map { it.id }.toSet()
            container.quoteRepository.ensureTodayComputed(isRecommendedSource = { it in builtInIds })
        } catch (_: Exception) {
            // quote computation must never fail the sync
        }
        // Fresh articles are in Room -> cache artwork and save widget snapshots
        // so the widget can render every page after ordinary caches are cleared.
        runCatching { prefetchWidgetImages() }
        // Re-render any placed home-screen widgets.
        runCatching { IntentWidgets.refreshAll(applicationContext) }
        return Result.success()
    }

    /**
     * Downloads the artwork of the eight articles the widget shows into Coil's
     * disk cache and the widget's bounded snapshots. The app may use the network
     * here; the widget itself only reads local artwork.
     */
    private suspend fun prefetchWidgetImages() {
        val articles = WidgetLoader.load(applicationContext).articles
        val urls = (articles.mapNotNull { it.imageUrl } + articles.mapNotNull { it.sourceLogoUrl })
            .distinct()
        val snapshots = WidgetArtworkStore(applicationContext)
        coroutineScope {
            urls.map { url ->
                async {
                    val request = ImageRequest.Builder(applicationContext)
                        .data(url)
                        .size(640, 640)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .allowHardware(false)
                        .build()
                    runCatching {
                        val result = applicationContext.imageLoader.execute(request) as? SuccessResult
                        val bitmap = (result?.drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null) withContext(Dispatchers.IO) { snapshots.save(url, bitmap) }
                    }
                }
            }.awaitAll()
        }
        withContext(Dispatchers.IO) { snapshots.retain(urls.toSet()) }
    }

    companion object {
        const val PERIODIC_WORK = "intent_periodic_sync"
        const val ONESHOT_WORK = "intent_manual_refresh"

        fun schedulePeriodic(context: Context, intervalMinutes: Int, wifiOnly: Boolean) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                intervalMinutes.toLong().coerceAtLeast(60), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun refreshNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONESHOT_WORK,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
