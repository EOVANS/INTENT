package com.intent.reader.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intent.reader.IntentApplication
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
    val userName: String = "",
    val avatarPath: String = "",
    val refreshing: Boolean = false,
    val loaded: Boolean = false,
)

class HomeViewModel(private val app: IntentApplication) : ViewModel() {

    private val container = app.container
    private val refreshing = MutableStateFlow(false)

    val state: StateFlow<HomeUiState> = combine(
        container.database.articleDao().observeRecent(800),
        container.database.sourceDao().observeAll(),
        container.settingsRepository.settings,
        container.quoteRepository.observeToday(),
        refreshing,
    ) { articles, sources, settings, quote, refreshing ->
        if (articles.isEmpty()) {
            HomeUiState(
                rows = emptyList(), quote = quote, userName = settings.userName,
                avatarPath = settings.avatarPath,
                refreshing = refreshing, loaded = true,
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
                rows = uiRows, quote = quote, userName = settings.userName,
                avatarPath = settings.avatarPath,
                refreshing = refreshing, loaded = true,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // Compute today's quote in the background if missing
        viewModelScope.launch {
            try {
                val builtInIds = container.database.sourceDao().getAll()
                    .filter { it.isBuiltIn }.map { it.id }.toSet()
                container.quoteRepository.ensureTodayComputed(
                    isRecommendedSource = { it in builtInIds }
                )
            } catch (_: Exception) {
            }
        }
    }

    fun refresh(context: Context) {
        refreshing.value = true
        SyncWorker.refreshNow(context)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            refreshing.value = false
        }
    }

    fun setRead(articleId: String, read: Boolean) {
        viewModelScope.launch {
            container.database.articleDao().setRead(articleId, read)
        }
    }
}
