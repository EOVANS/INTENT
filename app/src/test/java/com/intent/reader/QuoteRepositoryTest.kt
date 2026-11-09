package com.intent.reader

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.intent.reader.data.db.AppDatabase
import com.intent.reader.data.db.Article
import com.intent.reader.data.db.DailyQuote
import com.intent.reader.data.repo.QuoteRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class QuoteRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: QuoteRepository
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.now(zone)
    private val body = "设计的本质是秩序，而非装饰，克制往往比堆砌更有力量。"

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java,
        ).build()
        repository = QuoteRepository(db)
    }

    @After fun cleanup() {
        db.close()
    }

    private fun article(id: String, title: String = "自由与未来，改变世界的勇气与希望", text: String = body) = Article(
        id = id, sourceId = "source", guid = id, title = title,
        link = "https://example.com/$id", author = null,
        summary = "他说：“自由是一种力量”，让我们拥有改变未来和世界的勇气。",
        contentHtml = "<p>$text</p>", imageUrl = null,
        publishedAt = today.atStartOfDay(zone).toInstant().toEpochMilli(),
        fetchedAt = today.atStartOfDay(zone).toInstant().toEpochMilli(), cardStyle = null,
    )

    private fun cached(text: String) = DailyQuote(
        date = today.toString(), text = text, sourceId = "source", articleId = "a1",
        highlights = null, sourceTitle = "Source", articleSummary = null,
    )

    @Test fun bodyReachesBothDailyQuoteAndRotation() = runBlocking {
        val second = "我们沿着河岸慢慢前行，观察两侧建筑与道路之间的细微变化。"
        db.articleDao().insertAll(listOf(article("a1"), article("a2", text = second)))

        val quotes = repository.todayCandidates(zone = zone)

        assertEquals(setOf(body, second), quotes.map { it.text }.toSet())
        assertEquals(setOf("a1", "a2"), quotes.map { it.articleId }.toSet())
        assertEquals(quotes.first().text, repository.getToday(zone)?.text)
    }

    @Test fun legacyFullAndTruncatedHeadlinesAreReplacedToday() = runBlocking {
        val title = "自由与未来，改变世界的勇气与希望".repeat(5)
        db.articleDao().insertAll(listOf(article("a1", title = title)))
        for (headline in listOf(title, title.take(60) + "…")) {
            db.dailyQuoteDao().upsert(cached(headline))
            assertEquals(body, repository.ensureTodayComputed(zone = zone)?.text)
            assertEquals(body, repository.getToday(zone)?.text)
        }
    }

    @Test fun existingExcerptRemainsStableWhenNewArticlesArrive() = runBlocking {
        val excerpt = "我们沿着河岸慢慢前行，观察两侧建筑与道路之间的细微变化。"
        db.articleDao().insertAll(listOf(article("a1", text = excerpt)))
        val original = repository.ensureTodayComputed(zone = zone)
        db.articleDao().insertAll(listOf(article("a2")))

        assertEquals(original, repository.ensureTodayComputed(zone = zone))
        assertEquals(excerpt, repository.todayCandidates(zone = zone).first().text)
    }
    @Test fun olderArticlesProvideQuotesWhenTodayHasNoUpdates() = runBlocking {
        val old = article("older").copy(publishedAt = today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli(), fetchedAt = today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli())
        db.articleDao().insertAll(listOf(old))
        val quotes = repository.todayCandidates(zone = zone)
        assertEquals(body, quotes.first().text)
        assertEquals("older", quotes.first().articleId)
    }

    @Test fun emptyLibraryShowsTemporaryFallbackAndReplacesItAfterSync() = runBlocking {
        val fallback = repository.todayCandidates(zone = zone)
        assertEquals(1, fallback.size)
        assertEquals(null, fallback.first().articleId)
        assertEquals(null, repository.getToday(zone))
        db.articleDao().insertAll(listOf(article("a1")))
        assertEquals(body, repository.todayCandidates(zone = zone).first().text)
    }

}
