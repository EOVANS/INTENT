package com.intent.reader

import com.intent.reader.domain.CardStyle
import com.intent.reader.domain.FeedRow
import com.intent.reader.domain.HomeFeedBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Home feed builder tests (doc 4.1.2 + 5.6): aggregation + ordering + styles. */
class HomeFeedBuilderTest {

    private val dayStart = 1_760_000_000_000L // arbitrary "today" start
    private val dateKey = "2026-10-19"

    private fun input(
        id: String,
        source: String,
        publishedAt: Long,
        hasImage: Boolean = true,
    ) = HomeFeedBuilder.Inputs(
        articleId = id, guid = "g-$id", sourceId = source,
        publishedAt = publishedAt, hasImage = hasImage,
    )

    @Test
    fun `articles sorted by publishedAt descending`() {
        // distinct sources so no aggregation collapses rows
        val rows = HomeFeedBuilder.build(
            articles = listOf(
                input("a1", "s1", dayStart + 1000),
                input("a2", "s2", dayStart + 3000),
                input("a3", "s3", dayStart + 2000),
            ),
            dayStart = dayStart, dateKey = dateKey,
        )
        assertEquals(3, rows.size)
        assertTrue(rows[0].sortKey >= rows[1].sortKey && rows[1].sortKey >= rows[2].sortKey)
    }

    @Test
    fun `same source today with two articles aggregates into E row`() {
        val rows = HomeFeedBuilder.build(
            articles = listOf(
                input("a1", "s1", dayStart + 1000),
                input("a2", "s1", dayStart + 3000),
                input("a3", "s2", dayStart + 2000),
            ),
            dayStart = dayStart, dateKey = dateKey,
        )
        val aggregates = rows.filterIsInstance<FeedRow.Aggregate>()
        assertEquals(1, aggregates.size)
        assertEquals("s1", aggregates[0].sourceId)
        assertEquals(2, aggregates[0].count)
        assertEquals(listOf("a2", "a1"), aggregates[0].articleIds) // newest first
    }

    @Test
    fun `single article from a source today is not aggregated`() {
        val rows = HomeFeedBuilder.build(
            articles = listOf(
                input("a1", "s1", dayStart + 1000),
                input("a2", "s2", dayStart + 500),
            ),
            dayStart = dayStart, dateKey = dateKey,
        )
        assertTrue(rows.filterIsInstance<FeedRow.Aggregate>().isEmpty())
        assertEquals(2, rows.filterIsInstance<FeedRow.Single>().size)
    }

    @Test
    fun `articles before dayStart never aggregate`() {
        val rows = HomeFeedBuilder.build(
            articles = listOf(
                input("a1", "s1", dayStart - 2000),
                input("a2", "s1", dayStart - 1000),
                input("a3", "s1", dayStart + 100),
            ),
            dayStart = dayStart, dateKey = dateKey,
        )
        val aggregates = rows.filterIsInstance<FeedRow.Aggregate>()
        // only a3 is today, and a single today article does not aggregate
        assertTrue(aggregates.isEmpty())
        assertEquals(3, rows.filterIsInstance<FeedRow.Single>().size)
    }

    @Test
    fun `aggregate placed at newest article position`() {
        val rows = HomeFeedBuilder.build(
            articles = listOf(
                input("a1", "s1", dayStart + 1000),
                input("a2", "s1", dayStart + 3000), // newest overall
                input("a3", "s2", dayStart + 2000),
            ),
            dayStart = dayStart, dateKey = dateKey,
        )
        assertEquals(FeedRow.Aggregate::class, rows[0]::class)
        assertEquals(dayStart + 3000, rows[0].sortKey)
    }

    @Test
    fun `imageless article gets style D`() {
        val rows = HomeFeedBuilder.build(
            articles = listOf(input("a1", "s1", dayStart + 1000, hasImage = false)),
            dayStart = dayStart, dateKey = dateKey,
        )
        val single = rows[0] as FeedRow.Single
        assertEquals(CardStyle.D, single.style)
    }

    @Test
    fun `disabled styles never appear`() {
        val rows = HomeFeedBuilder.build(
            articles = (1..100).map { input("a$it", "s$it", dayStart + it) },
            dayStart = dayStart, dateKey = dateKey,
            disabledStyles = setOf("B"),
        )
        val styles = rows.filterIsInstance<FeedRow.Single>().map { it.style }
        assertTrue(styles.none { it == CardStyle.B })
    }

    @Test
    fun `aggregate counts only its own articles`() {
        val rows = HomeFeedBuilder.build(
            articles = listOf(
                input("a1", "s1", dayStart + 100),
                input("a2", "s1", dayStart + 200),
                input("a3", "s1", dayStart + 300),
                input("b1", "s2", dayStart + 400),
            ),
            dayStart = dayStart, dateKey = dateKey,
        )
        val agg = rows.filterIsInstance<FeedRow.Aggregate>().single()
        assertEquals(3, agg.count)
        assertEquals(listOf("a3", "a2", "a1"), agg.articleIds)
        // 3 same-source articles collapse into 1 aggregate row + 1 single row
        assertEquals(2, rows.size)
    }

    @Test
    fun `deterministic for identical input`() {
        val articles = listOf(
            input("a1", "s1", dayStart + 1000),
            input("a2", "s1", dayStart + 3000),
            input("a3", "s2", dayStart + 2000, hasImage = false),
        )
        val r1 = HomeFeedBuilder.build(articles, dayStart, dateKey)
        val r2 = HomeFeedBuilder.build(articles, dayStart, dateKey)
        assertEquals(r1, r2)
    }
}
