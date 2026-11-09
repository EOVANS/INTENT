package com.intent.reader.data.repo

import com.intent.reader.data.db.AppDatabase
import com.intent.reader.data.db.DailyQuote
import com.intent.reader.quote.QuoteEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId

@Serializable
data class HighlightRange(val start: Int, val end: Int)

/**
 * Daily quote: computed once per day, cached in Room, recomputed on date change.
 */
class QuoteRepository(private val db: AppDatabase) {

    private val json = Json { ignoreUnknownKeys = true }

    fun observeToday(zone: ZoneId = ZoneId.systemDefault()): Flow<DailyQuote?> =
        db.dailyQuoteDao().observeByDate(todayKey(zone))

    suspend fun getToday(zone: ZoneId = ZoneId.systemDefault()): DailyQuote? =
        db.dailyQuoteDao().getByDate(todayKey(zone))

    /** Computes today's quote if missing. Returns the stored row. */
    suspend fun ensureTodayComputed(
        isRecommendedSource: (String) -> Boolean = { false },
        zone: ZoneId = ZoneId.systemDefault(),
    ): DailyQuote? {
        val dateKey = todayKey(zone)
        val cached = db.dailyQuoteDao().getByDate(dateKey)
        if (cached != null && cached.articleId != null) {
            // Retry legacy headline quotes so upgrading can show a body excerpt
            // today instead of keeping the old title until the next day.
            val article = cached.articleId?.let { db.articleDao().getById(it) }
            if (article == null || !article.title.trim().startsWith(cached.text.removeSuffix("…"))) {
                return cached
            }
        }

        val dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val articles = db.articleDao().getSince(dayStart).ifEmpty { db.articleDao().getRecent() }
        if (articles.isEmpty() && cached != null) return cached

        val candidates = articles.map { a ->
            QuoteEngine.CandidateArticle(
                articleId = a.id,
                sourceId = a.sourceId,
                sourceTitle = db.sourceDao().getById(a.sourceId)?.title ?: "",
                isRecommendedSource = isRecommendedSource(a.sourceId),
                title = a.title,
                summary = a.summary,
                contentHtml = a.contentHtml,
            )
        }
        val result = QuoteEngine.generate(candidates)
        val quote = DailyQuote(
            date = dateKey,
            text = result.text,
            sourceId = result.sourceId,
            articleId = result.articleId,
            highlights = json.encodeToString(
                result.highlights.map { HighlightRange(it.start, it.end) }
            ),
            sourceTitle = result.sourceTitle,
            articleSummary = result.articleSummary,
        )
        // Do not persist a built-in placeholder: a later sync should replace it.
        if (articles.isEmpty()) return quote
        db.dailyQuoteDao().upsert(quote)
        db.dailyQuoteDao().trimBefore(LocalDate.now(zone).minusDays(7).toString())
        return quote
    }

    /**
     * Today's quote list for home-header rotation: the stored daily quote first,
     * followed by the next best distinct candidates from today's articles. The
     * extra entries are computed on the fly (deterministic per article set) and
     * are not persisted — the daily_quotes table still holds the single daily row.
     */
    suspend fun todayCandidates(
        isRecommendedSource: (String) -> Boolean = { false },
        max: Int = 6,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DailyQuote> {
        val primary = ensureTodayComputed(isRecommendedSource, zone) ?: return emptyList()
        val dateKey = todayKey(zone)
        val dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val articles = db.articleDao().getSince(dayStart).ifEmpty { db.articleDao().getRecent() }
        if (articles.isEmpty()) return listOf(primary)

        val candidates = articles.map { a ->
            QuoteEngine.CandidateArticle(
                articleId = a.id,
                sourceId = a.sourceId,
                sourceTitle = db.sourceDao().getById(a.sourceId)?.title ?: "",
                isRecommendedSource = isRecommendedSource(a.sourceId),
                title = a.title,
                summary = a.summary,
                contentHtml = a.contentHtml,
            )
        }
        val out = mutableListOf(primary)
        for (r in QuoteEngine.ranked(candidates, max)) {
            if (r.text == primary.text) continue
            out += DailyQuote(
                date = dateKey,
                text = r.text,
                sourceId = r.sourceId,
                articleId = r.articleId,
                highlights = json.encodeToString(
                    r.highlights.map { HighlightRange(it.start, it.end) }
                ),
                sourceTitle = r.sourceTitle,
                articleSummary = r.articleSummary,
            )
            if (out.size >= max) break
        }
        return out
    }

    fun decodeHighlights(raw: String?): List<HighlightRange> = try {
        if (raw.isNullOrBlank()) emptyList() else json.decodeFromString<List<HighlightRange>>(raw)
    } catch (_: Exception) {
        emptyList()
    }

    companion object {
        fun todayKey(zone: ZoneId = ZoneId.systemDefault()): String = LocalDate.now(zone).toString()
    }
}
