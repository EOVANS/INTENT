package com.intent.reader.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intent.reader.IntentApplication
import com.intent.reader.R
import com.intent.reader.ai.AiConfig
import com.intent.reader.quote.QuoteEngine
import com.intent.reader.data.repo.HighlightRange
import com.intent.reader.ui.settings.aiErrorResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.intent.reader.data.db.Article
import com.intent.reader.data.db.DailyQuote
import com.intent.reader.domain.CardStyle
import com.intent.reader.domain.FeedRow
import com.intent.reader.domain.HomeFeedBuilder
import com.intent.reader.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ArticleUi(
    val article: Article,
    val sourceTitle: String,
    val sourceLogo: String?,
    val style: CardStyle,
)

sealed interface HomeRowUi {
    data class Single(val a: ArticleUi) : HomeRowUi
    data class Aggregate(
        val sourceTitle: String,
        val sourceLogo: String?,
        val count: Int,
        val items: List<ArticleUi>,
    ) : HomeRowUi
}

data class HomeUiState(
    val rows: List<HomeRowUi> = emptyList(),
    val quote: DailyQuote? = null,
    val quotes: List<DailyQuote> = emptyList(),
    val userName: String = "",
    val greetingEmoji: String = "",
    val avatarPath: String = "",
    val refreshing: Boolean = false,
    val loaded: Boolean = false,
    val reduceAnimations: Boolean = false,
    val aiQuote: HomeAiQuoteState = HomeAiQuoteState(),
)

data class HomeAiQuoteState(
    val enabled: Boolean = false,
    val loading: Boolean = false,
    val quote: DailyQuote? = null,
    val message: Int? = null,
)

class HomeViewModel(private val app: IntentApplication) : ViewModel() {

    private val container = app.container
    private val refreshing = MutableStateFlow(false)
    private val quoteList = MutableStateFlow<List<DailyQuote>>(emptyList())
    private val aiQuote = MutableStateFlow(HomeAiQuoteState())
    private var aiQuoteJob: Job? = null
    private var homeForeground = false
    private val displayedQuotes = combine(quoteList, aiQuote) { offline, ai ->
        (ai.quote?.let { listOf(it) } ?: offline) to ai
    }

    val state: StateFlow<HomeUiState> = combine(
        container.database.articleDao().observeRecent(800),
        container.database.sourceDao().observeAll(),
        container.settingsRepository.settings,
        displayedQuotes,
        refreshing,
    ) { articles, sources, settings, quoteDisplay, refreshing ->
        val (quotes, ai) = quoteDisplay
        if (articles.isEmpty()) {
            HomeUiState(
                rows = emptyList(), quote = quotes.firstOrNull(), quotes = quotes,
                userName = settings.userName,
                greetingEmoji = settings.greetingEmoji,
                avatarPath = settings.avatarPath,
                refreshing = refreshing, loaded = true,
                reduceAnimations = settings.reduceAnimations,
                aiQuote = ai,
            )
        } else {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val dateKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

            val rows = HomeFeedBuilder.build(
                articles = articles.map {
                    HomeFeedBuilder.Inputs(
                        articleId = it.id,
                        guid = it.guid,
                        sourceId = it.sourceId,
                        publishedAt = it.publishedAt,
                        hasImage = !it.imageUrl.isNullOrBlank(),
                    )
                },
                dayStart = dayStart,
                dateKey = dateKey,
                disabledStyles = settings.disabledCardStyles,
            )
            val byId = articles.associateBy { it.id }
            val bySource = sources.associateBy { it.id }
            val uiRows = rows.mapNotNull { r ->
                when (r) {
                    is FeedRow.Single -> {
                        val a = byId[r.articleId] ?: return@mapNotNull null
                        val s = bySource[a.sourceId]
                        HomeRowUi.Single(
                            ArticleUi(a, s?.title ?: "", s?.logoUrl, r.style)
                        )
                    }
                    is FeedRow.Aggregate -> {
                        val items = r.articleIds.mapNotNull { id -> byId[id] }
                        if (items.isEmpty()) return@mapNotNull null
                        val s = bySource[r.sourceId]
                        HomeRowUi.Aggregate(
                            sourceTitle = s?.title ?: "",
                            sourceLogo = s?.logoUrl,
                            count = r.count,
                            items = items.map { ArticleUi(it, s?.title ?: "", s?.logoUrl, CardStyle.E) },
                        )
                    }
                }
            }
            HomeUiState(
                rows = uiRows, quote = quotes.firstOrNull(), quotes = quotes,
                userName = settings.userName,
                greetingEmoji = settings.greetingEmoji,
                avatarPath = settings.avatarPath,
                refreshing = refreshing, loaded = true,
                reduceAnimations = settings.reduceAnimations,
                aiQuote = ai,
            )
        }
    // Keep the retained Home entry ready while another tab is visible. Grouping
    // hundreds of articles must not run on the UI thread during a tab transition.
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    init {
        viewModelScope.launch {
            var previousConfig: AiConfig? = null
            combine(
                container.aiConfigStore.config,
                container.database.articleDao().observeRecent(1).map { it.firstOrNull()?.id }.distinctUntilChanged(),
            ) { config, articleId -> config to articleId }.collect { (config, articleId) ->
                if (config != previousConfig) {
                    aiQuoteJob?.cancel()
                    aiQuote.value = HomeAiQuoteState(enabled = config.enabled && config.quoteEnabled)
                    previousConfig = config
                }
                if (articleId != null) refreshAiQuote()
            }
        }
        // Compute today's quote rotation once articles are available. On a fresh
        // install the first sync lands after this ViewModel is created, so wait
        // for the first non-empty article list instead of trying exactly once.
        viewModelScope.launch {
            try {
                val builtInIds = container.database.sourceDao().getAll()
                    .filter { it.isBuiltIn }.map { it.id }.toSet()
                container.database.articleDao().observeRecent(1).collect { list ->
                    if (quoteList.value.isEmpty() || (list.isNotEmpty() && quoteList.value.firstOrNull()?.articleId == null)) {
                        quoteList.value = container.quoteRepository.todayCandidates(
                            isRecommendedSource = { it in builtInIds }
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("HomeViewModel", "Unable to load daily quotes", error)
            }
        }
    }

    fun refresh(context: Context) {
        refreshing.value = true
        SyncWorker.refreshNow(context)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            refreshing.value = false
            refreshAiQuote()
        }
    }

    fun setRead(articleId: String, read: Boolean) {
        viewModelScope.launch {
            container.database.articleDao().setRead(articleId, read)
        }
    }

    fun refreshAiQuote(regenerate: Boolean = false) {
        if (!homeForeground || !aiQuote.value.enabled || aiQuote.value.loading) return
        aiQuote.value = aiQuote.value.copy(loading = true, message = null,
            quote = aiQuote.value.quote?.takeIf { it.date == LocalDate.now().toString() })
        aiQuoteJob = viewModelScope.launch {
            try {
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val articles = container.database.articleDao().getSince(today.atStartOfDay(zone).toInstant().toEpochMilli())
                val result = container.aiRepository.dailyQuote(articles, regenerate)
                val article = articles.firstOrNull { it.id == result?.articleId }
                val quote = if (result != null && article != null) DailyQuote(
                    date = today.toString(), text = result.text, sourceId = article.sourceId, articleId = article.id,
                    highlights = Json.encodeToString(QuoteEngine.extractHighlights(result.text).map { HighlightRange(it.start, it.end) }),
                    sourceTitle = container.database.sourceDao().getById(article.sourceId)?.title.orEmpty(),
                    articleSummary = article.summary?.take(80),
                ) else null
                aiQuote.value = aiQuote.value.copy(loading = false, quote = quote ?: aiQuote.value.quote,
                    message = if (quote == null) R.string.ai_quote_unavailable else null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                aiQuote.value = aiQuote.value.copy(loading = false, message = aiErrorResource(error))
            }
        }
    }

    fun setHomeForeground(active: Boolean) {
        homeForeground = active
        if (active) refreshAiQuote()
        else {
            aiQuoteJob?.cancel()
            aiQuote.value = aiQuote.value.copy(loading = false)
        }
    }
}
