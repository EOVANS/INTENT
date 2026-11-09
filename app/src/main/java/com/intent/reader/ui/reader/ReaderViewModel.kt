package com.intent.reader.ui.reader

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intent.reader.IntentApplication
import com.intent.reader.ui.settings.aiErrorResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.intent.reader.data.db.Article
import com.intent.reader.data.db.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val article: Article? = null,
    val source: Source? = null,
    val minutesToday: Int = 0,
    val secondsToday: Int = 0,
    val goalMinutes: Int = 10,
    val ttsEngine: String = "",
    val ttsRate: Float = 1f,
    val ttsPitch: Float = 1f,
    val aiEnabled: Boolean = false,
    val summaryVisible: Boolean = false,
    val summaryLoading: Boolean = false,
    val aiSummary: String? = null,
    val summaryError: Int? = null,
    val notes: List<com.intent.reader.notes.IslandNote> = emptyList(),
    val notesReady: Boolean = false,
    val notesError: Boolean = false,
)

class ReaderViewModel(private val app: IntentApplication) : ViewModel() {
    private val container = app.container
    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state
    private var summaryJob: Job? = null

    init {
        viewModelScope.launch {
            container.notesRepository.state.collect { notes ->
                _state.update { current -> current.copy(
                    notes = notes.notes.filter { it.articleId == current.article?.id },
                    notesReady = notes.loaded, notesError = notes.unreadableFiles > 0,
                ) }
            }
        }
        viewModelScope.launch {
            container.aiConfigStore.config.collect { config ->
                summaryJob?.cancel()
                _state.update { it.copy(aiEnabled = config.enabled, summaryLoading = false, aiSummary = null, summaryError = null) }
            }
        }
        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings ->
                _state.update {
                    it.copy(
                        ttsEngine = settings.ttsEngine,
                        ttsRate = settings.ttsRate,
                        ttsPitch = settings.ttsPitch,
                        goalMinutes = settings.readingGoalMinutes,
                    )
                }
            }
        }
        viewModelScope.launch {
            container.readingTracker.observeToday().collect { log ->
                _state.update {
                    it.copy(
                        minutesToday = log?.minutes ?: 0,
                        secondsToday = ((log?.elapsedMillis ?: 0L) / 1_000 % 60).toInt(),
                    )
                }
            }
        }
    }

    fun load(articleId: String) {
        viewModelScope.launch {
            val article = container.database.articleDao().getById(articleId) ?: return@launch
            val source = container.database.sourceDao().getById(article.sourceId)
            _state.update { it.copy(article = article, source = source,
                notes = container.notesRepository.state.value.notes.filter { note -> note.articleId == articleId }) }
            try { container.notesRepository.load() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(notesError = true) } }
            if (!article.isRead) container.database.articleDao().setRead(article.id, true)
            container.database.articleDao().observeById(articleId).collect { updated ->
                if (updated != null) _state.update { it.copy(article = updated) }
            }
        }
    }

    fun setReadingForeground(active: Boolean) {
        container.readingTracker.setReadingForeground(active)
    }

    suspend fun saveNote(paragraph: com.intent.reader.notes.NoteParagraph, remark: String, drawing: com.intent.reader.notes.Handwriting?) {
        val current = state.value
        val article = current.article ?: error("Article unavailable")
        container.notesRepository.saveWithHandwriting(article, current.source?.title.orEmpty(), paragraph, remark, drawing)
    }

    suspend fun deleteNote(id: String) { container.notesRepository.delete(id) }

    fun summarize(regenerate: Boolean = false) {
        val article = _state.value.article ?: return
        _state.update { it.copy(summaryVisible = true) }
        if (_state.value.summaryLoading || (_state.value.aiSummary != null && !regenerate)) return
        _state.update { it.copy(summaryLoading = true, summaryError = null) }
        summaryJob = viewModelScope.launch {
            try {
                val text = container.aiRepository.summary(article, regenerate)
                _state.update { it.copy(aiSummary = text, summaryLoading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                _state.update { it.copy(summaryError = aiErrorResource(error), summaryLoading = false) }
            }
        }
    }

    fun dismissSummary() {
        // Closing and reopening the sheet must not issue a duplicate paid call.
        // The ViewModel still owns/cancels the work when this reader is removed.
        _state.update { it.copy(summaryVisible = false) }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        } catch (_: Exception) {
        }
    }
}
