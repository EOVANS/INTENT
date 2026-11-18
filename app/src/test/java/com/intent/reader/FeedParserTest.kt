package com.intent.reader

import com.intent.reader.rss.FeedParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedParserTest {

    /** Channel-level <image> must not swallow the items that follow it. */
    @Test
    fun rssWithChannelImageStillParsesItems() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
<channel>
<title>Daily</title>
<link>https://example.com</link>
<atom:link href="https://example.com/feed.xml" rel="self" type="application/rss+xml"/>
<description>desc</description>
<image>
<url>https://example.com/cover.jpg</url>
<title>Daily</title>
<link>https://example.com</link>
</image>
<item><title>One</title><link>https://example.com/1</link><guid>1</guid></item>
<item><title>Two</title><link>https://example.com/2</link><guid>2</guid></item>
</channel>
</rss>"""
        val result = FeedParser.parse(StringReader(xml))
        assertTrue(result is FeedParser.ParseResult.Ok)
        val feed = (result as FeedParser.ParseResult.Ok).feed
        assertEquals("Daily", feed.title)
        assertEquals("https://example.com/cover.jpg", feed.iconUrl)
        assertEquals(2, feed.items.size)
        assertEquals("One", feed.items[0].title)
        assertEquals("Two", feed.items[1].title)
    }
}
