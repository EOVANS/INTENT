package com.intent.reader

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.intent.reader.data.CacheCleaner
import com.intent.reader.data.db.Article
import com.intent.reader.notes.IslandNote
import com.intent.reader.notes.NoteMarkdown
import com.intent.reader.notes.NoteArticles
import com.intent.reader.notes.NoteParagraph
import com.intent.reader.notes.NoteParagraphs
import com.intent.reader.notes.NotesRepository
import com.intent.reader.rss.HtmlText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class IslandNotesTest {
    @get:Rule val temporary = TemporaryFolder()
    private val article = Article("article-1", "source-1", "guid", "标题 / title", "https://example.invalid/article", null,
        "摘要", "<p>读书让我们看见更大的世界。</p>", null, 0, 0, cardStyle = null)
    private val paragraph = NoteParagraph("paragraph:0", "读书让我们看见更大的世界。")
    private val timestamp = 1793097600000L

    @Test fun markdownRoundTripPreservesUnicodeQuotesAndMarkdownInRemarks() {
        val note = IslandNote(UUID.randomUUID().toString(), article.id, "标题 -->\n## 摘录", article.link, "来源 & 🌊",
            "key:0", timestamp, timestamp,
            "原文🌊\n> 原文里的引用\n## 备注\n中文与English", "# 自己的标题\n\n- 保留 **Markdown**\n\n## 备注\n\n第二段\n")
        val markdown = NoteMarkdown.encode(note)
        assertTrue(markdown.contains("> 原文🌊"))
        assertTrue(markdown.contains("- 保留 **Markdown**"))
        assertEquals(note, NoteMarkdown.decode(markdown))
        assertEquals(note, NoteMarkdown.decode(markdown.replace("\n", "\r\n")))
        assertEquals(1, markdown.lineSequence().first().windowed(3).count { it == "-->" })
    }

    @Test fun paragraphBoundariesPreserveHtmlBlocksAndInlineImages() {
        val blocks = HtmlText.toBlocks("<p>第一段\n <em>强调</em>。</p><p>第二段。</p><img src='https://example.invalid/a.png'><p>第三段<br>第四段</p><script>不应显示</script>")
        assertEquals(listOf("第一段 强调 。", "第二段。", "第三段", "第四段"), blocks.filterIsInstance<HtmlText.Block.Para>().map { it.text })
        assertTrue(blocks[2] is HtmlText.Block.Img)
        assertEquals(listOf("第一段", "第二段"), HtmlText.toBlocks("第一段\n\n第二段").filterIsInstance<HtmlText.Block.Para>().map { it.text })
    }

    @Test fun coverUrlPersistsAndLegacyMarkdownWithoutCoverStillLoads() = runBlocking {
        val directory = temporary.newFolder()
        val repository = NotesRepository(directory) { timestamp }
        val cover = "https://example.invalid/article-cover.jpg"
        repository.save(article.copy(imageUrl = cover), "来源", paragraph, "保留图片来源")
        val reopened = NotesRepository(directory)
        reopened.load()
        val note = reopened.state.value.notes.single()
        assertEquals(cover, note.imageUrl)
        reopened.update(note.id, "修改备注后图片仍在")
        assertEquals(cover, reopened.state.value.notes.single().imageUrl)
        val legacy = NoteMarkdown.encode(note.copy(imageUrl = null))
        assertFalse(legacy.contains("imageUrl"))
        assertNull(NoteMarkdown.decode(legacy).imageUrl)
        assertEquals(note.quote, NoteMarkdown.decode(legacy).quote)
    }

    @Test fun anchorsDistinguishDuplicateTextAndSurviveUnrelatedInsertions() {
        val before = NoteParagraphs.from(HtmlText.toBlocks("<p>重复。</p><p>不同。</p><p>重复。</p>"))
        val after = NoteParagraphs.from(HtmlText.toBlocks("<p>新段落。</p><p>重复。</p><p>不同。</p><p>重复。</p>"))
        assertNotEquals(before.getValue(0).key, before.getValue(2).key)
        assertEquals(before.getValue(0).key, after.getValue(1).key)
        assertEquals(before.getValue(2).key, after.getValue(3).key)
        assertNotEquals(before.getValue(0).key, NoteParagraphs.from(HtmlText.toBlocks("改过的内容。"))[0]!!.key)
    }

    @Test fun sameArticleNotesGroupAndFollowOriginalParagraphOrder() {
        val blocks = HtmlText.toBlocks("<p>第一段 🚀</p><p>第二段。</p>")
        val keys = NoteParagraphs.from(blocks)
        fun note(id: String, articleId: String, key: String, quote: String, remark: String, created: Long, updated: Long) =
            IslandNote(id, articleId, "标题", "https://example.invalid", "来源", key, created, updated, quote, remark)
        val first = note(UUID.randomUUID().toString(), "article-1", keys.getValue(0).key, "第一段 🚀", "想法一", 20, 22)
        val second = note(UUID.randomUUID().toString(), "article-1", keys.getValue(1).key, "第二段。", "想法二", 10, 21)
        val orphan = note(UUID.randomUUID().toString(), "article-1", "missing", "原文已变化", "保留", 5, 5)
        val other = note(UUID.randomUUID().toString(), "article-2", "another", "另一篇", "", 1, 30)

        val groups = NoteArticles.group(listOf(first, other, second, orphan))
        assertEquals(listOf("article-2", "article-1"), groups.map { it.articleId })
        val ordered = NoteArticles.order(groups.last().paragraphs, blocks)
        assertEquals(listOf(first.id, second.id, orphan.id), ordered.map { it.id })
        assertEquals(listOf(orphan.id, second.id, first.id), NoteArticles.order(groups.last().paragraphs, emptyList()).map { it.id })
        assertEquals(14L, NoteArticles.characterCount(listOf(first, second)))
        val export = NoteMarkdown.encodeArticle(ordered)
        assertEquals(1, export.lineSequence().count { it == "# 标题" })
        assertTrue(export.indexOf("第一段 🚀") < export.indexOf("想法一"))
        assertTrue(export.indexOf("想法一") < export.indexOf("第二段。"))
        assertTrue(export.indexOf("第二段。") < export.indexOf("想法二"))
    }

    @Test fun saveReopenUpdateAndDeleteUseMarkdownWithoutArticleDatabase() = runBlocking {
        val directory = temporary.newFolder()
        val repository = NotesRepository(directory) { timestamp }
        repository.save(article, "来源", paragraph, "我的 **思考**")
        val original = repository.state.value.notes.single()
        val files = directory.walkTopDown().filter { it.isFile }.toList()
        assertEquals(1, files.size)
        assertEquals("md", files.single().extension)
        assertTrue(files.single().readText().contains("我的 **思考**"))

        val reopened = NotesRepository(directory) { timestamp + 1_000 }
        reopened.load()
        assertEquals(original, reopened.state.value.notes.single())
        reopened.update(original.id, "更新后的备注")
        assertEquals(original.createdAt, reopened.state.value.notes.single().createdAt)
        assertEquals(timestamp + 1_000, reopened.state.value.notes.single().updatedAt)
        assertEquals(paragraph.text, reopened.state.value.notes.single().quote)
        reopened.delete(original.id)
        val afterDeletion = NotesRepository(directory)
        afterDeletion.load()
        assertTrue(afterDeletion.state.value.notes.isEmpty())
    }

    @Test fun concurrentSavesDoNotDuplicateTheSameArticleParagraph() = runBlocking {
        val repository = NotesRepository(temporary.newFolder()) { timestamp }
        (1..8).map { async { repository.save(article, "来源", paragraph, "备注 $it") } }.awaitAll()
        assertEquals(1, repository.state.value.notes.size)
        repository.save(article.copy(id = "article-2"), "来源", paragraph, "另一篇的同一句话")
        repository.save(article, "来源", paragraph.copy(key = "paragraph:1"), "本篇重复出现的段落")
        assertEquals(3, repository.state.value.notes.size)
    }

    @Test fun ordinaryCacheClearingNeverDeletesLocalNotes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val directory = File(context.noBackupFilesDir, "notes-test-${UUID.randomUUID()}")
        val repository = NotesRepository(directory) { timestamp }
        try {
            repository.save(article, "来源", paragraph, "")
            val temporaryCache = File(context.cacheDir, "notes-test.tmp").apply { writeText("temporary") }
            CacheCleaner.clear(context)
            assertFalse(temporaryCache.exists())
            val reopened = NotesRepository(directory)
            reopened.load()
            assertEquals(paragraph.text, reopened.state.value.notes.single().quote)
            repository.delete(repository.state.value.notes.single().id)
        } finally { directory.deleteRecursively() }
    }

    @Test fun damagedNotesAreKeptAndDoNotHideHealthyNotes() = runBlocking {
        val directory = temporary.newFolder()
        val repository = NotesRepository(directory) { timestamp }
        repository.save(article, "来源", paragraph, "有效笔记")
        val bad = File(directory, "damaged.md").apply { writeText("not a valid note") }
        val reopened = NotesRepository(directory)
        reopened.load()
        assertEquals(1, reopened.state.value.notes.size)
        assertEquals(1, reopened.state.value.unreadableFiles)
        assertEquals("not a valid note", bad.readText())
    }

    @Test fun failedWriteDoesNotClaimTheNoteWasSavedOrChangeExistingNotes() = runBlocking {
        val directory = temporary.newFolder()
        val repository = NotesRepository(directory) { timestamp }
        repository.load()
        File(directory, "2026-10").writeText("block month directory")
        try { repository.save(article, "来源", paragraph, "不能丢失"); fail("Expected write failure") }
        catch (_: java.io.IOException) { }
        assertTrue(repository.state.value.notes.isEmpty())
    }
}
