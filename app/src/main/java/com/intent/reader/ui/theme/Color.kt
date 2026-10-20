package com.intent.reader.ui.theme

import androidx.compose.ui.graphics.Color

// Light
val LightBg = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightText = Color(0xFF000000)
val LightTextSecondary = Color(0xFF8A8A8A)
val LightDivider = Color(0xFFE6E6E6)
val LightHeroBg = Color(0xFFF5F5F5)
val LightGlass = Color(0xE6FAFAFA) // near-solid so the pill stays visible on white bg

// Dark
val DarkBg = Color(0xFF000000)
val DarkCard = Color(0xFF0F0F0F)
val DarkText = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFF7A7A7A)
val DarkDivider = Color(0xFF262626)
val DarkHeroBg = Color(0xFF161616)
val DarkGlass = Color(0x8C1A1A1A) // #1A1A1A alpha .55

// Shared
val QuoteUnderline = Color(0xFFE02B2B) // default accent, the only red in the app
val DarkScrim = Color(0xFF000000)

// Accent palette for settings (preset swatches)
val AccentPresets: List<Color> = listOf(
    Color(0xFFE02B2B), // red (default)
    Color(0xFF000000), // black
    Color(0xFF1D4ED8), // blue
    Color(0xFF15803D), // green
    Color(0xFFB45309), // amber
    Color(0xFF7C3AED), // violet
    Color(0xFF0F766E), // teal
    Color(0xFFBE185D), // magenta
)
