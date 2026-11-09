package com.intent.reader.sync

import com.intent.reader.data.db.AppDatabase
import com.intent.reader.data.db.Article
import com.intent.reader.data.db.Source
import com.intent.reader.data.repo.SourceRepository
import com.intent.reader.rss.HtmlText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches one source and persists its articles plus conditional-GET metadata.
 * Shared by [SyncWorker] and the subscribe flow, so a newly added source is
 * populated before the caller is told the subscription succeeded.
 */
class FeedSyncer(
    private val db: AppDatabase,
    private val fetcher: FeedFetcher,
) {
    sealed interface Outcome {
        /** Feed was fetched and parsed; [articles] is how many items it contained. */
        data class Ok(val articles: Int) : Outcome
        data object NotModified : Outcome
        data class Failed(val message: String, val kind: FeedFetcher.FailureKind) : Outcome
    }

    suspend fun sync(source: Source): Outcome = withContext(Dispatchers.IO) {
        when (val r = fetcher.fetch(source)) {
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
                if (entities.isNotEmpty()) db.articleDao().insertAll(entities)
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
                Outcome.Ok(entities.size)
            }

            FeedFetcher.FetchResult.NotModified -> {
                db.sourceDao().update(
                    source.copy(lastFetchedAt = System.currentTimeMillis(), lastError = null)
                )
                Outcome.NotModified
            }

            is FeedFetcher.FetchResult.Failed -> {
                db.sourceDao().update(source.copy(lastError = r.message.take(200)))
                Outcome.Failed(r.message, r.kind)
            }
        }
    }
}
