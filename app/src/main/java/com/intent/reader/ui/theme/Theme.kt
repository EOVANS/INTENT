package com.intent.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Design tokens per doc section 3. Shapes live in IntentDimens. */
@Immutable
data class IntentColors(
    val bg: Color,
    val card: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val heroBg: Color,
    val surface: Color,
    val accent: Color,
    val glass: Color,
    val scrim: Color,
    val isDark: Boolean,
)

val LightScheme = IntentColors(
    bg = LightBg, card = LightCard, text = LightText,
    textSecondary = LightTextSecondary, textTertiary = Color(0xFFB4B4B4),
    divider = LightDivider, heroBg = LightHeroBg, surface = Color(0xFFF5F5F5),
    accent = QuoteUnderline, glass = LightGlass, scrim = DarkScrim, isDark = false,
)

val DarkScheme = IntentColors(
    bg = DarkBg, card = DarkCard, text = DarkText,
    textSecondary = DarkTextSecondary, textTertiary = Color(0xFF5A5A5A),
    divider = DarkDivider, heroBg = DarkHeroBg, surface = Color(0xFF161616),
    accent = QuoteUnderline, glass = DarkGlass, scrim = DarkScrim, isDark = true,
)

val LocalIntentColors = staticCompositionLocalOf { LightScheme }
val LocalIntentTypography = staticCompositionLocalOf { IntentTypography(FontFamily.Serif, FontFamily.Default) }

object IntentDimens {
    const val radiusCard = 16
    const val radiusInput = 12
    const val radiusTab = 28
    const val pageHMarginPhone = 16
    const val pageHMarginTablet = 28
    const val cardInnerPadding = 16
    const val cardSpacing = 12
    const val headerToFeedGap = 28
}

@Composable
fun IntentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color = QuoteUnderline,
    typography: IntentTypography? = null,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkScheme else LightScheme
    val colors0 = base.copy(accent = accent)
    // High-contrast accessibility: pull secondary/tertiary text toward full
    // contrast and make dividers clearly visible.
    val colors = if (highContrast) colors0.copy(
        textSecondary = colors0.text,
        textTertiary = colors0.textSecondary,
        divider = colors0.text.copy(alpha = 0.4f),
    ) else colors0
    val m3Scheme = if (darkTheme) {
        darkColorScheme(
            background = colors.bg, surface = colors.bg, surfaceVariant = colors.card,
            onBackground = colors.text, onSurface = colors.text, onSurfaceVariant = colors.textSecondary,
            outlineVariant = colors.divider, primary = accent, onPrimary = colors.bg,
            secondary = colors.text, onSecondary = colors.bg,
        )
    } else {
        lightColorScheme(
            background = colors.bg, surface = colors.bg, surfaceVariant = colors.card,
            onBackground = colors.text, onSurface = colors.text, onSurfaceVariant = colors.textSecondary,
            outlineVariant = colors.divider, primary = accent, onPrimary = colors.bg,
            secondary = colors.text, onSecondary = colors.bg,
        )
    }
    val typo = typography ?: LocalIntentTypography.current
    CompositionLocalProvider(
        LocalIntentColors provides colors,
        LocalIntentTypography provides typo,
    ) {
        MaterialTheme(colorScheme = m3Scheme, typography = typo.mapToM3(), content = content)
    }
}

object IntentTheme {
    val colors: IntentColors
        @Composable get() = LocalIntentColors.current
    val typography: IntentTypography
        @Composable get() = LocalIntentTypography.current
}
