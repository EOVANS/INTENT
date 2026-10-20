package com.intent.reader

import android.app.Application
import androidx.work.Configuration
import com.intent.reader.di.AppContainer
import com.intent.reader.notify.DailySummaryWorker
import com.intent.reader.notify.NotificationChannels
import com.intent.reader.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
    }
}
