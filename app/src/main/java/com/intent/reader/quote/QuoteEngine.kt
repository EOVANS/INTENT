package com.intent.reader.quote

import kotlin.math.max
import kotlin.math.min

/**
 * Rule-based daily quote engine (doc section 6). Pure functions, offline,
 * O(text size) — no ML models. Deterministic for a given article set.
 */
object QuoteEngine {

    private const val BEST_THRESHOLD = 5

    data class CandidateArticle(
        val articleId: String,
        val sourceId: String,
        val sourceTitle: String,
        val isRecommendedSource: Boolean,
        val title: String,
        val summary: String?, // plain text
    )

    data class Highlight(val start: Int, val end: Int) // end exclusive

    data class QuoteResult(
        val text: String,
        val highlights: List<Highlight>,
        val articleId: String?,
        val sourceId: String?,
        val sourceTitle: String?,
        val articleSummary: String?,
        val isFallback: Boolean,
    )

    private val fallbackQuotes = listOf(
        "Stay hungry, stay foolish.",
        "Less, but better.",
        "Reading is thinking with someone else's head.",
        "Simplicity is the ultimate sophistication.",
        "知之者不如好之者，好之者不如乐之者。",
        "学而不思则罔，思而不学则殆。",
        "The details are not the details. They make the design.",
        "Everything should be made as simple as possible.",
    )

    fun generate(articles: List<CandidateArticle>): QuoteResult {
        data class Scored(
            val text: String,
            val score: Int,
            val articleId: String,
            val sourceId: String,
            val sourceTitle: String,
            val articleSummary: String?,
        )

        val scored = mutableListOf<Scored>()

        for (article in articles) {
            // Title as candidate (title rewrite bonus +3)
            val title = article.title.trim()
            if (isEligible(title, isTitle = true)) {
                scored += Scored(
                    text = title,
                    score = scoreSentence(title, isFirst = true) + 3 +
                        (if (article.isRecommendedSource) 1 else 0),
                    articleId = article.articleId,
                    sourceId = article.sourceId,
                    sourceTitle = article.sourceTitle,
                    articleSummary = article.summary?.take(80),
                )
            }
            val summary = article.summary?.trim().orEmpty()
            if (summary.isNotEmpty()) {
                val sentences = splitSentences(summary)
                sentences.forEachIndexed { i, s ->
                    val clean = s.trim()
                    if (isEligible(clean, isTitle = false)) {
                        val first = i == 0
                        scored += Scored(
                            text = clean,
                            score = scoreSentence(clean, isFirst = first) +
                                (if (first) 3 else 0) +
                                (if (article.isRecommendedSource) 1 else 0),
                            articleId = article.articleId,
                            sourceId = article.sourceId,
                            sourceTitle = article.sourceTitle,
                            articleSummary = article.summary?.take(80),
                        )
                    }
                }
            }
        }

        val best = scored.maxByOrNull { it.score }
        if (best != null && best.score >= BEST_THRESHOLD) {
            val highlights = extractHighlights(best.text)
            return QuoteResult(
                text = best.text,
                highlights = highlights,
                articleId = best.articleId,
                sourceId = best.sourceId,
                sourceTitle = best.sourceTitle,
                articleSummary = best.articleSummary,
                isFallback = false,
            )
        }

        // Fallback 1: first article from the highest-weight source, use its title
        val fallbackArticle = articles.firstOrNull { it.isRecommendedSource } ?: articles.firstOrNull()
        if (fallbackArticle != null && fallbackArticle.title.isNotBlank()) {
            val t = truncate(fallbackArticle.title.trim())
            return QuoteResult(
                text = t,
                highlights = extractHighlights(t),
                articleId = fallbackArticle.articleId,
                sourceId = fallbackArticle.sourceId,
                sourceTitle = fallbackArticle.sourceTitle,
                articleSummary = fallbackArticle.summary?.take(80),
                isFallback = false,
            )
        }

        // Fallback 2: built-in copy, no source -> UI hides button & meta line
        val pick = fallbackQuotes[(articles.size + System.currentTimeMillis().toInt()) % fallbackQuotes.size]
        return QuoteResult(
            text = pick,
            highlights = emptyList(),
            articleId = null,
            sourceId = null,
            sourceTitle = null,
            articleSummary = null,
            isFallback = true,
        )
    }

    /** Sentence splitter: CJK and latin terminators, newlines. */
    fun splitSentences(text: String): List<String> {
        val parts = StringBuilder()
        val out = mutableListOf<String>()
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            when {
                c == '。' || c == '！' || c == '？' || c == '；' || c == '…' || c == '\n' || c == '！' -> {
                    flush(parts, out)
                }
                c == '.' || c == '!' || c == '?' || c == ';' -> {
                    // Latin terminator only when followed by space/end (avoid U.S., e.g.)
                    val next = if (i + 1 < n) text[i + 1] else ' '
                    if (next == ' ' || next == '　' || i + 1 >= n) {
                        flush(parts, out)
                    } else {
                        parts.append(c)
                    }
                }
                else -> parts.append(c)
            }
            i++
        }
        flush(parts, out)
        return out
    }

    private fun flush(buf: StringBuilder, out: MutableList<String>) {
        val s = buf.toString().trim()
        buf.clear()
        if (s.isNotEmpty()) out.add(s)
    }

    fun isEligible(sentence: String, isTitle: Boolean): Boolean {
        val s = sentence.trim()
        if (s.isEmpty()) return false
        val cjkCount = s.count { isCjk(it) }
        val isCjkSentence = cjkCount * 2 >= s.length // CJK-heavy
        if (isCjkSentence) {
            if (s.length !in 20..120) return isTitle && s.length in 8..120
        } else {
            val wordCount = s.split(Regex("\\s+")).size
            if (wordCount !in 10..25) return false
        }
        val lower = s.lowercase()
        if (SentimentLexicon.adPatterns.any { lower.contains(it.lowercase()) }) return false
        if (s.contains("http") || s.contains("www.")) return false
        if (SentimentLexicon.captionPatterns.any { lower.startsWith(it.lowercase()) }) return false
        if (s.contains('？') || s.contains('?')) return false       // questions are not quotes
        if (s.count { it == '!' || it == '！' } > 0) return false   // exclamations
        return true
    }

    fun scoreSentence(sentence: String, isFirst: Boolean): Int {
        var score = 0
        // Quotation marks: quoted opinions are likelier quotes
        if (SentimentLexicon.quoteMarks.any { sentence.contains(it) }) score += 2
        // Strong-opinion words
        val hits = SentimentLexicon.words.count { w ->
            sentence.contains(w, ignoreCase = sentence.all { it.code < 128 } == false || true) &&
                sentence.lowercase().contains(w.lowercase())
        }
        if (hits >= 1) score += 2
        if (hits >= 2) score += 1
        // Well-formed structure
        val cjkCount = sentence.count { isCjk(it) }
        val isCjkSentence = cjkCount * 2 >= sentence.length
        if (isCjkSentence) {
            val commas = sentence.count { it == '，' || it == ',' }
            if (sentence.length in 20..40 && commas in 1..2) score += 1
        } else {
            val words = sentence.split(Regex("\\s+")).filter { it.isNotBlank() }
            val avgLen = if (words.isEmpty()) 0.0 else words.sumOf { it.length }.toDouble() / words.size
            if (avgLen >= 5.5) score += 1
        }
        // Numeric/finance penalty
        if (isNumericLike(sentence)) score -= 3
        return score
    }

    private fun isNumericLike(s: String): Boolean {
        val digits = s.count { it.isDigit() }
        if (digits.toDouble() / max(s.length, 1) >= 0.4) return true
        if (s.contains('%') || s.contains('％')) return true
        if (Regex("\\d+\\s*[-–:：]\\s*\\d+").containsMatchIn(s)) return true // score like 3-2
        if (s.contains("美元") || s.contains("欧元") || s.contains("日元") || s.contains("股价")) return true
        if (Regex("\\$\\d").containsMatchIn(s)) return true
        return false
    }

    private fun isCjk(c: Char): Boolean =
        c.code in 0x4E00..0x9FFF || c.code in 0x3040..0x30FF || c.code in 0xAC00..0xD7AF

    /** Extract 1-3 keyword ranges for the red underline; empty when impossible. */
    fun extractHighlights(sentence: String): List<Highlight> {
        val maxLen = max(1, sentence.length / 3)
        val ranges = mutableListOf<Highlight>()
        var total = 0

        // 1) quoted phrases (2-8 chars inside quote marks)
        val quoteRegex = Regex("[“\"「‘]([^“”\"」‘’]{2,8})[”\"」’]")
        for (m in quoteRegex.findAll(sentence)) {
            val start = m.groupValues[1].let { sentence.indexOf(it, m.range.first) }
            if (start >= 0) {
                val r = Highlight(start, start + m.groupValues[1].length)
                if (r.end - r.start + total <= maxLen && ranges.none { overlap(it, r) }) {
                    ranges.add(r); total += r.end - r.start
                }
            }
        }

        // 2) lexicon hits, longest first, non-overlapping
        val found = SentimentLexicon.words
            .mapNotNull { w ->
                val idx = sentence.indexOf(w, ignoreCase = true)
                if (idx >= 0) Highlight(idx, idx + w.length) else null
            }
            .sortedByDescending { it.end - it.start }
        for (r in found) {
            if (ranges.size >= 3) break
            if (r.end - r.start + total <= maxLen && ranges.none { overlap(it, r) }) {
                ranges.add(r); total += r.end - r.start
            }
        }
        return ranges.sortedBy { it.start }.take(3)
    }

    private fun overlap(a: Highlight, b: Highlight): Boolean =
        a.start < b.end && b.start < a.end

    private fun truncate(text: String): String {
        val cjk = text.count { isCjk(it) }
        val isCjkText = cjk * 2 >= text.length
        return if (isCjkText) {
            if (text.length > 60) text.take(60) + "…" else text
        } else {
            val words = text.split(Regex("\\s+"))
            if (words.size > 25) words.take(25).joinToString(" ") + "…" else text
        }
    }
}
