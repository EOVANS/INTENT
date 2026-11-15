package com.intent.reader

import com.intent.reader.notes.NoteParagraphs
import com.intent.reader.rss.HtmlText
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlHeadingTest {
    @Test
    fun headingsKeepTheirLevelAndOrderAmongParagraphsAndImages() {
        val blocks = HtmlText.toBlocks(
            "<div><H2 class='section'>Section <em>one</em></H2><p>Body</p>" +
                "<img src='https://example.org/image.png'><h3>Next &amp; more</h3><p>End</p></div>"
        )
        assertEquals(
            listOf(
                HtmlText.Block.Para("Section one", 2),
                HtmlText.Block.Para("Body"),
                HtmlText.Block.Img("https://example.org/image.png", null),
                HtmlText.Block.Para("Next & more", 3),
                HtmlText.Block.Para("End"),
            ), blocks
        )
    }

    @Test
    fun headingNotesKeepTheSameKeyAsBefore() {
        val heading = NoteParagraphs.from(HtmlText.toBlocks("<h2>Heading</h2>"))
        val paragraph = NoteParagraphs.from(HtmlText.toBlocks("<p>Heading</p>"))
        assertEquals(paragraph.getValue(0).key, heading.getValue(0).key)
    }
}
