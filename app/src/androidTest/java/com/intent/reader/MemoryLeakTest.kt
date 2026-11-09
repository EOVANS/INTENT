package com.intent.reader

import androidx.activity.compose.setContent
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.intent.reader.ui.reader.ReaderScreen
import com.intent.reader.ui.reader.ReaderUiState
import com.intent.reader.ui.settings.AccessibilityScreen
import com.intent.reader.ui.theme.IntentTheme
import leakcanary.LeakAssertions
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Run on a device: gradle :app:connectedDebugAndroidTest.
 * No subscriptions, notes or settings are changed by these tests.
 */
class MemoryLeakTest {
    @Test fun standaloneReaderActivityReleasesAfterFinish() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, ReaderActivity::class.java)
            .putExtra(MainActivity.EXTRA_ARTICLE_ID, "leak-test-missing-article")
        ActivityScenario.launch<ReaderActivity>(intent).use { scenario ->
            scenario.onActivity { }
        }
        LeakAssertions.assertNoLeaks()
    }

    @Test fun activityRecreationAndFinishReleaseInstances() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            repeat(3) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                recreate(scenario)
            }
        }
        LeakAssertions.assertNoLeaks()
    }

    private fun recreate(scenario: ActivityScenario<MainActivity>) {
        val resumed = CountDownLatch(1)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        val callback = ActivityLifecycleCallback { activity, stage ->
            if (activity is MainActivity && stage == Stage.RESUMED) resumed.countDown()
        }
        instrumentation.runOnMainSync { monitor.addLifecycleCallback(callback) }
        try {
            // ActivityScenario.recreate() uses an intermediate EmptyActivity,
            // which stalls on this API 37 device with our singleTask activity.
            scenario.onActivity { it.recreate() }
            assertTrue("Recreated Activity did not resume", resumed.await(10, TimeUnit.SECONDS))
        } finally {
            instrumentation.runOnMainSync { monitor.removeLifecycleCallback(callback) }
        }
    }

    @Test fun leavingAccessibilityDuringTtsInitializationReleasesActivity() {
        exerciseScreen { AccessibilityScreen(onBack = {}) }
        LeakAssertions.assertNoLeaks()
    }

    @Test fun leavingReaderDuringTtsInitializationReleasesActivity() {
        exerciseScreen {
            ReaderScreen(
                state = ReaderUiState(), onBack = {}, onOpenInBrowser = {},
                onShare = {}, onAiSummary = {}, onRegenerateSummary = {},
                onDismissSummary = {}, onOpenNotes = {}, onSaveNote = { _, _ -> },
                onDeleteNote = {},
            )
        }
        LeakAssertions.assertNoLeaks()
    }

    private fun exerciseScreen(content: @Composable () -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            repeat(5) {
                render(scenario, content)
                // Dispose immediately after composition, without waiting for
                // an arbitrary TTS initialization delay or service callback.
                render(scenario) {}
            }
        }
    }

    private fun render(scenario: ActivityScenario<MainActivity>, content: @Composable () -> Unit) {
        val composed = CountDownLatch(1)
        scenario.onActivity { activity ->
            activity.setContent {
                IntentTheme {
                    content()
                    SideEffect { composed.countDown() }
                }
            }
        }
        assertTrue("Screen did not compose within 10 seconds", composed.await(10, TimeUnit.SECONDS))
    }
}
