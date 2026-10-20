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
        db.dailyQuoteDao().getByDate(dateKey)?.let { return it }

        val dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val articles = db.articleDao().getSince(dayStart)
        if (articles.isEmpty()) return null

        val candidates = articles.map { a ->
            QuoteEngine.CandidateArticle(
                articleId = a.id,
                sourceId = a.sourceId,
                sourceTitle = db.sourceDao().getById(a.sourceId)?.title ?: "",
                isRecommendedSource = isRecommendedSource(a.sourceId),
                title = a.title,
                summary = a.summary,
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
        db.dailyQuoteDao().upsert(quote)
        db.dailyQuoteDao().trimBefore(LocalDate.now(zone).minusDays(7).toString())
        return quote
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
