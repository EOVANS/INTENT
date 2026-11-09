package com.intent.reader

import com.intent.reader.notes.Handwriting
import com.intent.reader.notes.HandwritingFormat
import com.intent.reader.notes.InkPoint
import com.intent.reader.notes.InkStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HandwritingFormatTest {
    @Test fun noteMetadataKeepsOptionalHandwritingReference() {
        val id = java.util.UUID.randomUUID().toString()
        val revision = java.util.UUID.randomUUID().toString()
        val note = com.intent.reader.notes.IslandNote(id, "article", "title", "url", "source", "paragraph", 1, 1, "quote", "remark", handwritingRevision = revision)
        assertEquals(note, com.intent.reader.notes.NoteMarkdown.decode(com.intent.reader.notes.NoteMarkdown.encode(note)))
        val old = note.copy(handwritingRevision = null)
        assertEquals(old, com.intent.reader.notes.NoteMarkdown.decode(com.intent.reader.notes.NoteMarkdown.encode(old)))
    }
    @Test fun roundTripPreservesStrokes() {
        val ink = Handwriting(width = 400f, height = 800f, strokes = listOf(
            InkStroke(listOf(InkPoint(0f, 0f), InkPoint(120.5f, 799f))),
            InkStroke(listOf(InkPoint(400f, 800f))),
        ))
        assertEquals(ink, HandwritingFormat.decode(HandwritingFormat.encode(ink)))
    }

    @Test fun rejectsUnsupportedAndInvalidData() {
        val ink = Handwriting(width = 400f, height = 800f, strokes = emptyList())
        assertThrows(IllegalArgumentException::class.java) { HandwritingFormat.encode(ink.copy(version = 2)) }
        assertThrows(IllegalArgumentException::class.java) { HandwritingFormat.encode(ink.copy(strokes = listOf(InkStroke(listOf(InkPoint(Float.NaN, 0f)))))) }
        assertThrows(IllegalArgumentException::class.java) { HandwritingFormat.encode(ink.copy(strokes = listOf(InkStroke(listOf(InkPoint(401f, 0f)))))) }
        assertThrows(IllegalArgumentException::class.java) { HandwritingFormat.decode(ByteArray(HandwritingFormat.MAX_BYTES + 1)) }
    }
}
