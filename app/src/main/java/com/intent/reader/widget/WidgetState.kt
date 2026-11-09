package com.intent.reader.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Home-screen widget state, persisted through Glance's
 * [androidx.glance.appwidget.state.PreferencesGlanceStateDefinition].
 * Articles are stored as a single JSON blob (RemoteViews-friendly).
 */
@Serializable
data class WidgetArticle(
    val articleId: String,
    val title: String,
    val sourceTitle: String,
    val publishedAt: Long,
    val imageUrl: String?,
    val isRead: Boolean,
    val sourceLogoUrl: String? = null,
)

/** Materialised view of the persisted preferences for one composition pass. */
data class WidgetSnapshot(
    val accentColor: Long,
    val themeMode: String,
    val style: String,
    val articles: List<WidgetArticle>,
    val startIndex: Int,
    val artworkRevision: Long = 0L,
)

object WidgetKeys {
    val accentColor = longPreferencesKey("widget_accent_color")
    val themeMode = stringPreferencesKey("widget_theme_mode")
    val style = stringPreferencesKey("widget_style")
    val articlesJson = stringPreferencesKey("widget_articles_json")
    val startIndex = intPreferencesKey("widget_start_index")
    val artworkRevision = longPreferencesKey("widget_artwork_revision")
}

object WidgetStateCodec {
    private val json = Json { ignoreUnknownKeys = true }
    private val articleListSerializer = ListSerializer(WidgetArticle.serializer())

    fun encodeArticles(list: List<WidgetArticle>): String =
        json.encodeToString(articleListSerializer, list)

    fun decodeArticles(raw: String): List<WidgetArticle> =
        runCatching { json.decodeFromString(articleListSerializer, raw) }.getOrDefault(emptyList())
}

/**
 * Writes a freshly loaded snapshot into one widget's state. Used by
 * [IntentWidgets] before re-rendering: the style branch lives inside
 * `provideContent`, so the state has to be current before `update()` runs.
 */
fun MutablePreferences.putWidgetData(fresh: WidgetLoader.FreshState) {
    this[WidgetKeys.accentColor] = fresh.accentColor
    this[WidgetKeys.themeMode] = fresh.themeMode
    this[WidgetKeys.style] = fresh.style
    this[WidgetKeys.articlesJson] = WidgetStateCodec.encodeArticles(fresh.articles)
    this[WidgetKeys.artworkRevision] = System.currentTimeMillis()
    val size = fresh.articles.size
    val current = this[WidgetKeys.startIndex] ?: 0
    this[WidgetKeys.startIndex] = if (size == 0) 0 else Math.floorMod(current, size)
}

fun Preferences.toWidgetSnapshot(): WidgetSnapshot = WidgetSnapshot(
    accentColor = this[WidgetKeys.accentColor] ?: 0xFFE02B2B,
    themeMode = this[WidgetKeys.themeMode] ?: "FOLLOW_SYSTEM",
    style = this[WidgetKeys.style] ?: "classic",
    articles = WidgetStateCodec.decodeArticles(this[WidgetKeys.articlesJson] ?: "[]"),
    startIndex = this[WidgetKeys.startIndex] ?: 0,
    artworkRevision = this[WidgetKeys.artworkRevision] ?: 0L,
)
