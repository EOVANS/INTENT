package com.intent.reader.widget

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.glance.unit.ColorProvider
import com.intent.reader.ui.theme.DarkCard
import com.intent.reader.ui.theme.DarkDivider
import com.intent.reader.ui.theme.DarkHeroBg
import com.intent.reader.ui.theme.DarkText
import com.intent.reader.ui.theme.DarkTextSecondary
import com.intent.reader.ui.theme.LightBg
import com.intent.reader.ui.theme.LightDivider
import com.intent.reader.ui.theme.LightHeroBg
import com.intent.reader.ui.theme.LightText
import com.intent.reader.ui.theme.LightTextSecondary

/**
 * Color scheme for the widgets, reusing the main app's design tokens.
 *
 * [themeMode] is the raw setting value: LIGHT / DARK pin a single color,
 * FOLLOW_SYSTEM emits a day/night provider so the launcher can flip the
 * widget with the system dark mode in real time.
 */
class WidgetColors(accentColor: Long, private val themeMode: String) {

    private val accentInt = accentColor.toInt()

    val accent = ColorProvider(Color(accentInt))

    // Black or white on the accent, whichever reads better.
    val onAccent = ColorProvider(
        if (ColorUtils.calculateContrast(android.graphics.Color.BLACK, accentInt) >=
            ColorUtils.calculateContrast(android.graphics.Color.WHITE, accentInt)
        ) Color.Black else Color.White,
    )

    val surface = pick(LightBg, DarkCard)
    val onSurface = pick(LightText, DarkText)
    val onSurfaceVariant = pick(LightTextSecondary, DarkTextSecondary)
    val surfaceVariant = pick(LightHeroBg, DarkHeroBg)
    val outline = pick(LightDivider, DarkDivider)

    // Placeholder behind missing thumbnails.
    val accentContainer = ColorProvider(Color(accentInt).copy(alpha = 0.12f))

    private fun pick(light: Color, dark: Color): ColorProvider = when (themeMode) {
        "LIGHT" -> ColorProvider(light)
        "DARK" -> ColorProvider(dark)
        // Day/night variant lives in androidx.glance.color (same function name).
        else -> androidx.glance.color.ColorProvider(day = light, night = dark)
    }
}
