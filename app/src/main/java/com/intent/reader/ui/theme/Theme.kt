package com.intent.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import android.os.Build
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
    dynamicColor: Boolean = false,
    dynamicBackground: Boolean = true,
    customBackground: Color? = null,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkScheme else LightScheme
    val context = LocalContext.current
    val dynamicScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null
    val themed = if (dynamicScheme != null) base.copy(
        bg = if (dynamicBackground) dynamicScheme.background else base.bg,
        card = dynamicScheme.surfaceContainerLow,
        text = dynamicScheme.onSurface,
        textSecondary = dynamicScheme.onSurfaceVariant,
        textTertiary = dynamicScheme.onSurfaceVariant.copy(alpha = 0.7f),
        divider = dynamicScheme.outlineVariant,
        heroBg = dynamicScheme.surfaceContainer,
        surface = dynamicScheme.surfaceContainerHigh,
        accent = dynamicScheme.primary,
        glass = dynamicScheme.surfaceContainerHigh.copy(alpha = if (darkTheme) 0.85f else 0.9f),
    ) else base.copy(accent = accent)
    val colors0 = if (customBackground != null) themed.withCustomBackground(customBackground) else themed
    // High-contrast accessibility: pull secondary/tertiary text toward full
    // contrast and make dividers clearly visible.
    val colors = if (highContrast) colors0.copy(
        textSecondary = colors0.text,
        textTertiary = colors0.textSecondary,
        divider = colors0.text.copy(alpha = 0.4f),
    ) else colors0
    val m3Scheme = if (dynamicScheme != null && customBackground == null) dynamicScheme else if (colors.isDark) {
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

/**
 * Replaces the page background with a user-picked colour. Surfaces and text are
 * re-derived from that colour so the UI stays legible even when it contradicts
 * the light/dark mode the user picked.
 */
private fun IntentColors.withCustomBackground(bgColor: Color): IntentColors {
    val dark = bgColor.luminance() < 0.5f
    val onBg = if (dark) Color.White else Color.Black
    return copy(
        bg = bgColor,
        card = bgColor.blendWith(Color.White, if (dark) 0.06f else 0.03f),
        heroBg = bgColor.blendWith(if (dark) Color.White else Color.Black, if (dark) 0.09f else 0.04f),
        surface = bgColor.blendWith(if (dark) Color.White else Color.Black, if (dark) 0.10f else 0.04f),
        text = onBg,
        textSecondary = onBg.copy(alpha = if (dark) 0.72f else 0.62f),
        textTertiary = onBg.copy(alpha = if (dark) 0.50f else 0.40f),
        divider = onBg.copy(alpha = if (dark) 0.20f else 0.14f),
        glass = if (dark) Color(0x8C1A1A1A) else Color(0xE6FAFAFA),
        isDark = dark,
    )
}

private fun Color.blendWith(other: Color, fraction: Float): Color = Color(
    red = red + (other.red - red) * fraction,
    green = green + (other.green - green) * fraction,
    blue = blue + (other.blue - blue) * fraction,
    alpha = 1f,
)
