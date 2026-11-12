package com.intent.reader

import android.app.Application
import androidx.work.Configuration
import com.intent.reader.di.AppContainer
import com.intent.reader.notify.DailySummaryWorker
import com.intent.reader.notify.NotificationChannels
import com.intent.reader.notify.LiveUpdate
import com.intent.reader.data.repo.ReadingTracker
import com.intent.reader.sync.SyncWorker
import com.intent.reader.widget.IntentWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private data class LiveState(val eligible: Boolean, val minutes: Int, val goal: Int, val language: String)

class IntentApplication : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Manifest removes the default WorkManagerInitializer (on-demand init);
    // Configuration.Provider lets WorkManager lazy-init itself from this config.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.ensure(this)

        // Keep the card in sync with settings, dismissal, day changes and permissions.
        appScope.launch {
            combine(
                container.readingTracker.observeToday(),
                container.settingsRepository.settings,
                LiveUpdate.refreshRequests,
            ) { log, settings, refresh ->
                val eligible = (log?.elapsedMillis ?: 0L) > 0L &&
                    settings.liveUpdateEnabled &&
                    settings.liveUpdateDismissedDate != ReadingTracker.todayKey() &&
                    LiveUpdate.canPost(this@IntentApplication)
                LiveState(eligible, log?.minutes ?: 0, settings.readingGoalMinutes, settings.languageTag) to refresh
            }.distinctUntilChanged().collect { (state, _) ->
                val (eligible, minutes, goal, language) = state
                val localized = if (language.isBlank()) this@IntentApplication else {
                    val config = android.content.res.Configuration(resources.configuration)
                    config.setLocales(android.os.LocaleList(java.util.Locale.forLanguageTag(language)))
                    createConfigurationContext(config)
                }
                if (eligible) LiveUpdate.update(localized, minutes, goal)
                else LiveUpdate.cancel(this@IntentApplication)
            }
        }

        // Wire periodic sync + daily summary to settings changes
        appScope.launch {
            container.settingsRepository.settings
                .map { Triple(it.syncIntervalMin, it.syncWifiOnly, it.dailySummaryEnabled) }
                .distinctUntilChanged()
                .collect { (interval, wifiOnly, _) ->
                    SyncWorker.schedulePeriodic(this@IntentApplication, interval, wifiOnly)
                }
        }
        appScope.launch {
            container.settingsRepository.settings
                .map { Triple(it.dailySummaryHour, it.dailySummaryMinute, it.dailySummaryEnabled) }
                .distinctUntilChanged()
                .collect { (hour, minute, enabled) ->
                    if (enabled) DailySummaryWorker.scheduleDaily(this@IntentApplication, hour, minute)
                }
        }

        // Generated widget-picker previews (API 35+); no-op below / when rate-limited.
        appScope.launch { IntentWidgets.publishPickerPreviews(this@IntentApplication) }
    }
}
