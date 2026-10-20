package com.intent.reader

import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.Column
import com.intent.reader.data.prefs.SettingsState
import com.intent.reader.data.prefs.ThemeMode
import com.intent.reader.ui.IntentNavHost
import com.intent.reader.ui.theme.IntentFonts
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.IntentTypography
import com.intent.reader.ui.theme.TextScaleLevel
import java.util.Locale

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Guard: on some API 35/36 ROMs (MIUI/ColorOS/OneUI) enableEdgeToEdge
        // can throw during window inset setup; failing open keeps the app alive.
        try {
            enableEdgeToEdge()
        } catch (_: Throwable) { /* fall back to default system bars */ }
        setContent {
            val app = application as IntentApplication
            val settings by app.container.settingsRepository.settings
                .collectAsState(initial = null)
            val s: SettingsState? = settings

            if (s == null) {
                Box(Modifier.fillMaxSize().background(Color.White))
                return@setContent
            }

            // In-app language override (doc: 4 languages + CJK typesetting)
            val ctx = remember(s.languageTag) {
                if (s.languageTag.isNotBlank()) {
                    val locale = Locale.forLanguageTag(s.languageTag)
                    Locale.setDefault(locale)
                    val config = Configuration(resources.configuration)
                    config.setLocales(android.os.LocaleList(locale))
                    // Wrapped so the localized context is still recognised as an
                    // ActivityResultRegistryOwner (see LocalizedContext).
                    LocalizedContext(createConfigurationContext(config), this)
                } else {
                    this
                }
            }

            // Display size (in-app density scale) + text size (font scale override)
            val baseDensity = LocalDensity.current
            val densityFactor = when (s.displaySizeLevel) {
                0 -> 0.85f; 1 -> 1f; 2 -> 1.15f; else -> 1.3f
            }
            val fontScale = if (s.followSystemFontScale) baseDensity.fontScale
            else TextScaleLevel.entries[s.textScaleLevel.coerceIn(0, 3)].factor

            CompositionLocalProvider(
                LocalContext provides ctx,
                LocalDensity provides Density(baseDensity.density * densityFactor, fontScale),
            ) {
                val dark = when (s.themeMode) {
                    ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                // High-contrast accessibility: bump text/border contrast by
                // darkening secondary/tertiary text and strengthening dividers.
                val highContrast = s.highContrast
                // Sync window background with the effective theme so any
                // framework-level surface (transitions / predictive back / scrims)
                // does not flash the system dark color under light content.
                androidx.compose.runtime.SideEffect {
                    window.setBackgroundDrawable(
                        android.graphics.drawable.ColorDrawable(
                            if (dark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                        )
                    )
                }
                // System bar icon appearance must follow the in-app theme, not the
                // system one: in light mode the status/navigation bar icons have to
                // turn dark, otherwise the clock stays white on a light background.
                androidx.compose.runtime.SideEffect {
                    val controller = androidx.core.view.WindowCompat
                        .getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !dark
                    controller.isAppearanceLightNavigationBars = !dark
                }
                val accent = Color(s.accentColor and 0xFFFFFFFF)
                val sans = when {
                    s.fontChoice.startsWith("custom:") ->
                        IntentFonts.fromFile(s.fontChoice.removePrefix("custom:"))
                            ?: IntentFonts.resolveGlobalFamily()
                    s.fontChoice == "serif" -> IntentFonts.serifHeader
                    else -> IntentFonts.resolveGlobalFamily()
                }
                // Serif greeting/quote header is locked to serifHeader regardless of
                // user font setting (design doc 3.3). Body uses `sans` above.
                val typo = IntentTypography(
                    serif = IntentFonts.serifHeader,
                    sans = sans,
                    scale = 1f,
                )
                val widthClass = calculateWindowSizeClass(this)

                IntentTheme(darkTheme = dark, accent = accent, typography = typo, highContrast = highContrast) {
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .fillMaxSize()
                            .background(IntentTheme.colors.bg)
                    ) {
                        IntentNavHost(
                            startOnOnboarding = s.onboardingCompletedVersion == 0,
                            widthSizeClass = widthClass.widthSizeClass,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Configuration-wrapped Context used for the in-app language override.
 *
 * The ActivityResultRegistryOwner CompositionLocal is resolved from
 * `LocalContext`, so handing a bare `createConfigurationContext(...)` result to
 * `LocalContext provides` makes it resolve to null and
 * `rememberLauncherForActivityResult` throws
 * "No ActivityResultRegistryOwner was provided via LocalActivityResultRegistryOwner"
 * — which is exactly what happened on every settings page as soon as a
 * non-system language was selected. Delegating the registry keeps the context
 * recognized as the owner.
 */
private class LocalizedContext(
    base: android.content.Context,
    private val owner: ActivityResultRegistryOwner,
) : ContextWrapper(base), ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry
        get() = owner.activityResultRegistry
}
