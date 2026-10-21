package com.intent.reader.data.repo

import android.content.Context
import android.net.Uri
import com.intent.reader.data.assets.RecommendedSource
import com.intent.reader.data.db.AppDatabase
import com.intent.reader.data.db.Source
import com.intent.reader.sync.Opml
import com.intent.reader.sync.SiteDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class SourceRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val client: okhttp3.OkHttpClient,
) {
    fun observeSources(): Flow<List<Source>> = db.sourceDao().observeAll()

    suspend fun getAll(): List<Source> = db.sourceDao().getAll()

    suspend fun isSubscribed(feedUrl: String): Boolean =
        db.sourceDao().getByFeedUrl(feedUrl) != null

    suspend fun getById(id: String): Source? = db.sourceDao().getById(id)

    /**
     * Adds a source from a feed URL or a site URL (auto-discovers the feed).
     * Returns the added Source, or null when discovery failed.
     */
    suspend fun addFromUrl(rawUrl: String): Result<Source> = withContext(Dispatchers.IO) {
        val url = SiteDiscovery.normalizeUrl(rawUrl)
        db.sourceDao().getByFeedUrl(url)?.let { return@withContext Result.success(it) }

        var feedUrl = url
        var siteUrl: String? = null
        var title: String? = null
        var iconUrl: String? = null

        // Try feed discovery first when it doesn't look like XML
        val info = SiteDiscovery.fetch(client, url).getOrNull()
            // Unreachable / HTTP error: there is nothing usable to subscribe to.
            ?: return@withContext Result.failure(IllegalStateException("site unreachable"))
        info.feedUrl?.let { feedUrl = it }
        info.siteUrl?.let { siteUrl = it }
        info.title?.let { title = it }
        info.iconUrl?.let { iconUrl = it }
        // Reached a web page that advertises no feed: subscribing is pointless.
        if (info.feedUrl == null && info.siteUrl != null) {
            return@withContext Result.failure(IllegalStateException("no feed found"))
        }

        db.sourceDao().getByFeedUrl(feedUrl)?.let { return@withContext Result.success(it) }

        val source = Source(
            id = idFor(feedUrl),
            title = title ?: feedUrl,
            feedUrl = feedUrl,
            siteUrl = siteUrl ?: guessSite(feedUrl),
            logoUrl = iconUrl ?: SiteDiscovery.appleTouchIcon(siteUrl ?: guessSite(feedUrl)),
            category = null,
            addedAt = System.currentTimeMillis(),
            sortOrder = db.sourceDao().getAll().size,
        )
        val rowId = db.sourceDao().insert(source)
        if (rowId == -1L) {
            // Unique conflict: fetch the existing one
            val existing = db.sourceDao().getByFeedUrl(feedUrl)
            return@withContext if (existing != null) Result.success(existing)
            else Result.failure(IllegalStateException("insert conflict"))
        }
        Result.success(source)
    }

    suspend fun addRecommended(rs: RecommendedSource): Result<Source> =
        addFromUrl(rs.feedUrl).map { s ->
            if (s.isBuiltIn.not() && s.title == rs.feedUrl) {
                val renamed = s.copy(title = rs.name, logoUrl = s.logoUrl ?: rs.logoUrl, isBuiltIn = true)
                db.sourceDao().update(renamed)
                renamed
            } else s
        }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        db.articleDao().deleteBySource(id)
        db.sourceDao().delete(id)
    }

    suspend fun rename(id: String, title: String) = db.sourceDao().rename(id, title)

    suspend fun setNotifyEnabled(id: String, enabled: Boolean) =
        db.sourceDao().setNotifyEnabled(id, enabled)

    suspend fun reorder(sources: List<Source>) = withContext(Dispatchers.IO) {
        sources.forEachIndexed { i, s -> db.sourceDao().setSortOrder(s.id, i) }
    }

    suspend fun importOpml(xml: String): Int = withContext(Dispatchers.IO) {
        val outlines = Opml.parse(xml)
        var added = 0
        val existing = db.sourceDao().getAll()
        val urls = existing.map { it.feedUrl }.toSet()
        for (o in outlines) {
            if (o.xmlUrl in urls) continue
            db.sourceDao().insert(
                Source(
                    id = idFor(o.xmlUrl),
                    title = o.title,
                    feedUrl = o.xmlUrl,
                    siteUrl = o.htmlUrl,
                    logoUrl = o.htmlUrl?.let { SiteDiscovery.appleTouchIcon(it) },
                    category = o.category,
                    addedAt = System.currentTimeMillis(),
                    sortOrder = existing.size + added,
                )
            )
            added++
        }
        added
    }

    suspend fun exportOpml(): String = withContext(Dispatchers.IO) {
        Opml.export(
            db.sourceDao().getAll().map { Opml.Outline(it.title, it.feedUrl, it.siteUrl, it.category) }
        )
    }

    /** Writes exported OPML through SAF uri. */
    suspend fun writeOpmlTo(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
                os.flush()
            } ?: return@withContext false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readTextFrom(uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
    } catch (_: Exception) {
        null
    }

    companion object {
        fun idFor(feedUrl: String): String = try {
            val md = MessageDigest.getInstance("MD5")
            md.digest(feedUrl.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }.take(12)
        } catch (_: Exception) {
            feedUrl.hashCode().toString(16)
        }

        fun guessSite(feedUrl: String): String =
            Regex("(https?://[^/]+)").find(feedUrl)?.value ?: feedUrl
    }
}
