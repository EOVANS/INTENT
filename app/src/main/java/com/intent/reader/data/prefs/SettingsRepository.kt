package com.intent.reader.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { FOLLOW_SYSTEM, LIGHT, DARK }

data class SettingsState(
    val userName: String = "",
    val avatarPath: String = "",
    val onboardingCompletedVersion: Int = 0,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val accentColor: Long = 0xFFE02B2B,
    val widgetStyle: String = "classic",     // "classic" | "image"
    val fontChoice: String = "system",     // "system" | "serif" | "custom:<path>"
    val languageTag: String = "",           // "" = follow system
    val textScaleLevel: Int = 1,            // 0..3
    val followSystemFontScale: Boolean = true,
    val displaySizeLevel: Int = 1,          // 0..3 (in-app density scale)
    val highContrast: Boolean = false,
    val largeTouchTargets: Boolean = false,
    val reduceAnimations: Boolean = false,
    val glassEffectEnabled: Boolean = false,   // AGSL liquid-glass tab bar (API 33+)
    val glassMaterial: String = "regular",     // "regular" (adaptive / readable) | "clear" (high transparency)
    val glassAdaptiveTint: Boolean = true,     // per-pixel backdrop-luminance tinting
    val glassScrollEdgeBlur: Boolean = true,   // progressive blur at the scrolling edge
    val ttsEngine: String = "",
    val ttsRate: Float = 1f,
    val ttsPitch: Float = 1f,
    val dailySummaryEnabled: Boolean = true,
    val dailySummaryHour: Int = 20,
    val dailySummaryMinute: Int = 0,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 8,
    val liveUpdateEnabled: Boolean = true,
    val readingGoalMinutes: Int = 10,
    val syncIntervalMin: Int = 240,
    val syncWifiOnly: Boolean = false,
    val wifiOnlyImages: Boolean = false,
    val retainDays: Int = 30,
    val perSourceKeep: Int = 200,
    val disabledCardStyles: Set<String> = emptySet(),
    val liveUpdateDismissedDate: String = "",
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val userName = stringPreferencesKey("user_name")
        val avatarPath = stringPreferencesKey("avatar_path")
        val onboardingVersion = intPreferencesKey("onboarding_completed_version")
        val themeMode = intPreferencesKey("theme_mode")
        val accentColor = stringPreferencesKey("accent_color")
        val widgetStyle = stringPreferencesKey("widget_style")
        val fontChoice = stringPreferencesKey("font_choice")
        val languageTag = stringPreferencesKey("language_tag")
        val textScale = intPreferencesKey("text_scale_level")
        val followFontScale = booleanPreferencesKey("follow_system_font_scale")
        val displaySize = intPreferencesKey("display_size_level")
        val highContrast = booleanPreferencesKey("high_contrast")
        val largeTouch = booleanPreferencesKey("large_touch_targets")
        val reduceAnim = booleanPreferencesKey("reduce_animations")
        val glassEffect = booleanPreferencesKey("glass_effect_enabled")
        val glassMaterial = stringPreferencesKey("glass_material")
        val glassAdaptiveTint = booleanPreferencesKey("glass_adaptive_tint")
        val glassScrollEdgeBlur = booleanPreferencesKey("glass_scroll_edge_blur")
        val ttsEngine = stringPreferencesKey("tts_engine")
        val ttsRate = floatPreferencesKey("tts_rate")
        val ttsPitch = floatPreferencesKey("tts_pitch")
        val dailySummary = booleanPreferencesKey("daily_summary_enabled")
        val dailySummaryTime = stringPreferencesKey("daily_summary_time") // "HH:mm"
        val quietStart = intPreferencesKey("quiet_start_hour")
        val quietEnd = intPreferencesKey("quiet_end_hour")
        val liveUpdate = booleanPreferencesKey("live_update_enabled")
        val readingGoal = intPreferencesKey("reading_goal_minutes")
        val syncInterval = intPreferencesKey("sync_interval_min")
        val syncWifiOnly = booleanPreferencesKey("sync_wifi_only")
        val wifiOnlyImages = booleanPreferencesKey("wifi_only_images")
        val retainDays = intPreferencesKey("retain_days")
        val perSourceKeep = intPreferencesKey("per_source_keep")
        val disabledCards = stringSetPreferencesKey("disabled_card_styles")
        val liveUpdateDismissed = stringPreferencesKey("live_update_dismissed_date")
    }

    val settings: Flow<SettingsState> = context.dataStore.data.map { p ->
        val time = (p[Keys.dailySummaryTime] ?: "20:00").split(":")
        SettingsState(
            userName = p[Keys.userName] ?: "",
            avatarPath = p[Keys.avatarPath] ?: "",
            onboardingCompletedVersion = p[Keys.onboardingVersion] ?: 0,
            themeMode = ThemeMode.entries.getOrElse(p[Keys.themeMode] ?: 0) { ThemeMode.FOLLOW_SYSTEM },
            accentColor = p[Keys.accentColor]?.toLongOrNull() ?: 0xFFE02B2B,
            widgetStyle = p[Keys.widgetStyle] ?: "classic",
            fontChoice = p[Keys.fontChoice] ?: "system",
            languageTag = p[Keys.languageTag] ?: "",
            textScaleLevel = p[Keys.textScale] ?: 1,
            followSystemFontScale = p[Keys.followFontScale] ?: true,
            displaySizeLevel = p[Keys.displaySize] ?: 1,
            highContrast = p[Keys.highContrast] ?: false,
            largeTouchTargets = p[Keys.largeTouch] ?: false,
            reduceAnimations = p[Keys.reduceAnim] ?: false,
            glassEffectEnabled = p[Keys.glassEffect] ?: false,
            glassMaterial = p[Keys.glassMaterial] ?: "regular",
            glassAdaptiveTint = p[Keys.glassAdaptiveTint] ?: true,
            glassScrollEdgeBlur = p[Keys.glassScrollEdgeBlur] ?: true,
            ttsEngine = p[Keys.ttsEngine] ?: "",
            ttsRate = p[Keys.ttsRate] ?: 1f,
            ttsPitch = p[Keys.ttsPitch] ?: 1f,
            dailySummaryEnabled = p[Keys.dailySummary] ?: true,
            dailySummaryHour = time.getOrNull(0)?.toIntOrNull() ?: 20,
            dailySummaryMinute = time.getOrNull(1)?.toIntOrNull() ?: 0,
            quietStartHour = p[Keys.quietStart] ?: 22,
            quietEndHour = p[Keys.quietEnd] ?: 8,
            liveUpdateEnabled = p[Keys.liveUpdate] ?: true,
            readingGoalMinutes = p[Keys.readingGoal] ?: 10,
            syncIntervalMin = p[Keys.syncInterval] ?: 240,
            syncWifiOnly = p[Keys.syncWifiOnly] ?: false,
            wifiOnlyImages = p[Keys.wifiOnlyImages] ?: false,
            retainDays = p[Keys.retainDays] ?: 30,
            perSourceKeep = p[Keys.perSourceKeep] ?: 200,
            disabledCardStyles = p[Keys.disabledCards] ?: emptySet(),
            liveUpdateDismissedDate = p[Keys.liveUpdateDismissed] ?: "",
        )
    }

    suspend fun setUserName(v: String) = edit { it[Keys.userName] = v }
    suspend fun setAvatarPath(v: String) = edit { it[Keys.avatarPath] = v }
    suspend fun setOnboardingVersion(v: Int) = edit { it[Keys.onboardingVersion] = v }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.themeMode] = mode.ordinal }
    suspend fun setAccentColor(argb: Long) = edit { it[Keys.accentColor] = argb.toString() }
    suspend fun setWidgetStyle(v: String) = edit { it[Keys.widgetStyle] = v }
    suspend fun setFontChoice(v: String) = edit { it[Keys.fontChoice] = v }
    suspend fun setLanguageTag(v: String) = edit { it[Keys.languageTag] = v }
    suspend fun setTextScaleLevel(v: Int) = edit { it[Keys.textScale] = v }
    suspend fun setFollowSystemFontScale(v: Boolean) = edit { it[Keys.followFontScale] = v }
    suspend fun setDisplaySizeLevel(v: Int) = edit { it[Keys.displaySize] = v }
    suspend fun setHighContrast(v: Boolean) = edit { it[Keys.highContrast] = v }
    suspend fun setLargeTouchTargets(v: Boolean) = edit { it[Keys.largeTouch] = v }
    suspend fun setReduceAnimations(v: Boolean) = edit { it[Keys.reduceAnim] = v }
    suspend fun setGlassEffectEnabled(v: Boolean) = edit { it[Keys.glassEffect] = v }
    suspend fun setGlassMaterial(v: String) = edit { it[Keys.glassMaterial] = v }
    suspend fun setGlassAdaptiveTint(v: Boolean) = edit { it[Keys.glassAdaptiveTint] = v }
    suspend fun setGlassScrollEdgeBlur(v: Boolean) = edit { it[Keys.glassScrollEdgeBlur] = v }
    suspend fun setTtsEngine(v: String) = edit { it[Keys.ttsEngine] = v }
    suspend fun setTtsRate(v: Float) = edit { it[Keys.ttsRate] = v }
    suspend fun setTtsPitch(v: Float) = edit { it[Keys.ttsPitch] = v }
    suspend fun setDailySummaryEnabled(v: Boolean) = edit { it[Keys.dailySummary] = v }
    suspend fun setDailySummaryTime(hour: Int, minute: Int) = edit {
        it[Keys.dailySummaryTime] = "%02d:%02d".format(hour, minute)
    }
    suspend fun setQuietHours(start: Int, end: Int) = edit {
        it[Keys.quietStart] = start; it[Keys.quietEnd] = end
    }
    suspend fun setLiveUpdateEnabled(v: Boolean) = edit { it[Keys.liveUpdate] = v }
    suspend fun setReadingGoalMinutes(v: Int) = edit { it[Keys.readingGoal] = v }
    suspend fun setSyncIntervalMin(v: Int) = edit { it[Keys.syncInterval] = v }
    suspend fun setSyncWifiOnly(v: Boolean) = edit { it[Keys.syncWifiOnly] = v }
    suspend fun setWifiOnlyImages(v: Boolean) = edit { it[Keys.wifiOnlyImages] = v }
    suspend fun setRetainDays(v: Int) = edit { it[Keys.retainDays] = v }
    suspend fun setPerSourceKeep(v: Int) = edit { it[Keys.perSourceKeep] = v }
    suspend fun setCardStyleDisabled(style: String, disabled: Boolean) = edit {
        val cur = it[Keys.disabledCards] ?: emptySet()
        it[Keys.disabledCards] = if (disabled) cur + style else cur - style
    }
    suspend fun setLiveUpdateDismissedDate(v: String) = edit { it[Keys.liveUpdateDismissed] = v }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
