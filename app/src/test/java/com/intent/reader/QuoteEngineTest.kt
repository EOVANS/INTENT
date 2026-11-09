package com.intent.reader

import com.intent.reader.quote.QuoteEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Quote engine tests (doc section 6): 20+ samples across eligibility,
 * scoring, splitting, highlight extraction and end-to-end generation.
 */
class QuoteEngineTest {

    private fun article(
        id: String,
        title: String,
        summary: String? = null,
        recommended: Boolean = false,
        sourceId: String = "s1",
        sourceTitle: String = "The Verge",
        contentHtml: String? = null,
    ) = QuoteEngine.CandidateArticle(
        articleId = id, sourceId = sourceId, sourceTitle = sourceTitle,
        isRecommendedSource = recommended, title = title, summary = summary,
        contentHtml = contentHtml,
    )

    // ---------- splitSentences ----------

    @Test
    fun `split CJK by full stop`() {
        val s = QuoteEngine.splitSentences("第一句。第二句。第三句")
        assertEquals(listOf("第一句。", "第二句。", "第三句"), s)
    }

    @Test
    fun `split latin by dot only when followed by space`() {
        val s = QuoteEngine.splitSentences("Go to the U.S.A today.")
        assertEquals(listOf("Go to the U.S.A today."), s)
    }

    @Test
    fun `split latin normal sentences`() {
        val s = QuoteEngine.splitSentences("Hello world. Next one! Third?")
        assertEquals(listOf("Hello world.", "Next one!", "Third?"), s)
    }

    @Test
    fun `split on newlines and semicolons`() {
        val s = QuoteEngine.splitSentences("A；B\nC")
        assertEquals(listOf("A；", "B", "C"), s)
    }

    @Test
    fun `split retains closing quotation marks`() {
        assertEquals(
            listOf("他说：“自由来自对生活的认真理解。”", "另一句话。"),
            QuoteEngine.splitSentences("他说：“自由来自对生活的认真理解。”另一句话。"),
        )
    }

    // ---------- isEligible ----------

    @Test
    fun `eligible CJK sentence within length`() {
        val t = "设计的本质是秩序，而非装饰，克制往往比堆砌更有力量。"
        assertTrue(QuoteEngine.isEligible(t, isTitle = false))
    }

    @Test
    fun `reject too short CJK`() {
        assertFalse(QuoteEngine.isEligible("好的。", isTitle = false))
    }

    @Test
    fun `reject too long CJK`() {
        val long = "长".repeat(130) + "。"
        assertFalse(QuoteEngine.isEligible(long, isTitle = false))
    }

    @Test
    fun `eligible english sentence ten to twenty five words`() {
        val t = "The best way to predict the future is to invent it and show it to people who care"
        assertTrue(QuoteEngine.isEligible(t, isTitle = false))
    }

    @Test
    fun `reject short english`() {
        assertFalse(QuoteEngine.isEligible("Nice try friend", isTitle = false))
    }

    @Test
    fun `reject question sentence`() {
        assertFalse(QuoteEngine.isEligible("你认为什么是好的设计呢？这个问题值得思考，也需要不断实践。", isTitle = false))
    }

    @Test
    fun `reject exclamation sentence`() {
        assertFalse(QuoteEngine.isEligible("太棒了，这个设计真的很棒，令人印象深刻，值得学习。！", isTitle = false))
    }

    @Test
    fun `reject url containing sentence`() {
        val t = "阅读本文的完整内容请访问 https://example.com/article 获取更多详细信息与背景。"
        assertFalse(QuoteEngine.isEligible(t, isTitle = false))
    }

    @Test
    fun `reject ad pattern`() {
        val t = "点击订阅我们的频道，第一时间获取最新内容推送，立即关注不走丢。"
        assertFalse(QuoteEngine.isEligible(t, isTitle = false))
    }

    @Test
    fun `reject caption pattern`() {
        val t = "图片来源： unsplash 的一张非常好看的照片，拍摄于去年秋天。"
        assertFalse(QuoteEngine.isEligible(t, isTitle = false))
    }

    @Test
    fun `title eligible with shorter length`() {
        assertTrue(QuoteEngine.isEligible("专注阅读，从今天开始", isTitle = true))
    }

    // ---------- scoreSentence ----------

    @Test
    fun `quoted sentence scores higher`() {
        val quoted = "他说：“少即是多”，这句话贯穿了整个现代设计史的发展脉络。"
        val plain = "少即是多的理念贯穿了整个现代设计史的发展脉络，影响深远。"
        assertTrue(QuoteEngine.scoreSentence(quoted, false) > QuoteEngine.scoreSentence(plain, false))
    }

    @Test
    fun `numeric sentence gets penalty`() {
        val numeric = "营收增长百分之三十点五，股价上涨两块五，市值达到新高点。"
        val plain = "设计的本质是秩序，而非装饰，克制往往比堆砌更有力量。"
        assertTrue(QuoteEngine.scoreSentence(numeric, false) < QuoteEngine.scoreSentence(plain, false))
    }

    @Test
    fun `first sentence bonus flag is accepted`() {
        val t = "设计的本质是秩序，而非装饰，克制往往比堆砌更有力量。"
        val first = QuoteEngine.scoreSentence(t, isFirst = true)
        val notFirst = QuoteEngine.scoreSentence(t, isFirst = false)
        assertEquals(first, notFirst) // bonus applied outside scoreSentence
    }

    // ---------- extractHighlights ----------

    @Test
    fun `extract quoted phrase`() {
        val s = "他始终坚持“少即是多”的原则。"
        val hs = QuoteEngine.extractHighlights(s)
        assertTrue(hs.isNotEmpty())
        assertEquals("少即是多", s.substring(hs[0].start, hs[0].end))
    }

    @Test
    fun `extract lexicon words sorted and non overlapping`() {
        val s = "自由不是随心所欲，而是自我主宰；时间是每个人最宝贵的资源。"
        val hs = QuoteEngine.extractHighlights(s)
        assertTrue(hs.isNotEmpty())
        assertTrue(hs.size <= 3)
        for (h in hs) assertTrue(h.end <= s.length)
        for (i in 1 until hs.size) assertFalse(hs[i - 1].end > hs[i].start)
        assertTrue(hs.all { s.substring(it.start, it.end).isNotBlank() })
    }

    @Test
    fun `highlights within one third of length`() {
        val s = "他说：“专注是一种美德”，这句话让复杂的问题变得简单清晰。"
        val hs = QuoteEngine.extractHighlights(s)
        assertTrue(hs.sumOf { it.end - it.start } <= s.length / 3 + 1)
    }

    @Test
    fun `no highlights for sentence without signals`() {
        val hs = QuoteEngine.extractHighlights("这一篇文章并没有什么值得划线的关键词或短语存在。")
        // Either empty or only short lexicon hits; must not crash and must be valid ranges
        assertTrue(hs.all { it.start in 0 until it.end && it.end <= 25 })
    }

    // ---------- generate (end to end) ----------

    @Test
    fun `generate picks best scoring candidate`() {
        val result = QuoteEngine.generate(
            listOf(
                article("a1", "Some boring headline", "股价上涨百分之五，市值突破一万亿美元大关。"),
                article(
                    "a2", "Focus matters",
                    "他说：“专注是一种美德”，这句话值得反复品味，也让复杂的问题变得简单。",
                    recommended = true,
                ),
            )
        )
        assertFalse(result.isFallback)
        assertEquals("a2", result.articleId)
        assertNotNull(result.sourceTitle)
        assertTrue(result.highlights.isNotEmpty())
    }

    @Test
    fun `generate falls back to article title when nothing scores`() {
        val result = QuoteEngine.generate(
            listOf(article("a1", "Market movers: stocks, bonds, and commodities today")),
        )
        assertFalse(result.isFallback)
        assertEquals("a1", result.articleId)
    }

    @Test
    fun `generate falls back to builtin when no articles`() {
        val result = QuoteEngine.generate(emptyList())
        assertTrue(result.isFallback)
        assertNull(result.articleId)
        assertNull(result.sourceTitle)
        assertTrue(result.text.isNotBlank())
    }

    @Test
    fun `generate is deterministic for same input`() {
        val input = listOf(
            article("a1", "Design is order", "设计的本质是秩序，而非装饰，克制往往比堆砌更有力量，值得我们学习。"),
            article("a2", "Less but better", "他说：“少即是多”，这句话贯穿了整个现代设计史的发展脉络。"),
        )
        val r1 = QuoteEngine.generate(input)
        val r2 = QuoteEngine.generate(input)
        assertEquals(r1.text, r2.text)
        assertEquals(r1.highlights, r2.highlights)
    }

    @Test
    fun `recommended source breaks ties between excerpts`() {
        val summary = "设计的本质是秩序，而非装饰，克制往往比堆砌更有力量。"
        val result = QuoteEngine.generate(listOf(
            article("a1", "T1", summary),
            article("a2", "T2", summary, recommended = true),
        ))
        assertEquals("a2", result.articleId)
        assertEquals(summary, result.text)
    }

    @Test
    fun `body excerpt wins over catchy titles and summaries from all articles`() {
        val body = "我们沿着河岸慢慢前行，观察两侧建筑与道路之间的细微变化。"
        val result = QuoteEngine.generate(listOf(
            article("headline", "自由与未来，改变世界的勇气与希望",
                "他说：“自由是一种力量”，让我们拥有改变未来和世界的勇气。", recommended = true),
            article("body", "普通的行走记录", contentHtml = "<p>$body</p>"),
        ))
        assertEquals("body", result.articleId)
        assertEquals(body, result.text)
    }

    @Test
    fun `summary excerpt wins over headline even without opinion keywords`() {
        val summary = "我们沿着河岸慢慢前行，观察两侧建筑与道路之间的细微变化。"
        assertEquals(summary, QuoteEngine.generate(listOf(
            article("a1", "自由与未来，改变世界的勇气与希望", summary),
        )).text)
    }

    @Test
    fun `html paragraphs are not joined into fabricated excerpts`() {
        val summary = "我们沿着河岸慢慢前行，观察两侧建筑与道路之间的细微变化。"
        val result = QuoteEngine.generate(listOf(article(
            "a1", "行走记录", summary,
            contentHtml = "<p>我们沿着河岸慢慢前行</p><p>观察两侧建筑与道路</p>",
        )))
        assertEquals(summary, result.text)
    }

    @Test
    fun `body parsing removes scripts and decodes entities`() {
        val sentence = "Knowledge & wisdom grow when we take the time to understand other people."
        val result = QuoteEngine.generate(listOf(article(
            "a1", "A headline",
            contentHtml = "<script>\nFreedom and hope change the world for everyone who believes in the future.\n</script>" +
                "<p>Knowledge &amp; wisdom grow when we take the <b>time</b> to understand other people.</p>",
        )))
        assertEquals(sentence, result.text)
    }

    @Test
    fun `body questions exclamations and ads are rejected before fallback`() {
        val summary = "设计的本质是秩序，而非装饰，克制往往比堆砌更有力量。"
        val result = QuoteEngine.generate(listOf(article(
            "a1", "一份普通文章的标题", summary,
            contentHtml = "<p>我们是否能够通过持续学习改变未来，并让所有人都拥有更多自由与希望？</p>" +
                "<p>持续学习能够改变未来，让所有人都拥有更多自由与希望！</p>" +
                "<p>订阅我们的频道，第一时间获取关于未来和自由的深刻观点。</p>",
        )))
        assertEquals(summary, result.text)
    }

    @Test
    fun `repeated heading is not treated as body excerpt`() {
        val title = "自由与未来，改变世界的勇气与希望始终来自持续学习"
        val body = "我们沿着河岸慢慢前行，观察两侧建筑与道路之间的细微变化。"
        assertEquals(body, QuoteEngine.generate(listOf(article(
            "a1", title, contentHtml = "<h1>$title</h1><p>$body</p>",
        ))).text)
    }

    @Test
    fun `rotation uses distinct body excerpts instead of titles or summaries`() {
        val first = "设计的本质是秩序，而非装饰，克制往往比堆砌更有力量。"
        val second = "我们沿着河岸慢慢前行，观察两侧建筑与道路之间的细微变化。"
        val summary = "他说：“自由是一种力量”，让我们拥有改变未来和世界的勇气。"
        val ranked = QuoteEngine.ranked(listOf(
            article("a1", "自由与未来，改变世界的勇气与希望", summary,
                contentHtml = "<p>$first</p><p>$second</p>"),
            article("a2", "自由与未来，改变世界的勇气与希望", summary,
                contentHtml = "<p>$second</p>"),
        ), 6)
        assertEquals(listOf(first, second), ranked.map { it.text })
        assertEquals(listOf("a1", "a2"), ranked.map { it.articleId })
    }
}
