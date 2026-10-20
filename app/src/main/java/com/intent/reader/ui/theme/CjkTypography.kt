package com.intent.reader.ui.theme

/**
 * CJK typography normalization for the render layer only.
 * Inserts a space at boundaries between CJK characters (Han / Kana / Hangul)
 * and Latin letters or digits. Idempotent; never touches full-width
 * punctuation, percentages, units, URLs or code.
 */
object CjkTypography {

    private const val CJK_CLASS = "\\u4E00-\\u9FFF\\u3040-\\u30FF\\uAC00-\\uD7AF"
    private val cjkThenLatin = Regex("([$CJK_CLASS])([A-Za-z0-9])")
    private val latinThenCjk = Regex("([A-Za-z0-9])([$CJK_CLASS])")

    fun apply(text: String): String {
        if (text.isEmpty()) return text
        val step1 = latinThenCjk.replace(text) { m -> "${m.groupValues[1]} ${m.groupValues[2]}" }
        return cjkThenLatin.replace(step1) { m -> "${m.groupValues[1]} ${m.groupValues[2]}" }
    }
}

fun String.withCjkSpacing(): String = CjkTypography.apply(this)
