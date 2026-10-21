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
import com.intent.reader.IntentApplication
import com.intent.reader.data.db.Article
import com.intent.reader.data.repo.SourceRepository
import com.intent.reader.rss.HtmlText
import com.intent.reader.widget.IntentWidgets
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

        for (source in sources) {
            when (val r = container.feedFetcher.fetch(source)) {
                is FeedFetcher.FetchResult.Ok -> {
                    val now = System.currentTimeMillis()
                    val entities = r.feed.items.map { item ->
                        val summaryPlain = item.summaryHtml?.let { HtmlText.toPlain(it) }?.take(400)
                        val image = item.imageUrl
                            ?: HtmlText.firstImageUrl(item.contentHtml)
                            ?: item.summaryHtml?.let { HtmlText.firstImageUrl(it) }
                        val guid = item.guid.ifBlank { item.link.ifBlank { item.title } }
                        Article(
                            id = SourceRepository.idFor(guid),
                            sourceId = source.id,
                            guid = guid,
                            title = item.title,
                            link = item.link,
                            author = item.author,
                            summary = summaryPlain,
                            contentHtml = item.contentHtml ?: item.summaryHtml,
                            imageUrl = image,
                            publishedAt = if (item.publishedAt > 0) item.publishedAt else now,
                            fetchedAt = now,
                            cardStyle = null,
                        )
                    }
                    db.articleDao().insertAll(entities)
                    db.sourceDao().update(
                        source.copy(
                            etag = r.etag,
                            lastModified = r.lastModified,
                            lastFetchedAt = now,
                            lastError = null,
                            title = if (source.title == source.feedUrl && r.feed.title.isNotBlank())
                                r.feed.title else source.title,
                            siteUrl = source.siteUrl ?: r.feed.siteUrl,
                            logoUrl = source.logoUrl ?: r.feed.iconUrl,
                        )
                    )
                }
                is FeedFetcher.FetchResult.NotModified -> {
                    db.sourceDao().update(source.copy(lastFetchedAt = System.currentTimeMillis()))
                }
                is FeedFetcher.FetchResult.Failed -> {
                    db.sourceDao().update(
                        source.copy(lastError = r.message.take(200))
                    )
                }
            }
        }

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
        // Fresh articles are in Room -> re-render any placed home-screen widgets.
        runCatching { IntentWidgets.refreshAll(applicationContext) }
        return Result.success()
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
