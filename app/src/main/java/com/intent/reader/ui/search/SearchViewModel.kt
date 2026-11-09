package com.intent.reader.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intent.reader.IntentApplication
import com.intent.reader.data.assets.RecommendedSources
import com.intent.reader.data.db.Article
import com.intent.reader.data.db.Source
import com.intent.reader.data.repo.SourceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val sources: List<Source> = emptyList(),
    val subscribedFeedUrls: Set<String> = emptySet(),
    val results: List<Article> = emptyList(),
    val searching: Boolean = false,
)

class SearchViewModel(private val app: IntentApplication) : ViewModel() {

    private val container = app.container
    private val query = MutableStateFlow("")
    private val searching = MutableStateFlow(false)
    private val results = MutableStateFlow<List<Article>>(emptyList())

    val state: StateFlow<SearchUiState> = combine(
        query, container.database.sourceDao().observeAll(), results, searching,
    ) { q, sources, res, searching ->
        SearchUiState(
            query = q,
            sources = sources,
            subscribedFeedUrls = sources.map { it.feedUrl }.toSet() +
                RecommendedSources.all.map { it.feedUrl }.filter { url ->
                    sources.any { it.feedUrl == url }
                }.toSet(),
            results = res,
            searching = searching,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun isSubscribed(feedUrl: String): Boolean =
        state.value.sources.any { it.feedUrl == feedUrl }

    fun onQueryChange(q: String) {
        query.value = q
        results.value = emptyList()
        if (q.isBlank()) return
        viewModelScope.launch {
            searching.value = true
            val db = container.database
            val found = try {
                db.articleDao().searchFts(q)
            } catch (_: Exception) {
                db.articleDao().searchLike(q)
            }
            results.value = found
            searching.value = false
        }
    }

    /** Adds by feed URL or site URL (auto-discovery). */
    fun addSource(raw: String, onDone: (SourceRepository.AddSourceError?) -> Unit) {
        viewModelScope.launch {
            val r = container.sourceRepository.addFromUrl(raw)
            if (r.isSuccess) {
                com.intent.reader.sync.SyncWorker.refreshNow(app)
            }
            onDone(r.errorOrNull())
        }
    }

    fun addRecommended(feedUrl: String, onDone: (SourceRepository.AddSourceError?) -> Unit) {
        val rs = RecommendedSources.all.firstOrNull { it.feedUrl == feedUrl } ?: run {
            onDone(SourceRepository.AddSourceError.INVALID); return
        }
        viewModelScope.launch {
            val r = container.sourceRepository.addRecommended(rs)
            if (r.isSuccess) com.intent.reader.sync.SyncWorker.refreshNow(app)
            onDone(r.errorOrNull())
        }
    }

    /** Tap-to-toggle: subscribe when not subscribed, unsubscribe when already subscribed. */
    fun toggleRecommended(feedUrl: String, onDone: (SourceRepository.AddSourceError?) -> Unit) {
        val existing = state.value.sources.firstOrNull { it.feedUrl == feedUrl }
        if (existing != null) {
            removeSource(existing.id) { onDone(null) }
        } else {
            addRecommended(feedUrl, onDone)
        }
    }

    private fun <T> Result<T>.errorOrNull(): SourceRepository.AddSourceError? =
        if (isSuccess) null
        else (exceptionOrNull() as? SourceRepository.AddSourceException)?.error
            ?: SourceRepository.AddSourceError.INVALID

    fun removeSource(id: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            container.sourceRepository.remove(id)
            onDone(true)
        }
    }
}
