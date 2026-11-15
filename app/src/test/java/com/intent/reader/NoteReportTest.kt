package com.intent.reader

import com.intent.reader.notes.IslandNote
import com.intent.reader.notes.NoteReport
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class NoteReportTest {
    private fun note(id: String, article: String, source: String, time: String) = IslandNote(
        id, article, "title", "url", source, id, Instant.parse(time).toEpochMilli(), Instant.parse(time).toEpochMilli(), "quote", "remark"
    )

    @Test fun countsArticlesOnceAndKeepsSourceSharesConsistent() {
        val notes = listOf(note("1", "a", "RSS A", "2026-01-01T00:00:00Z"),
            note("2", "a", "RSS A", "2026-01-02T00:00:00Z"),
            note("3", "b", "RSS B", "2026-01-02T00:00:00Z"))
        assertEquals(2, NoteReport.articleCount(notes))
        assertEquals(listOf("RSS A" to 1, "RSS B" to 1), NoteReport.sources(notes).map { it.title to it.count })
    }

    @Test fun usesLocalCreationDateAcrossMonthBoundary() {
        val note = note("1", "a", "RSS", "2026-01-31T17:00:00Z")
        assertEquals("2026-02-01", NoteReport.date(note, ZoneId.of("Asia/Hong_Kong")).toString())
        assertEquals("2026-01-31", NoteReport.date(note, ZoneId.of("UTC")).toString())
    }
}
