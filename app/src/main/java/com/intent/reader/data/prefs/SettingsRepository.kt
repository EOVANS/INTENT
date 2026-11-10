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

/** AI features counted locally for the privacy page; never uploaded. */
enum class AiUsageFeature { QUICK_READ, QUOTE, TEST }

data class SettingsState(
    val userName: String = "",
    val greetingEmoji: String = "",
    val avatarPath: String = "",
    val onboardingCompletedVersion: Int = 0,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val accentColor: Long = 0xFFE02B2B,
    val dynamicColor: Boolean = false,
    val dynamicBackground: Boolean = true,
    val customBackgroundColor: Long? = null, // null = theme default page background
    val widgetStyle: String = "classic",     // "classic" | "image"
    val fontChoice: String = "system",     // "system" | "serif" | "custom:<path>"
    val fontWeight: Int = 400,
    val languageTag: String = "",           // "" = follow system
    val textScaleLevel: Int = 1,            // 0..3
    val followSystemFontScale: Boolean = true,
    val displaySizeLevel: Int = 1,          // 0..3 (in-app density scale)
    val highContrast: Boolean = false,
    val largeTouchTargets: Boolean = false,
    val reduceAnimations: Boolean = false,
    val clipboardAccess: Boolean = true,
    val widgetCreation: Boolean = true,
    val glassEffectEnabled: Boolean = false,   // AGSL liquid-glass tab bar (API 33+)
    val glassMaterial: String = "regular",     // "regular" (adaptive / readable) | "clear" (high transparency)
    val glassTransparency: Float = 0.5f,
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
    val clearCacheOnExit: Boolean = false,
    val aiQuickReadCount: Int = 0,
    val aiQuoteCount: Int = 0,
    val aiTestCount: Int = 0,
)

class SettingsRepository(
    private val context: Context,
    private val currentDate: () -> java.time.LocalDate = { java.time.LocalDate.now() },
) {

    private object Keys {
        val userName = stringPreferencesKey("user_name")
        val greetingEmoji = stringPreferencesKey("greeting_emoji")
        val greetingEmojiDate = stringPreferencesKey("greeting_emoji_date")
        val avatarPath = stringPreferencesKey("avatar_path")
        val onboardingVersion = intPreferencesKey("onboarding_completed_version")
        val themeMode = intPreferencesKey("theme_mode")
        val accentColor = stringPreferencesKey("accent_color")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val dynamicBackground = booleanPreferencesKey("dynamic_background")
        val customBackgroundColor = stringPreferencesKey("custom_background_color")
        val widgetStyle = stringPreferencesKey("widget_style")
        val fontChoice = stringPreferencesKey("font_choice")
        val fontWeight = intPreferencesKey("font_weight")
        val languageTag = stringPreferencesKey("language_tag")
        val textScale = intPreferencesKey("text_scale_level")
        val followFontScale = booleanPreferencesKey("follow_system_font_scale")
        val displaySize = intPreferencesKey("display_size_level")
        val highContrast = booleanPreferencesKey("high_contrast")
        val largeTouch = booleanPreferencesKey("large_touch_targets")
        val reduceAnim = booleanPreferencesKey("reduce_animations")
        val clipboardAccess = booleanPreferencesKey("clipboard_access")
        val widgetCreation = booleanPreferencesKey("widget_creation")
        val glassEffect = booleanPreferencesKey("glass_effect_enabled")
        val glassMaterial = stringPreferencesKey("glass_material")
        val glassTransparency = floatPreferencesKey("glass_transparency")
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
        val clearCacheOnExit = booleanPreferencesKey("clear_cache_on_exit")
        val aiQuickReadCount = intPreferencesKey("ai_quick_read_count")
        val aiQuoteCount = intPreferencesKey("ai_quote_count")
        val aiTestCount = intPreferencesKey("ai_test_count")
    }

    val settings: Flow<SettingsState> = context.dataStore.data.map { p ->
        val time = (p[Keys.dailySummaryTime] ?: "20:00").split(":")
        SettingsState(
            userName = p[Keys.userName] ?: "",
            greetingEmoji = if (p[Keys.greetingEmojiDate] == currentDate().toString())
                p[Keys.greetingEmoji].orEmpty() else "",
            avatarPath = p[Keys.avatarPath] ?: "",
            onboardingCompletedVersion = p[Keys.onboardingVersion] ?: 0,
            themeMode = ThemeMode.entries.getOrElse(p[Keys.themeMode] ?: 0) { ThemeMode.FOLLOW_SYSTEM },
            accentColor = p[Keys.accentColor]?.toLongOrNull() ?: 0xFFE02B2B,
            dynamicColor = p[Keys.dynamicColor] ?: false,
            dynamicBackground = p[Keys.dynamicBackground] ?: true,
            customBackgroundColor = p[Keys.customBackgroundColor]?.toLongOrNull(),
            widgetStyle = p[Keys.widgetStyle] ?: "classic",
            fontChoice = p[Keys.fontChoice] ?: "system",
            fontWeight = (p[Keys.fontWeight] ?: 400).coerceIn(1, 1000),
            languageTag = p[Keys.languageTag] ?: "",
            textScaleLevel = p[Keys.textScale] ?: 1,
            followSystemFontScale = p[Keys.followFontScale] ?: true,
            displaySizeLevel = p[Keys.displaySize] ?: 1,
            highContrast = p[Keys.highContrast] ?: false,
            largeTouchTargets = p[Keys.largeTouch] ?: false,
            reduceAnimations = p[Keys.reduceAnim] ?: false,
            clipboardAccess = p[Keys.clipboardAccess] ?: true,
            widgetCreation = p[Keys.widgetCreation] ?: true,
            glassEffectEnabled = p[Keys.glassEffect] ?: false,
            glassMaterial = p[Keys.glassMaterial] ?: "regular",
            glassTransparency = (p[Keys.glassTransparency] ?: if (p[Keys.glassMaterial] == "clear") 0.85f else 0.5f).coerceIn(0f, 1f),
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
            clearCacheOnExit = p[Keys.clearCacheOnExit] ?: false,
            aiQuickReadCount = p[Keys.aiQuickReadCount] ?: 0,
            aiQuoteCount = p[Keys.aiQuoteCount] ?: 0,
            aiTestCount = p[Keys.aiTestCount] ?: 0,
        )
    }

    suspend fun setUserName(v: String) = edit { it[Keys.userName] = v }

    /** Pick once per local day, atomically across concurrent activity resumes. */
    suspend fun ensureDailyGreetingEmoji(
        today: java.time.LocalDate = currentDate(),
        pick: (Int) -> Int = { kotlin.random.Random.nextInt(it) },
    ) {
        context.dataStore.edit { prefs ->
            val previous = prefs[Keys.greetingEmoji]
            if (prefs[Keys.greetingEmojiDate] != today.toString() || previous !in GREETING_EMOJIS) {
                val choices = GREETING_EMOJIS.filter { it != previous }
                prefs[Keys.greetingEmoji] = choices[pick(choices.size)]
                prefs[Keys.greetingEmojiDate] = today.toString()
            }
        }
    }

    companion object {
        val GREETING_EMOJIS = listOf("💪", "👂", "👏", "👋", "💭", "💬", "🗯️", "🗨️", "👁️", "👀")
    }
    suspend fun setAvatarPath(v: String) = edit { it[Keys.avatarPath] = v }
    suspend fun setOnboardingVersion(v: Int) = edit { it[Keys.onboardingVersion] = v }
    suspend fun setClearCacheOnExit(v: Boolean) = edit { it[Keys.clearCacheOnExit] = v }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.themeMode] = mode.ordinal }
    suspend fun setAccentColor(argb: Long) = edit { it[Keys.accentColor] = argb.toString() }
    suspend fun setDynamicColor(v: Boolean) = edit { it[Keys.dynamicColor] = v }
    suspend fun setDynamicBackground(v: Boolean) = edit { it[Keys.dynamicBackground] = v }
    suspend fun setCustomBackgroundColor(argb: Long?) = edit {
        if (argb == null) it.remove(Keys.customBackgroundColor) else it[Keys.customBackgroundColor] = argb.toString()
    }
    suspend fun setWidgetStyle(v: String) = edit { it[Keys.widgetStyle] = v }
    suspend fun setFontChoice(v: String) = edit { it[Keys.fontChoice] = v }
    suspend fun setFontWeight(v: Int) = edit { it[Keys.fontWeight] = v.coerceIn(1, 1000) }
    suspend fun setLanguageTag(v: String) = edit { it[Keys.languageTag] = v }
    suspend fun setTextScaleLevel(v: Int) = edit { it[Keys.textScale] = v }
    suspend fun setFollowSystemFontScale(v: Boolean) = edit { it[Keys.followFontScale] = v }
    suspend fun setDisplaySizeLevel(v: Int) = edit { it[Keys.displaySize] = v }
    suspend fun setHighContrast(v: Boolean) = edit { it[Keys.highContrast] = v }
    suspend fun setLargeTouchTargets(v: Boolean) = edit { it[Keys.largeTouch] = v }
    suspend fun setReduceAnimations(v: Boolean) = edit { it[Keys.reduceAnim] = v }
    suspend fun setClipboardAccess(v: Boolean) = edit { it[Keys.clipboardAccess] = v }
    suspend fun setWidgetCreation(v: Boolean) = edit { it[Keys.widgetCreation] = v }
    suspend fun setGlassEffectEnabled(v: Boolean) = edit { it[Keys.glassEffect] = v }
    suspend fun setGlassMaterial(v: String) = edit { it[Keys.glassMaterial] = v }
    suspend fun setGlassTransparency(v: Float) = edit { it[Keys.glassTransparency] = v.coerceIn(0f, 1f) }
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

    suspend fun incrementAiUsage(feature: AiUsageFeature) = edit {
        val key = when (feature) {
            AiUsageFeature.QUICK_READ -> Keys.aiQuickReadCount
            AiUsageFeature.QUOTE -> Keys.aiQuoteCount
            AiUsageFeature.TEST -> Keys.aiTestCount
        }
        it[key] = (it[key] ?: 0) + 1
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
