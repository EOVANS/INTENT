package com.intent.reader.domain

import java.time.LocalDate

/** One row of the home feed: either a single card (A-D) or an aggregate card (E). */
sealed interface FeedRow {
    val sortKey: Long

    data class Single(
        val articleId: String,
        val style: CardStyle,
        override val sortKey: Long,
    ) : FeedRow

    data class Aggregate(
        val sourceId: String,
        val articleIds: List<String>,
        val count: Int,
        override val sortKey: Long,
    ) : FeedRow
}

/**
 * Builds the home feed from recent articles (doc 4.1.2 + 5.6):
 * - articles sorted by publishedAt desc;
 * - same-source articles published today (>= dayStart) with count >= 2 are
 *   collapsed into one Aggregate row placed at the newest article's position;
 * - remaining articles get deterministic card styles.
 */
object HomeFeedBuilder {

    data class Inputs(
        val articleId: String,
        val guid: String,
        val sourceId: String,
        val publishedAt: Long,
        val hasImage: Boolean,
    )

    fun build(
        articles: List<Inputs>,
        dayStart: Long,
        dateKey: String,
        disabledStyles: Set<String> = emptySet(),
    ): List<FeedRow> {
        val sorted = articles.sortedByDescending { it.publishedAt }

        // Group today's articles per source
        val todayPerSource = sorted
            .filter { it.publishedAt >= dayStart }
            .groupBy { it.sourceId }
        val aggregateSources = todayPerSource
            .filter { it.value.size >= 2 }
            .keys

        // Eligible for A/B/C hash: has image, not part of an aggregate
        val eligible = sorted.filter { it.hasImage && it.sourceId !in aggregateSources }
        val styles = CardStyleSelector.assignStyles(
            guids = eligible.map { it.guid },
            dateKey = dateKey,
            hasImage = eligible.map { true },
            disabled = disabledStyles,
        )
        val styleByArticle = eligible.indices.associate { eligible[it].articleId to styles[it] }

        return sorted.map { a ->
            if (a.sourceId in aggregateSources) {
                null
            } else {
                val style = styleByArticle[a.articleId] ?: CardStyle.D
                FeedRow.Single(a.articleId, style, a.publishedAt)
            }
        }.let { rows ->
            // Insert aggregate rows at the newest article position of each source
            val aggregates = aggregateSources.map { sourceId ->
                val list = todayPerSource.getValue(sourceId).sortedByDescending { it.publishedAt }
                FeedRow.Aggregate(sourceId, list.map { it.articleId }, list.size, list.first().publishedAt)
            }
            (rows.filterNotNull() + aggregates).sortedByDescending { it.sortKey }
        }
    }
}
