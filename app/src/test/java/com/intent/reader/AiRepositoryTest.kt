package com.intent.reader

import android.app.Application
import com.intent.reader.ai.AiCompletion
import com.intent.reader.ai.AiConfig
import com.intent.reader.ai.AiError
import com.intent.reader.ai.AiException
import com.intent.reader.ai.AiQuote
import com.intent.reader.ai.AiRepository
import com.intent.reader.ai.SummaryLanguage
import com.intent.reader.data.db.Article
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class AiRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()
    private var config = AiConfig(true, true, "https://example.com/v1", "test-model", "test-key-not-real")
    private var date = LocalDate.of(2026, 10, 26)
    private var calls = 0
    private var answer = "这是一段简明摘要。"
    private var input = ""
    private var systemPrompt = ""
    private val body = "设计的本质是秩序，而非装饰，克制往往比堆砌更有力量。"
    private val directory by lazy { temporary.newFolder() }
    private val completion = AiCompletion { _, system, user, _ -> calls++; systemPrompt = system; input = user; answer }
    private fun repository(client: AiCompletion = completion) = AiRepository({ config }, client, directory, { date })
    private fun article(id: String = "a1", text: String = "$body 我们沿着河岸慢慢前行，观察两侧建筑与道路之间的细微变化。") = Article(
        id, "source", id, "一篇关于设计的文章", "https://example.com/$id", null, "文章的RSS摘要。",
        "<p>$text</p>", null, 0, 0, cardStyle = null,
    )

    private suspend fun expect(reason: AiError, action: suspend () -> Unit) {
        try { action(); fail("Expected $reason") }
        catch (error: AiException) { assertEquals(reason, error.reason) }
    }

    @Test fun summaryCacheSurvivesRecreationAndRegenerationIsExplicit() = runBlocking {
        assertEquals(answer, repository().summary(article()))
        assertEquals(answer, repository().summary(article()))
        assertEquals(1, calls)
        repository().summary(article(), regenerate = true)
        assertEquals(2, calls)
    }

    @Test fun concurrentSummaryRequestsShareTheCachedResult() = runBlocking {
        val repo = repository()
        listOf(async { repo.summary(article()) }, async { repo.summary(article()) }).awaitAll()
        assertEquals(1, calls)
    }

    @Test fun contentAndModelChangesInvalidateSummaryCache() = runBlocking {
        val repo = repository()
        repo.summary(article())
        repo.summary(article(text = body))
        config = config.copy(model = "another-model")
        repo.summary(article(text = body))
        assertEquals(3, calls)
    }

    @Test fun summariesFollowLanguageAndHaveSeparateCaches() = runBlocking {
        val repo = repository()
        val languages = listOf(SummaryLanguage.CHINESE, SummaryLanguage.ENGLISH, SummaryLanguage.JAPANESE, SummaryLanguage.KOREAN)
        languages.forEach { language ->
            answer = "result-" + language.tag
            assertEquals(answer, repo.summary(article(), language = language))
            assertTrue(systemPrompt.contains(language.instruction))
        }
        assertEquals(4, calls)
        languages.forEach { language -> assertEquals("result-" + language.tag, repo.summary(article(), language = language)) }
        assertEquals(4, calls)
    }

    @Test fun summaryLanguageFollowsDeviceUnlessAppOverridesIt() {
        assertEquals(SummaryLanguage.JAPANESE, SummaryLanguage.resolve("", "ja-JP"))
        assertEquals(SummaryLanguage.ENGLISH, SummaryLanguage.resolve("en", "zh-CN"))
        assertEquals(SummaryLanguage.KOREAN, SummaryLanguage.resolve("", "ko-KR"))
        assertEquals(SummaryLanguage.CHINESE, SummaryLanguage.resolve("", "fr-FR"))
    }

    @Test fun englishSummaryIsNotTruncatedAtChineseCharacterLimit() {
        val text = "This is an English summary of the article. ".repeat(5).trim()
        assertEquals(text, AiRepository.limitSummary(text, SummaryLanguage.ENGLISH))
        val longText = "word ".repeat(150).trim()
        val limited = AiRepository.limitSummary(longText, SummaryLanguage.ENGLISH)
        assertTrue(limited.length <= 600)
        assertTrue(limited.endsWith("…"))
        assertTrue(limited.removeSuffix("…").endsWith("word"))
    }

    @Test fun disabledEmptyAndOverlongArticlesMakeNoPaidRequests() = runBlocking {
        val repo = repository()
        config = config.copy(enabled = false)
        expect(AiError.CONFIG) { repo.summary(article()) }
        config = config.copy(enabled = true)
        expect(AiError.EMPTY) { repo.summary(article().copy(contentHtml = "", summary = "")) }
        expect(AiError.TOO_LONG) { repo.summary(article(text = "字".repeat(AiRepository.MAX_INPUT_CHARS + 1))) }
        assertEquals(0, calls)
    }

    @Test fun summaryOnlyInputIsIdentifiedInThePrompt() = runBlocking {
        repository().summary(article().copy(contentHtml = null))
        assertTrue(input.contains("仅RSS摘要"))
        assertTrue(input.contains("文章的RSS摘要。"))
    }

    @Test fun summaryLimitPreservesUnicodeAndNeverExceedsOneHundredCodePoints() {
        val result = AiRepository.limitSummary("🙂".repeat(110))
        assertEquals(100, result.codePointCount(0, result.length))
        assertFalse(result.contains('\uFFFD'))
        assertTrue(result.endsWith("…"))
        val sentence = "字".repeat(60) + "。" + "字".repeat(60)
        assertEquals("字".repeat(60) + "。", AiRepository.limitSummary(sentence))
    }

    @Test fun verifiedQuoteIsCachedAcrossRecreationAndNextDayAllowsNewAttempt() = runBlocking {
        answer = "{\"id\":1}"
        assertEquals(AiQuote("a1", body), repository().dailyQuote(listOf(article())))
        assertEquals(AiQuote("a1", body), repository().dailyQuote(listOf(article())))
        assertEquals(1, calls)
        date = date.plusDays(1)
        repository().dailyQuote(listOf(article()))
        assertEquals(2, calls)
    }

    @Test fun invalidSelectionCannotBePresentedAsExcerpt() = runBlocking {
        answer = "{\"id\":999}"
        expect(AiError.QUOTE_SELECTION) { repository().dailyQuote(listOf(article()), regenerate = true) }
        assertEquals(1, calls)
    }

    @Test fun noEligibleSentencesMakesNoRequest() = runBlocking {
        val text = "这是一个不能作为金句的问题吗？这是另一个不能作为金句的问题吗？"
        expect(AiError.QUOTE_CANDIDATES) { repository().dailyQuote(listOf(article(text = text)), regenerate = true) }
        assertEquals(0, calls)
    }

    @Test fun failedAutomaticQuoteDoesNotRetrySilentlyEvenAfterCacheClear() = runBlocking {
        answer = "invalid JSON"
        expect(AiError.RESPONSE) { repository().dailyQuote(listOf(article())) }
        assertNull(repository().dailyQuote(listOf(article())))
        repository().clearCache()
        assertNull(repository().dailyQuote(listOf(article())))
        assertEquals(1, calls)
        answer = "{\"id\":1}"
        assertNotNull(repository().dailyQuote(listOf(article()), regenerate = true))
        assertEquals(2, calls)
    }

    @Test fun cancellationKeepsAttemptMarkerSoReturningToHomeDoesNotRespend() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val waiting = AiCompletion { _, _, _, _ ->
            calls++
            started.complete(Unit)
            CompletableDeferred<String>().await()
        }
        val job = async { repository(waiting).dailyQuote(listOf(article())) }
        started.await()
        job.cancelAndJoin()
        assertNull(repository().dailyQuote(listOf(article())))
        assertEquals(1, calls)
    }

    @Test fun quoteSkipsSummaryOnlyAndOversizedCandidatesWithoutSendingFragments() = runBlocking {
        val repo = repository()
        expect(AiError.QUOTE_CANDIDATES) { repo.dailyQuote(listOf(article().copy(contentHtml = null))) }
        expect(AiError.QUOTE_CANDIDATES) { repo.dailyQuote(listOf(article(text = "字".repeat(AiRepository.MAX_INPUT_CHARS + 1)))) }
        assertEquals(0, calls)
        answer = "{\"id\":1}"
        assertNotNull(repo.dailyQuote(listOf(article())))
        assertEquals(1, calls)
    }

    @Test fun quoteCannotJoinParagraphsOrDropAConditionFromWithinASentence() {
        val prefix = "只有在尊重事实并充分了解背景的前提下，"
        val source = article().copy(contentHtml = "<p>$prefix$body</p>")
        assertFalse(AiRepository.verifies(AiQuote("a1", body), listOf(source)))
        val joined = article().copy(contentHtml = "<p>设计的本质是秩序，而非装饰，</p><p>克制往往比堆砌更有力量。</p>")
        assertFalse(AiRepository.verifies(AiQuote("a1", body), listOf(joined)))
    }

    @Test fun quoteCandidatesAreBoundedAndContainOnlyEligibleSentences() = runBlocking {
        answer = "{\"id\":1}"
        val articles = (1..8).map { article("a$it") }
        repository().dailyQuote(articles)
        assertEquals(6, kotlinx.serialization.json.Json.parseToJsonElement(input).let { it as kotlinx.serialization.json.JsonArray }.size)
        assertTrue(input.contains(body))
        assertFalse(input.contains("<article"))
    }

    @Test fun laterArticlesAreUsedWhenEarlierOnesHaveNoEligibleSentence() = runBlocking {
        val invalid = article(text = "这是一个不能作为金句的问题吗？".repeat(4))
        answer = "{\"id\":1}"
        assertEquals(AiQuote("a1", body), repository().dailyQuote(listOf(invalid, invalid, invalid, article())))
    }

    @Test fun chosenNumberMapsToOriginalSentence() = runBlocking {
        val second = "我们沿着河岸慢慢前行，观察两侧建筑与道路之间的细微变化。"
        answer = "{\"id\":2}"
        assertEquals(AiQuote("a1", second), repository().dailyQuote(listOf(article())))
    }

    @Test fun disablingQuotesMakesNoRequests() = runBlocking {
        config = config.copy(quoteEnabled = false)
        assertNull(repository().dailyQuote(listOf(article()), regenerate = true))
        assertEquals(0, calls)
    }
}
