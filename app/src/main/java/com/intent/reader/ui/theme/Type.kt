package com.intent.reader.ui.theme

import android.graphics.Typeface
import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * Font policy per design doc 3.3:
 * - Greeting header (name/date/quote) is ALWAYS serif (Times New Roman / Songti fallback chain)
 *   -> FontFamily.Serif, locked, unaffected by user font settings.
 * - Everything else: Roboto on AOSP; MiSans (Medium/Semibold) on Xiaomi/HyperOS ROMs;
 *   fallback to default (Roboto) when MiSans is unavailable.
 * - User imported .ttf/.otf applies to non-serif areas only.
 */
object IntentFonts {

    /**
     * Greeting / quote serif family. FontFamily.Serif alone falls back to sans on
     * many Chinese ROMs (Xiaomi / Oppo / Vivo) because the platform doesn't ship
     * a real serif face. Load NotoSerif / system serif fonts explicitly when
     * present, otherwise fall back to FontFamily.Serif.
     */
    val serifHeader: FontFamily by lazy { loadSerifFamily() ?: FontFamily.Serif }

    private fun loadSerifFamily(): FontFamily? {
        val candidates = listOf(
            // AOSP Noto Serif (regular + bold) shipped since Android 7
            "/system/fonts/NotoSerif-Regular.ttf" to FontWeight.Normal,
            "/system/fonts/NotoSerif-Bold.ttf" to FontWeight.Bold,
            // Source Han Serif (some CN ROMs)
            "/system/fonts/NotoSerifCJK-Regular.ttc" to FontWeight.Normal,
            "/system/fonts/SourceHanSerifCN-Regular.otf" to FontWeight.Normal,
            "/system/fonts/HYWenHei-65W.ttf" to FontWeight.Normal,
        )
        val fonts = candidates.mapNotNull { (path, weight) ->
            val f = File(path)
            if (!f.exists() || f.length() == 0L) return@mapNotNull null
            try {
                Font(f, weight = weight, style = androidx.compose.ui.text.font.FontStyle.Normal)
            } catch (_: Exception) {
                null
            }
        }
        if (fonts.isEmpty()) return null
        return FontFamily(fonts)
    }

    /** Detects ROM and builds the global sans family. */
    fun resolveGlobalFamily(): FontFamily {
        val manufacturer = Build.MANUFACTURER?.uppercase() ?: ""
        val isXiaomi = manufacturer.contains("XIAOMI") || manufacturer.contains("REDMI") ||
            manufacturer.contains("POCO")
        if (isXiaomi || Build.BRAND?.uppercase()?.contains("XIAOMI") == true) {
            val family = loadSystemMiSans()
            if (family != null) return family
        }
        return FontFamily.Default // Roboto on AOSP
    }

    /** MIUI/HyperOS ships MiSans under /system/fonts. Try regular/medium/semibold. */
    private fun loadSystemMiSans(): FontFamily? {
        val dir = File("/system/fonts/")
        if (!dir.isDirectory) return null
        val files = dir.listFiles { f -> f.name.startsWith("MiSans") } ?: return null
        if (files.isEmpty()) return null
        fun pick(vararg keys: String): File? = files.firstOrNull { f ->
            keys.any { f.name.contains(it, ignoreCase = true) }
        }
        val regular = pick("Regular")
        val medium = pick("Medium")
        val semibold = pick("Semibold", "SemiBold", "Bold")
        if (regular == null && medium == null) return null
        return FontFamily(
            listOfNotNull(
                regular?.let { Font(it, weight = FontWeight.Normal, style = androidx.compose.ui.text.font.FontStyle.Normal) },
                medium?.let { Font(it, weight = FontWeight.Medium, style = androidx.compose.ui.text.font.FontStyle.Normal) },
                semibold?.let { Font(it, weight = FontWeight.SemiBold, style = androidx.compose.ui.text.font.FontStyle.Normal) },
            )
        )
    }

    /** Loads a user-imported font file; returns null when unusable. */
    fun fromFile(path: String): FontFamily? {
        val f = File(path)
        if (!f.exists() || f.length() == 0L) return null
        return try {
            // Sanity check with platform Typeface before creating Compose font
            Typeface.createFromFile(f)
            FontFamily(Font(f))
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Global text scale multiplier (accessibility "font size" levels).
 */
enum class TextScaleLevel(val factor: Float) {
    SMALL(0.85f), NORMAL(1f), LARGE(1.15f), XLARGE(1.3f)
}

/**
 * Swiss-style type ramp. Sizes follow doc 3.3 (quote 22-28 / page title 20 /
 * card title 15-16 / body 13-14 / meta 11-12), scaled by [scale].
 */
data class IntentTypography(
    val serif: FontFamily,
    val sans: FontFamily,
    val scale: Float = 1f,
) {
    private fun sp(v: Int) = (v * scale).sp

    // Serif header block (locked)
    val quoteSerif: TextStyle = TextStyle(
        fontFamily = serif, fontWeight = FontWeight.Normal,
        fontSize = sp(20), lineHeight = sp(30)
    )
    val greetingSerif: TextStyle = TextStyle(
        fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = sp(28)
    )
    val dateSerif: TextStyle = TextStyle(
        fontFamily = serif, fontWeight = FontWeight.Normal, fontSize = sp(14)
    )
    val sourceSerif: TextStyle = TextStyle(
        fontFamily = serif, fontWeight = FontWeight.Normal, fontSize = sp(12)
    )

    // Sans UI
    val pageTitle: TextStyle = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = sp(20)
    )
    val sectionTitle: TextStyle = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = sp(14)
    )
    val cardTitle: TextStyle = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = sp(15), lineHeight = sp(21)
    )
    val body: TextStyle = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = sp(14), lineHeight = sp(22)
    )
    val bodySmall: TextStyle = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = sp(13), lineHeight = sp(19)
    )
    val meta: TextStyle = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = sp(12)
    )
    val micro: TextStyle = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = sp(11)
    )
    val button: TextStyle = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = sp(14)
    )

    fun mapToM3(): Typography = Typography(
        displayLarge = quoteSerif,
        headlineMedium = pageTitle,
        titleMedium = cardTitle,
        bodyLarge = body,
        bodyMedium = bodySmall,
        bodySmall = meta,
        labelSmall = micro,
        labelLarge = button,
    )
}
