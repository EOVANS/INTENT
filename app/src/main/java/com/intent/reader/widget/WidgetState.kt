package com.intent.reader.widget

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
)

object WidgetKeys {
    val accentColor = longPreferencesKey("widget_accent_color")
    val themeMode = stringPreferencesKey("widget_theme_mode")
    val style = stringPreferencesKey("widget_style")
    val articlesJson = stringPreferencesKey("widget_articles_json")
    val startIndex = intPreferencesKey("widget_start_index")
}

object WidgetStateCodec {
    private val json = Json { ignoreUnknownKeys = true }
    private val articleListSerializer = ListSerializer(WidgetArticle.serializer())

    fun encodeArticles(list: List<WidgetArticle>): String =
        json.encodeToString(articleListSerializer, list)

    fun decodeArticles(raw: String): List<WidgetArticle> =
        runCatching { json.decodeFromString(articleListSerializer, raw) }.getOrDefault(emptyList())
}

fun Preferences.toWidgetSnapshot(): WidgetSnapshot = WidgetSnapshot(
    accentColor = this[WidgetKeys.accentColor] ?: 0xFFE02B2B,
    themeMode = this[WidgetKeys.themeMode] ?: "FOLLOW_SYSTEM",
    style = this[WidgetKeys.style] ?: "classic",
    articles = WidgetStateCodec.decodeArticles(this[WidgetKeys.articlesJson] ?: "[]"),
    startIndex = this[WidgetKeys.startIndex] ?: 0,
)
