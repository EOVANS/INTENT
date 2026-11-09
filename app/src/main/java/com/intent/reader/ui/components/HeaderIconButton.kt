package com.intent.reader.ui.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.intent.reader.ui.theme.IntentTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop

/** Supplied by the navigation host from the existing advanced-material settings. */
data class HeaderMaterial(
    val backdrop: Backdrop? = null,
    val liquidGlass: Boolean = false,
    val clearMaterial: Boolean = false,
    val adaptiveTint: Boolean = true,
    val overLight: Boolean = false,
    val reduceMotion: Boolean = false,
    val scrollEdgeBlur: Boolean = true,
    val transparency: Float = 0.5f,
)

val LocalHeaderMaterial = staticCompositionLocalOf { HeaderMaterial() }

/** One circular top-bar control, sharing the tab bar's material and press treatment. */
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
) {
    val c = IntentTheme.colors
    val material = LocalHeaderMaterial.current
    val advanced = Build.VERSION.SDK_INT >= 33 && material.liquidGlass
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = if (material.reduceMotion) snap() else spring(0.7f, 450f),
        label = "headerIconPress",
    )
    // Current top bars have a solid page background. Never sample the ancestor
    // NavHost layer here: that would include this button and cause self-sampling.
    val backingColor = c.bg
    val backdrop = if (advanced) material.backdrop ?: rememberCanvasBackdrop { drawRect(backingColor) } else null
    val foreground = if (tint == Color.Unspecified) c.text else tint
    IconButton(onClick = onClick, enabled = enabled, interactionSource = interaction, modifier = modifier.size(48.dp)) {
        Box(
            Modifier.size(40.dp).navigationSurface(
                advanced, backdrop, material.clearMaterial, material.adaptiveTint,
                material.overLight, press, transparency = material.transparency,
                readableOnPlainBackground = true,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(23.dp),
                tint = foreground.copy(alpha = foreground.alpha * if (enabled) 1f else 0.38f))
        }
    }
}
