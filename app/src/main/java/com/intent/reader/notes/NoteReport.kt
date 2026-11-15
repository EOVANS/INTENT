package com.intent.reader.notes

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReportSource(val title: String, val count: Int)

/** Counts article notes once per period, using the creation date of each saved excerpt. */
object NoteReport {
    fun date(note: IslandNote, zone: ZoneId): LocalDate = Instant.ofEpochMilli(note.createdAt).atZone(zone).toLocalDate()
    fun articleCount(notes: List<IslandNote>): Int = notes.distinctBy { it.articleId }.size
    fun sources(notes: List<IslandNote>): List<ReportSource> = notes
        .groupBy { it.articleId }.values.map { it.minBy { note -> note.createdAt }.sourceTitle.trim() }
        .groupingBy { it }.eachCount().map { ReportSource(it.key, it.value) }
        .sortedWith(compareByDescending<ReportSource> { it.count }.thenBy { it.title })
}
