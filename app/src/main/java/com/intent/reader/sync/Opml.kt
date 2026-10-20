package com.intent.reader.sync

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * OPML 2.0 import/export for subscription lists.
 */
object Opml {

    data class Outline(
        val title: String,
        val xmlUrl: String,
        val htmlUrl: String?,
        val category: String?,
    )

    fun parse(xml: String): List<Outline> = try {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val p = factory.newPullParser()
        p.setInput(xml.reader())
        val out = mutableListOf<Outline>()
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType != XmlPullParser.START_TAG) continue
            if (!p.name.equals("outline", ignoreCase = true)) continue
            val xmlUrl = p.getAttributeValue(null, "xmlUrl") ?: continue
            if (xmlUrl.isBlank()) continue
            out += Outline(
                title = (p.getAttributeValue(null, "title")
                    ?: p.getAttributeValue(null, "text") ?: xmlUrl).trim(),
                xmlUrl = xmlUrl.trim(),
                htmlUrl = p.getAttributeValue(null, "htmlUrl")?.takeIf { it.isNotBlank() },
                category = p.getAttributeValue(null, "category")?.takeIf { it.isNotBlank() },
            )
        }
        out
    } catch (_: Exception) {
        emptyList()
    }

    fun export(outlines: List<Outline>, title: String = "INTENT subscriptions"): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<opml version=\"2.0\">\n")
        sb.append("  <head><title>").append(escape(title)).append("</title></head>\n")
        sb.append("  <body>\n")
        for (o in outlines) {
            sb.append("    <outline type=\"rss\" text=\"").append(escape(o.title))
                .append("\" title=\"").append(escape(o.title)).append("\"")
            if (o.htmlUrl != null) sb.append(" htmlUrl=\"").append(escape(o.htmlUrl)).append("\"")
            sb.append(" xmlUrl=\"").append(escape(o.xmlUrl)).append("\"")
            if (o.category != null) sb.append(" category=\"").append(escape(o.category)).append("\"")
            sb.append(" />\n")
        }
        sb.append("  </body>\n")
        sb.append("</opml>\n")
        return sb.toString()
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
