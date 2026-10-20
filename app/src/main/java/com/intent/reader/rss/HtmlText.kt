package com.intent.reader.rss

/**
 * Minimal HTML handling: strip tags, unescape entities, extract first image.
 * Pure Kotlin, no dependencies.
 */
object HtmlText {

    fun toPlain(html: String): String = stripTags(html).trim()

    fun stripTags(html: String): String {
        if (!html.contains('<') && !html.contains('&')) return html
        var s = html
        // Remove script/style blocks entirely
        s = s.replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), "")
        // <br> / block ends become space
        s = s.replace(Regex("(?i)<br\\s*/?>"), " ")
        s = s.replace(Regex("(?i)</(p|div|li|h[1-6]|tr)>"), " ")
        // Drop tags
        s = s.replace(Regex("<[^>]+>"), " ")
        // Collapse whitespace
        s = s.replace(Regex("\\s+"), " ")
        return unescapeEntities(s).trim()
    }

    fun firstImageUrl(html: String?): String? {
        if (html.isNullOrBlank()) return null
        Regex("(?i)<img[^>]+src=[\"']([^\"']+)[\"']").find(html)?.groupValues?.get(1)?.let { return it }
        return null
    }

    /** Structured body: ordered blocks of paragraph text and inline images. */
    sealed interface Block {
        data class Para(val text: String) : Block
        data class Img(val url: String, val alt: String?) : Block
    }

    /**
     * Parse article HTML into render blocks. Splits on <img> so images appear in
     * their original position between paragraphs. All other tags are stripped.
     */
    fun toBlocks(html: String?): List<Block> {
        if (html.isNullOrBlank()) return emptyList()
        val out = mutableListOf<Block>()
        val imgRe = Regex("(?is)<img\\b([^>]*)>")
        var cursor = 0
        for (m in imgRe.findAll(html)) {
            // Flush text before this <img>
            val pre = html.substring(cursor, m.range.first)
            val plain = stripTags(pre).trim()
            if (plain.isNotEmpty()) out += Block.Para(plain)
            // Extract src + alt from the attrs
            val attrs = m.groupValues[1]
            val src = Regex("(?i)src\\s*=\\s*[\"']([^\"']+)[\"']").find(attrs)?.groupValues?.get(1)
            val alt = Regex("(?i)alt\\s*=\\s*[\"']([^\"']*)[\"']").find(attrs)?.groupValues?.get(1)
            if (!src.isNullOrBlank() && (src.startsWith("http://") || src.startsWith("https://"))) {
                out += Block.Img(src, alt?.takeIf { it.isNotBlank() })
            }
            cursor = m.range.last + 1
        }
        // Trailing text
        val tail = stripTags(html.substring(cursor)).trim()
        if (tail.isNotEmpty()) out += Block.Para(tail)
        return out
    }

    fun unescapeEntities(text: String): String {
        if (!text.contains('&')) return text
        var sb: StringBuilder? = null
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            if (c == '&') {
                val semi = text.indexOf(';', i + 1)
                if (semi > i && semi - i <= 10) {
                    val entity = text.substring(i + 1, semi)
                    val decoded = decodeEntity(entity)
                    if (decoded != null) {
                        if (sb == null) sb = StringBuilder(n).append(text, 0, i)
                        sb.append(decoded)
                        i = semi + 1
                        continue
                    }
                }
            }
            if (sb != null) sb.append(c)
            i++
        }
        return sb?.toString() ?: text
    }

    private fun decodeEntity(entity: String): String? = when (entity.lowercase()) {
        "amp" -> "&"
        "lt" -> "<"
        "gt" -> ">"
        "quot" -> "\""
        "apos" -> "'"
        "nbsp" -> " "
        "hellip" -> "…"
        "mdash" -> "—"
        "ndash" -> "–"
        "ldquo" -> "“"
        "rdquo" -> "”"
        "lsquo" -> "‘"
        "rsquo" -> "’"
        "laquo" -> "«"
        "raquo" -> "»"
        "middot" -> "·"
        "deg" -> "°"
        "copy" -> "©"
        "reg" -> "®"
        "trade" -> "™"
        else -> {
            if (entity.length > 2 && (entity[0] == '#' || (entity[0] == 'x' && entity[1] == '#'))) {
                try {
                    val code = when {
                        entity[0] == '#' && entity[1] == 'x' -> entity.substring(2).toInt(16)
                        entity[0] == '#' && entity[1] == 'X' -> entity.substring(2).toInt(16)
                        entity[0] == '#' -> entity.substring(1).toInt()
                        else -> return null
                    }
                    String(Character.toChars(code))
                } catch (_: Exception) {
                    null
                }
            } else null
        }
    }
}
