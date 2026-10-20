package com.intent.reader

import com.intent.reader.domain.CardStyle
import com.intent.reader.domain.CardStyleSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Card style selector tests (doc 5.6): deterministic, no triple repeats, D fallback. */
class CardStyleSelectorTest {

    private val dateKey = "2026-10-19"

    @Test
    fun `same guid and date yields same style`() {
        val s1 = CardStyleSelector.assignStyles(listOf("guid-1"), dateKey, listOf(true))
        val s2 = CardStyleSelector.assignStyles(listOf("guid-1"), dateKey, listOf(true))
        assertEquals(s1, s2)
    }

    @Test
    fun `different guid can yield different style distribution`() {
        val guids = (1..100).map { "guid-$it" }
        val styles = CardStyleSelector.assignStyles(guids, dateKey, List(100) { true })
        val distinct = styles.toSet()
        // With 100 samples all three image styles should appear
        assertTrue(distinct.contains(CardStyle.A))
        assertTrue(distinct.contains(CardStyle.B))
        assertTrue(distinct.contains(CardStyle.C))
    }

    @Test
    fun `no image falls back to D regardless of hash`() {
        val styles = CardStyleSelector.assignStyles(
            listOf("g1", "g2", "g3"), dateKey, listOf(false, false, false),
        )
        assertEquals(listOf(CardStyle.D, CardStyle.D, CardStyle.D), styles)
    }

    @Test
    fun `no three identical styles in a row`() {
        val guids = (1..500).map { "feed-item-$it" }
        val styles = CardStyleSelector.assignStyles(guids, dateKey, List(500) { true })
        for (i in 2 until styles.size) {
            assertTrue(
                "triple repeat at $i: ${styles[i - 2]}, ${styles[i - 1]}, ${styles[i]}",
                !(styles[i] == styles[i - 1] && styles[i - 1] == styles[i - 2]),
            )
        }
    }

    @Test
    fun `hash is stable across calls`() {
        val h1 = CardStyleSelector.hashOf("guid-42", dateKey)
        val h2 = CardStyleSelector.hashOf("guid-42", dateKey)
        assertEquals(h1, h2)
    }

    @Test
    fun `different date can change style`() {
        val s1 = CardStyleSelector.assignStyles(listOf("guid-1"), "2026-10-19", listOf(true))
        val s2 = CardStyleSelector.assignStyles(listOf("guid-1"), "2026-10-20", listOf(true))
        // Not guaranteed different for a single guid, but across 50 guids most change
        val g = (1..50).map { "guid-$it" }
        val d1 = CardStyleSelector.assignStyles(g, "2026-10-19", List(50) { true })
        val d2 = CardStyleSelector.assignStyles(g, "2026-10-20", List(50) { true })
        val changed = d1.zip(d2).count { it.first != it.second }
        assertTrue("expected most styles to change across dates, changed=$changed", changed > 10)
        // Sanity: single-guid comparisons stay valid list sizes
        assertEquals(1, s1.size)
        assertEquals(1, s2.size)
    }

    @Test
    fun `disabled styles removed from pool`() {
        val guids = (1..200).map { "d-$it" }
        val styles = CardStyleSelector.assignStyles(
            guids, dateKey, List(200) { true }, disabled = setOf("A", "B"),
        )
        assertTrue(styles.none { it == CardStyle.A })
        assertTrue(styles.none { it == CardStyle.B })
    }

    @Test
    fun `ratio roughly favors A and C over B`() {
        val guids = (1..1000).map { "r-$it" }
        val styles = CardStyleSelector.assignStyles(guids, dateKey, List(1000) { true })
        val countA = styles.count { it == CardStyle.A }
        val countB = styles.count { it == CardStyle.B }
        val countC = styles.count { it == CardStyle.C }
        // Weighted pool A:A:B:C:C = 2:1:2 — B should be the rarest in aggregate
        assertTrue("B=$countB A=$countA C=$countC", countB < countA && countB < countC)
        assertTrue("B=$countB", countB > 0)
    }
}
