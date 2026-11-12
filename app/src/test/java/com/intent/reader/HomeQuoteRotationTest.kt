package com.intent.reader

import com.intent.reader.data.db.DailyQuote
import com.intent.reader.ui.home.mergeHomeQuotes
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeQuoteRotationTest {
    private fun quote(text: String) = DailyQuote("2026-11-12", text, null, null, null, null, null)

    @Test fun aiQuoteJoinsRotationAndDuplicatesAreRemoved() {
        val first = quote("First")
        val ai = quote("AI")
        val last = quote("Last")
        assertEquals(listOf(ai, first, last), mergeHomeQuotes(listOf(first, ai, last), ai))
        assertEquals(listOf(first, last), mergeHomeQuotes(listOf(first, last), null))
        assertEquals(listOf(ai), mergeHomeQuotes(emptyList(), ai))
    }
}
