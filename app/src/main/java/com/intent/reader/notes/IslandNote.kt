package com.intent.reader.notes

import com.intent.reader.rss.HtmlText
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

@Serializable
data class IslandNote(
    val id: String,
    val articleId: String,
    val articleTitle: String,
    val articleUrl: String,
    val sourceTitle: String,
    val paragraphKey: String,
    val createdAt: Long,
    val updatedAt: Long,
    @Transient val quote: String = "",
    @Transient val remark: String = "",
    val imageUrl: String? = null,
    val handwritingRevision: String? = null,
)

data class NoteParagraph(val key: String, val text: String)

/** A single visible note consists of all saved paragraphs from one article. */
data class NoteArticle(val articleId: String, val paragraphs: List<IslandNote>) {
    val latest: IslandNote get() = paragraphs.maxBy { it.updatedAt }
    val updatedAt: Long get() = latest.updatedAt
    val imageUrl: String? get() = paragraphs.asReversed().firstNotNullOfOrNull { it.imageUrl?.takeIf(String::isNotBlank) }
    val preview: String get() = paragraphs.firstOrNull { it.remark.isNotBlank() }?.remark ?: paragraphs.first().quote
}

object NoteArticles {
    fun group(notes: List<IslandNote>): List<NoteArticle> = notes.groupBy { it.articleId }
        .map { (articleId, paragraphs) -> NoteArticle(articleId, paragraphs.sortedBy { it.createdAt }) }
        .sortedByDescending { it.updatedAt }

    /** Unmatched paragraphs stay available, in creation order, if the article changed or expired. */
    fun order(notes: List<IslandNote>, blocks: List<HtmlText.Block>): List<IslandNote> {
        val positions = NoteParagraphs.from(blocks).entries.associate { it.value.key to it.key }
        return notes.sortedWith(compareBy<IslandNote> { positions[it.paragraphKey] ?: Int.MAX_VALUE }.thenBy { it.createdAt })
    }

    /** Count displayed excerpt and note text, excluding whitespace, as Unicode code points. */
    fun characterCount(notes: List<IslandNote>): Long = notes.sumOf { note ->
        (note.quote + note.remark).codePoints().filter { !Character.isWhitespace(it) }.count()
    }
}

/** Keys refer to original text, before visual CJK spacing, and distinguish repeated paragraphs. */
object NoteParagraphs {
    fun from(blocks: List<HtmlText.Block>): Map<Int, NoteParagraph> {
        val occurrences = mutableMapOf<String, Int>()
        return buildMap {
            blocks.forEachIndexed { index, block ->
                if (block is HtmlText.Block.Para) {
                    val text = block.text.trim()
                    val occurrence = occurrences.getOrDefault(text, 0)
                    occurrences[text] = occurrence + 1
                    val hash = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
                        .joinToString("") { "%02x".format(it) }
                    put(index, NoteParagraph("$hash:$occurrence", text))
                }
            }
        }
    }
}

/** Metadata and human-readable text live in the same Markdown file, with no sidecar database. */
object NoteMarkdown {
    private val json = Json { ignoreUnknownKeys = true }
    private const val HEADER = "<!-- intent-note-v1 "
    private const val QUOTE = "\n## 摘录\n\n"
    private const val REMARK = "\n\n## 备注\n\n"

    fun encode(note: IslandNote): String = buildString {
        // Prevent titles/URLs from closing the metadata's HTML comment.
        val metadata = json.encodeToString(note).replace("<", "\\u003c").replace(">", "\\u003e")
        append(HEADER).append(metadata).append(" -->\n\n")
        append("# ").append(oneLine(note.articleTitle)).append("\n\n")
        append(oneLine(note.sourceTitle)).append(" · ")
        append(java.time.Instant.ofEpochMilli(note.createdAt)).append("\n\n")
        append(oneLine(note.articleUrl)).append('\n')
        append(QUOTE)
        append(note.quote.lineSequence().joinToString("\n") { "> $it" })
        append(REMARK).append(note.remark).append('\n')
    }

    /** Readable grouped export; each embedded metadata comment preserves its paragraph identity. */
    fun encodeArticle(notes: List<IslandNote>): String = buildString {
        require(notes.isNotEmpty() && notes.all { it.articleId == notes.first().articleId })
        val latest = notes.maxBy { it.updatedAt }
        append("# ").append(oneLine(latest.articleTitle)).append("\n\n")
        append(oneLine(latest.sourceTitle)).append("\n\n").append(oneLine(latest.articleUrl)).append("\n\n")
        notes.forEach { note ->
            append(HEADER).append(json.encodeToString(note).replace("<", "\\u003c").replace(">", "\\u003e"))
                .append(" -->\n\n")
            append(note.quote.lineSequence().joinToString("\n") { "> $it" }).append("\n\n")
            if (note.remark.isNotBlank()) append(note.remark).append("\n\n")
        }
    }

    fun decode(markdown: String): IslandNote {
        val text = markdown.replace("\r\n", "\n")
        val header = text.lineSequence().first()
        require(header.startsWith(HEADER) && header.endsWith(" -->"))
        val metadata = json.decodeFromString<IslandNote>(header.removePrefix(HEADER).removeSuffix(" -->"))
        require(runCatching { java.util.UUID.fromString(metadata.id).toString() == metadata.id }.getOrDefault(false))
        require(metadata.articleId.isNotBlank() && metadata.paragraphKey.isNotBlank())
        require(metadata.createdAt >= 0 && metadata.updatedAt >= metadata.createdAt)
        val quoteStart = text.indexOf(QUOTE).also { require(it >= 0) } + QUOTE.length
        val remarkStart = text.indexOf(REMARK, quoteStart).also { require(it >= quoteStart) }
        val quote = text.substring(quoteStart, remarkStart).lineSequence().joinToString("\n") {
            require(it.startsWith("> "))
            it.removePrefix("> ")
        }
        require(quote.isNotBlank())
        return metadata.copy(quote = quote, remark = text.substring(remarkStart + REMARK.length).removeSuffix("\n"))
    }

    private fun oneLine(text: String) = text.replace(Regex("[\r\n]+"), " ")
}
