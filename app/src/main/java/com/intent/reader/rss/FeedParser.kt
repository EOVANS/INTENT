package com.intent.reader.rss

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader

/**
 * Lightweight feed parser based on XmlPullParser.
 * Supports RSS 2.0, Atom 1.0 and RDF (RSS 1.0). No external dependencies.
 */
object FeedParser {

    data class ParsedItem(
        val guid: String,
        val title: String,
        val link: String,
        val author: String?,
        val summaryHtml: String?,
        val contentHtml: String?,
        val imageUrl: String?,
        val publishedAt: Long,
    )

    data class ParsedFeed(
        val title: String,
        val siteUrl: String?,
        val iconUrl: String?,
        val items: List<ParsedItem>,
    )

    sealed interface ParseResult {
        data class Ok(val feed: ParsedFeed) : ParseResult
        data class Error(val message: String, val cause: Throwable? = null) : ParseResult
    }

    fun parse(reader: Reader): ParseResult = try {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(reader)
        parseRoot(parser)
    } catch (e: Exception) {
        ParseResult.Error("parse failed: ${e.message}", e)
    }

    private fun parseRoot(p: XmlPullParser): ParseResult {
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType != XmlPullParser.START_TAG) continue
            return when {
                p.name.equals("rss", ignoreCase = true) -> parseRss(p)
                p.name.equals("feed", ignoreCase = true) -> parseAtom(p)
                p.name.equals("RDF", ignoreCase = true) -> parseRdf(p)
                else -> { skip(p); continueParseRoot(p) }
            }
        }
        return ParseResult.Error("no feed root element found")
    }

    private fun continueParseRoot(p: XmlPullParser): ParseResult {
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType != XmlPullParser.START_TAG) continue
            return when {
                p.name.equals("rss", ignoreCase = true) -> parseRss(p)
                p.name.equals("feed", ignoreCase = true) -> parseAtom(p)
                p.name.equals("RDF", ignoreCase = true) -> parseRdf(p)
                else -> { skip(p); continueParseRoot(p) }
            }
        }
        return ParseResult.Error("no feed root element found")
    }

    // ---------- RSS 2.0 ----------

    /** p is at <rss>; consumes until </rss>. */
    private fun parseRss(p: XmlPullParser): ParseResult {
        var title = ""
        var siteUrl: String? = null
        var iconUrl: String? = null
        val items = mutableListOf<ParsedItem>()
        while (true) {
            val ev = p.next()
            if (ev == XmlPullParser.END_TAG) break
            if (ev == XmlPullParser.END_DOCUMENT) return ParseResult.Error("unexpected end of rss")
            if (ev != XmlPullParser.START_TAG) continue
            when (p.name.lowercase()) {
                "channel" -> {
                    while (true) {
                        val ev2 = p.next()
                        if (ev2 == XmlPullParser.END_TAG) break
                        if (ev2 == XmlPullParser.END_DOCUMENT) return ParseResult.Error("unexpected end of rss")
                        if (ev2 != XmlPullParser.START_TAG) continue
                        when (p.name.lowercase()) {
                            "title" -> title = p.nextText().trim()
                            "link" -> {
                                val t = p.nextText().trim()
                                if (t.isNotBlank()) siteUrl = t
                            }
                            "image" -> {
                                while (true) {
                                    val ev3 = p.next()
                                    if (ev3 == XmlPullParser.END_TAG) break
                                    if (ev3 == XmlPullParser.END_DOCUMENT) return ParseResult.Error("unexpected end of rss")
                                    if (ev3 == XmlPullParser.START_TAG && p.name.equals("url", true)) {
                                        iconUrl = p.nextText().trim()
                                    }
                                }
                            }
                            "item" -> parseRssItem(p)?.let(items::add)
                            else -> skip(p)
                        }
                    }
                }
                else -> skip(p)
            }
        }
        if (items.isEmpty() && title.isEmpty()) return ParseResult.Error("empty rss feed")
        return ParseResult.Ok(ParsedFeed(title, siteUrl, iconUrl, items))
    }

    /** p is at <item>; consumes until </item>. */
    private fun parseRssItem(p: XmlPullParser): ParsedItem? {
        var title = ""
        var link = ""
        var guid = ""
        var author: String? = null
        var description: String? = null
        var content: String? = null
        var image: String? = null
        var publishedAt = 0L
        while (!(p.eventType == XmlPullParser.END_TAG && p.name.equals("item", ignoreCase = true))) {
            p.next()
            if (p.eventType == XmlPullParser.END_DOCUMENT) break
            if (p.eventType != XmlPullParser.START_TAG) continue
            when (p.name.lowercase()) {
                "title" -> title = p.nextText().trim()
                "link" -> {
                    val t = p.nextText().trim()
                    if (t.isNotBlank()) link = t
                }
                "guid" -> guid = p.nextText().trim()
                "author", "dc:creator" -> author = p.nextText().trim().ifEmpty { null }
                "description" -> description = p.nextText()
                "encoded" -> content = p.nextText() // content:encoded with namespace-aware parser
                "pubdate" -> publishedAt = FeedDates.parse(p.nextText())
                "date" -> publishedAt = FeedDates.parse(p.nextText()) // dc:date
                "enclosure" -> {
                    if (p.getAttributeValue(null, "type")?.startsWith("image") == true) {
                        image = p.getAttributeValue(null, "url")
                    }
                    skip(p)
                }
                "content" -> { // media:content
                    val type = p.getAttributeValue(null, "type") ?: p.getAttributeValue(null, "medium")
                    val url = p.getAttributeValue(null, "url")
                    if (url != null && (type == null && image == null || type?.startsWith("image") == true)) {
                        image = url
                    }
                    skip(p)
                }
                "thumbnail" -> { // media:thumbnail
                    val url = p.getAttributeValue(null, "url")
                    if (url != null && image == null) image = url
                    skip(p)
                }
                else -> skip(p)
            }
        }
        if (title.isEmpty() && link.isEmpty()) return null
        if (guid.isEmpty()) guid = if (link.isNotBlank()) link else title
        return ParsedItem(
            guid = guid,
            title = HtmlText.toPlain(title),
            link = link,
            author = author,
            summaryHtml = description?.trim()?.takeIf { it.isNotEmpty() },
            contentHtml = content?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = image,
            publishedAt = publishedAt,
        )
    }

    // ---------- Atom 1.0 ----------

    /** p is at <feed>; consumes until </feed>. */
    private fun parseAtom(p: XmlPullParser): ParseResult {
        var title = ""
        var siteUrl: String? = null
        val items = mutableListOf<ParsedItem>()
        while (true) {
            val ev = p.next()
            if (ev == XmlPullParser.END_TAG) break
            if (ev == XmlPullParser.END_DOCUMENT) return ParseResult.Error("unexpected end of atom")
            if (ev != XmlPullParser.START_TAG) continue
            when (p.name.lowercase()) {
                "title" -> {
                    if (p.getAttributeValue(null, "type") == "xhtml") skip(p) else title = p.nextText().trim()
                }
                "link" -> {
                    val rel = p.getAttributeValue(null, "rel")
                    val href = p.getAttributeValue(null, "href")
                    if (href != null && (rel == null || rel == "alternate") && siteUrl == null) {
                        siteUrl = href
                    }
                    skip(p)
                }
                "icon", "logo" -> skip(p)
                "entry" -> parseAtomEntry(p)?.let(items::add)
                else -> skip(p)
            }
        }
        if (items.isEmpty() && title.isEmpty()) return ParseResult.Error("empty atom feed")
        return ParseResult.Ok(ParsedFeed(title, siteUrl, null, items))
    }

    /** p is at <entry>; consumes until </entry>. */
    private fun parseAtomEntry(p: XmlPullParser): ParsedItem? {
        var title = ""
        var link = ""
        var guid = ""
        var author: String? = null
        var summary: String? = null
        var content: String? = null
        var image: String? = null
        var publishedAt = 0L
        while (!(p.eventType == XmlPullParser.END_TAG && p.name.equals("entry", ignoreCase = true))) {
            p.next()
            if (p.eventType == XmlPullParser.END_DOCUMENT) break
            if (p.eventType != XmlPullParser.START_TAG) continue
            when (p.name.lowercase()) {
                "title" -> title = p.nextText().trim()
                "id" -> guid = p.nextText().trim()
                "link" -> {
                    val rel = p.getAttributeValue(null, "rel")
                    val href = p.getAttributeValue(null, "href")
                    when {
                        rel == null || rel == "alternate" -> if (link.isEmpty() && href != null) link = href
                        rel == "enclosure" && href != null -> {
                            val type = p.getAttributeValue(null, "type")
                            if (type?.startsWith("image") == true && image == null) image = href
                        }
                    }
                    skip(p)
                }
                "author" -> author = readAtomPerson(p)
                "summary" -> summary = p.nextText()
                "content" -> {
                    val type = p.getAttributeValue(null, "type")
                    val text = p.nextText()
                    if (type == null || type == "html" || type == "text") content = text
                }
                "published" -> publishedAt = FeedDates.parse(p.nextText())
                "updated" -> if (publishedAt == 0L) publishedAt = FeedDates.parse(p.nextText())
                "thumbnail" -> {
                    if (image == null) image = p.getAttributeValue(null, "url")
                    skip(p)
                }
                "content" -> { /* media:content handled above; safe guard */ skip(p) }
                else -> skip(p)
            }
        }
        if (title.isEmpty() && link.isEmpty()) return null
        if (guid.isEmpty()) guid = if (link.isNotBlank()) link else title
        return ParsedItem(
            guid = guid,
            title = HtmlText.toPlain(title),
            link = link,
            author = author,
            summaryHtml = summary?.trim()?.takeIf { it.isNotEmpty() },
            contentHtml = content?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = image,
            publishedAt = publishedAt,
        )
    }

    private fun readAtomPerson(p: XmlPullParser): String? {
        var name: String? = null
        while (!(p.eventType == XmlPullParser.END_TAG && p.name.equals("author", ignoreCase = true))) {
            p.next()
            if (p.eventType == XmlPullParser.END_DOCUMENT) break
            if (p.eventType == XmlPullParser.START_TAG && p.name.equals("name", ignoreCase = true)) {
                name = p.nextText().trim()
            }
        }
        return name?.takeIf { it.isNotEmpty() }
    }

    // ---------- RDF / RSS 1.0 ----------

    /** p is at <RDF>; consumes until </RDF>. Items live under the RDF root, channel only holds metadata. */
    private fun parseRdf(p: XmlPullParser): ParseResult {
        var title = ""
        var siteUrl: String? = null
        val items = mutableListOf<ParsedItem>()
        while (true) {
            val ev = p.next()
            if (ev == XmlPullParser.END_TAG) break
            if (ev == XmlPullParser.END_DOCUMENT) return ParseResult.Error("unexpected end of rdf")
            if (ev != XmlPullParser.START_TAG) continue
            when {
                p.name.equals("channel", ignoreCase = true) -> {
                    while (true) {
                        val ev2 = p.next()
                        if (ev2 == XmlPullParser.END_TAG) break
                        if (ev2 == XmlPullParser.END_DOCUMENT) return ParseResult.Error("unexpected end of rdf")
                        if (ev2 != XmlPullParser.START_TAG) continue
                        when (p.name.lowercase()) {
                            "title" -> title = p.nextText().trim()
                            "link" -> {
                                val t = p.nextText().trim()
                                if (t.isNotBlank()) siteUrl = t
                            }
                            else -> skip(p)
                        }
                    }
                }
                p.name.equals("item", ignoreCase = true) -> parseRssItem(p)?.let(items::add)
                else -> skip(p)
            }
        }
        if (items.isEmpty() && title.isEmpty()) return ParseResult.Error("empty rdf feed")
        return ParseResult.Ok(ParsedFeed(title, siteUrl, null, items))
    }

    // ---------- utils ----------

    private fun skip(p: XmlPullParser) {
        var depth = 1
        while (depth != 0) {
            when (p.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }
}
