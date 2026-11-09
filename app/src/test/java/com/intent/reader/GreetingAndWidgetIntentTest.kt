package com.intent.reader

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.intent.reader.data.CacheCleaner
import com.intent.reader.data.prefs.SettingsRepository
import com.intent.reader.widget.articleIntent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class GreetingAndWidgetIntentTest {
    @Test fun emojiIsChosenOncePerDayAndSurvivesCacheClearAndRepositoryRecreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        var day = LocalDate.of(2040, 3, 21)
        val repository = SettingsRepository(context) { day }
        var selections = 0
        val choose: (Int) -> Int = { selections++; 0 }

        (1..8).map { async { repository.ensureDailyGreetingEmoji(pick = choose) } }.awaitAll()
        val first = repository.settings.first().greetingEmoji
        assertEquals(1, selections)
        assertTrue(first in SettingsRepository.GREETING_EMOJIS)
        CacheCleaner.clear(context)
        val reopened = SettingsRepository(context) { day }
        reopened.ensureDailyGreetingEmoji(pick = choose)
        assertEquals(first, reopened.settings.first().greetingEmoji)
        assertEquals(1, selections)

        day = day.plusDays(1)
        reopened.ensureDailyGreetingEmoji(pick = choose)
        val next = reopened.settings.first().greetingEmoji
        assertEquals(2, selections)
        assertTrue(next in SettingsRepository.GREETING_EMOJIS)
        assertNotEquals(first, next)
        reopened.ensureDailyGreetingEmoji(pick = choose)
        assertEquals(2, selections)
    }

    @Test fun widgetArticleIntentsHaveDifferentAndroidIdentitiesAndPreserveIds() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val first = articleIntent(context, "article/first")
        val second = articleIntent(context, "article-second")
        assertFalse(first.filterEquals(second))
        assertTrue(first.filterEquals(articleIntent(context, "article/first")))
        assertEquals("article/first", first.getStringExtra(MainActivity.EXTRA_ARTICLE_ID))
        assertEquals("article-second", second.getStringExtra(MainActivity.EXTRA_ARTICLE_ID))
        assertEquals("article/first", first.data?.lastPathSegment)
        assertEquals(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            first.flags and (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }
}
