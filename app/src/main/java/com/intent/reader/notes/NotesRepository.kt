package com.intent.reader.notes

import android.util.AtomicFile
import com.intent.reader.data.db.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

data class NotesState(val notes: List<IslandNote> = emptyList(), val loaded: Boolean = false, val unreadableFiles: Int = 0)

/** App-owned local Markdown, excluded from ordinary caches and Android backup by its caller. */
class NotesRepository(private val directory: File, private val now: () -> Long = System::currentTimeMillis) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(NotesState())
    val state = mutableState.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) { mutex.withLock { loadLocked() } }

    suspend fun save(article: Article, sourceTitle: String, paragraph: NoteParagraph, remark: String) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                loadLocked()
                require(paragraph.text.isNotBlank() && paragraph.key.isNotBlank())
                val existing = state.value.notes.firstOrNull {
                    it.articleId == article.id && it.paragraphKey == paragraph.key
                }
                val timestamp = now()
                val note = existing?.copy(remark = remark, updatedAt = maxOf(timestamp, existing.updatedAt),
                    imageUrl = existing.imageUrl ?: article.imageUrl)
                    ?: IslandNote(UUID.randomUUID().toString(), article.id, article.title, article.link,
                        sourceTitle, paragraph.key, timestamp, timestamp, paragraph.text, remark, imageUrl = article.imageUrl)
                writeLocked(note)
            }
        }

    suspend fun update(id: String, remark: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadLocked()
            val note = state.value.notes.first { it.id == id }
            writeLocked(note.copy(remark = remark, updatedAt = maxOf(now(), note.updatedAt)))
        }
    }

    /** Write a new immutable revision before committing its reference in the note. */
    suspend fun updateHandwriting(id: String, drawing: Handwriting?) = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadLocked()
            val note = state.value.notes.first { it.id == id }
            val revision = drawing?.let {
                val bytes = HandwritingFormat.encode(it)
                val id = UUID.randomUUID().toString()
                val target = inkFile(note, id)
                if (!target.parentFile!!.isDirectory && !target.parentFile!!.mkdirs()) throw IOException("Cannot create handwriting directory")
                val atomic = AtomicFile(target)
                val stream = atomic.startWrite()
                try { stream.write(bytes); atomic.finishWrite(stream) }
                catch (error: Exception) { atomic.failWrite(stream); throw error }
                id
            }
            try {
                writeLocked(note.copy(handwritingRevision = revision, updatedAt = maxOf(now(), note.updatedAt)))
            } catch (error: Exception) {
                revision?.let { inkFile(note, it).delete() }
                throw error
            }
            note.handwritingRevision?.let { runCatching { inkFile(note, it).delete() } }
        }
    }

    suspend fun loadHandwriting(note: IslandNote): Handwriting? = withContext(Dispatchers.IO) {
        mutex.withLock {
            note.handwritingRevision?.let { revision ->
                val file = inkFile(note, revision)
                require(file.length() <= HandwritingFormat.MAX_BYTES)
                HandwritingFormat.decode(AtomicFile(file).openRead().use { it.readBytes() })
            }
        }
    }

    private fun inkFile(note: IslandNote, revision: String): File {
        require(UUID.fromString(revision).toString() == revision)
        return File(File(fileFor(note).parentFile, "handwriting"), "${note.id}-$revision.json")
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            loadLocked()
            val note = state.value.notes.first { it.id == id }
            val file = fileFor(note)
            if (file.exists() && !file.delete()) throw IOException("Cannot delete note")
            note.handwritingRevision?.let { runCatching { inkFile(note, it).delete() } }
            mutableState.value = state.value.copy(notes = state.value.notes.filterNot { it.id == id })
        }
    }

    private fun loadLocked() {
        if (state.value.loaded) return
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create notes directory")
        var unreadable = 0
        val notes = directory.walkTopDown().onFail { _, error -> throw error }
            .filter { it.isFile && it.extension == "md" }.mapNotNull { file ->
                runCatching {
                    val note = AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { NoteMarkdown.decode(it.readText()) }
                    require(file.canonicalFile == fileFor(note).canonicalFile)
                    note
                }.getOrElse { unreadable++; null }
            }.toList()
        mutableState.value = NotesState(notes.sortedByDescending { it.createdAt }, true, unreadable)
    }

    private fun writeLocked(note: IslandNote) {
        val target = fileFor(note)
        if (!target.parentFile!!.isDirectory && !target.parentFile!!.mkdirs()) throw IOException("Cannot create notes month")
        val file = AtomicFile(target)
        val output = file.startWrite()
        try {
            output.write(NoteMarkdown.encode(note).toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
        mutableState.value = state.value.copy(
            notes = (state.value.notes.filterNot { it.id == note.id } + note).sortedByDescending { it.createdAt },
        )
    }

    private fun fileFor(note: IslandNote): File {
        require(UUID.fromString(note.id).toString() == note.id)
        val month = YearMonth.from(Instant.ofEpochMilli(note.createdAt).atZone(ZoneOffset.UTC))
        return File(File(directory, month.toString()), "${note.id}.md")
    }
}
