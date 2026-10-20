package com.intent.reader.ui.reader

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intent.reader.IntentApplication
import com.intent.reader.data.db.Article
import com.intent.reader.data.db.Source
import com.intent.reader.notify.LiveUpdate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ReaderUiState(
    val article: Article? = null,
    val source: Source? = null,
    val minutesToday: Int = 0,
    val goalMinutes: Int = 10,
    // TTS prefs so the read-aloud button uses the engine/rate/pitch picked in settings.
    val ttsEngine: String = "",
    val ttsRate: Float = 1f,
    val ttsPitch: Float = 1f,
)

class ReaderViewModel(private val app: IntentApplication) : ViewModel() {

    private val container = app.container
    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state

    private var timerJob: kotlinx.coroutines.Job? = null

    fun load(articleId: String) {
        viewModelScope.launch {
            val settings = container.settingsRepository.settings.first()
            _state.value = _state.value.copy(
                ttsEngine = settings.ttsEngine,
                ttsRate = settings.ttsRate,
                ttsPitch = settings.ttsPitch,
            )
            val article = container.database.articleDao().getById(articleId) ?: return@launch
            val source = container.database.sourceDao().getById(article.sourceId)
            _state.value = _state.value.copy(article = article, source = source)
            // auto mark read
            if (!article.isRead) {
                container.database.articleDao().setRead(article.id, true)
            }
            container.database.articleDao().observeById(articleId).collect { updated ->
                if (updated != null) {
                    _state.value = _state.value.copy(article = updated)
                }
            }
        }
    }

    /** Starts the per-minute foreground reading timer + live update refresh. */
    fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val settings = container.settingsRepository.settings.first()
            val goal = settings.readingGoalMinutes
            _state.value = _state.value.copy(goalMinutes = goal)
            while (true) {
                delay(60_000)
                val minutes = container.readingTracker.addMinute(goal)
                _state.value = _state.value.copy(minutesToday = minutes)
                if (settings.liveUpdateEnabled && LiveUpdate.isSupported()) {
                    LiveUpdate.update(app, minutes, goal)
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        } catch (_: Exception) {
        }
    }
}
