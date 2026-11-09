package com.intent.reader.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Rect as AndroidRect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.PixelCopy
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

private class GlassLuminanceMeter {
    var bitmap: Bitmap? = null
    var bitmapSize = 0
    var pending = false
    var smoothed = 0.5f
    var overLight = false

    fun submit(luminance: Float): Boolean {
        smoothed += (luminance - smoothed) * EMA_ALPHA
        if (!overLight && smoothed > OVER_LIGHT_ON) {
            overLight = true
        } else if (overLight && smoothed < OVER_LIGHT_OFF) {
            overLight = false
        }
        return overLight
    }

    companion object {
        private const val EMA_ALPHA = 0.35f
        private const val OVER_LIGHT_ON = 0.60f
        private const val OVER_LIGHT_OFF = 0.45f
    }
}

private const val SAMPLE_INTERVAL_MS = 400L
private const val SAMPLE_INTERVAL_POWER_SAVE_MS = 1000L

/**
 * Watches the backdrop luminance: periodically samples a small square of the activity
 * window with `PixelCopy` (the Compose stand-in for the library's RenderNode downscale),
 * EMA-smooths it and applies the library's 0.60 / 0.45 hysteresis. Returns `false`
 * while inactive, unavailable or on any sampling failure — never throws.
 */
@Composable
fun rememberGlassAppearance(isActive: Boolean): Boolean {
    val context = LocalContext.current
    val window = remember(context) { context.findActivity()?.window }
    val meter = remember { GlassLuminanceMeter() }
    var overLight by remember { mutableStateOf(false) }

    LaunchedEffect(isActive, window) {
        if (!isActive || window == null) return@LaunchedEffect
        while (true) {
            sampleWindowLuminance(context, window, meter) { overLight = it }
            delay(if (isPowerSaveMode(context)) SAMPLE_INTERVAL_POWER_SAVE_MS else SAMPLE_INTERVAL_MS)
        }
    }

    return overLight
}

private fun sampleWindowLuminance(
    context: Context,
    window: Window,
    meter: GlassLuminanceMeter,
    onResult: (Boolean) -> Unit,
) {
    if (meter.pending) return
    val decor = window.decorView
    val width = decor.width
    val height = decor.height
    if (width <= 0 || height <= 0) return

    // 小采样方块（8×8 量级起步），落在底部玻璃条上方的一小块区域
    val size = (min(width, height) / 12).coerceIn(8, 48)
    val left = (width / 2 - size / 2).coerceIn(0, max(0, width - size))
    val top = (height * 72 / 100 - size / 2).coerceIn(0, max(0, height - size))

    val bitmap = meter.bitmap?.takeIf { !it.isRecycled && meter.bitmapSize == size }
        ?: Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
            meter.bitmap?.recycle()
            meter.bitmap = it
            meter.bitmapSize = size
        }

    meter.pending = true
    try {
        PixelCopy.request(
            window,
            AndroidRect(left, top, left + size, top + size),
            bitmap,
            PixelCopy.OnPixelCopyFinishedListener { result ->
                meter.pending = false
                if (result == PixelCopy.SUCCESS) {
                    onResult(meter.submit(measureLuminance(bitmap)))
                }
            },
            Handler(Looper.getMainLooper()),
        )
    } catch (_: Exception) {
        meter.pending = false
    }
}

/** 稀疏采样平均亮度（与库里 `measureBitmap` 同一套权重与网格）。 */
private fun measureLuminance(bitmap: Bitmap): Float {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0 || bitmap.isRecycled) return 0.5f
    val grid = 8
    val stepX = (w / grid).coerceAtLeast(1)
    val stepY = (h / grid).coerceAtLeast(1)
    var sum = 0f
    var count = 0
    var y = stepY / 2
    while (y < h) {
        var x = stepX / 2
        while (x < w) {
            val c = bitmap.getPixel(x, y)
            sum += 0.2126f * android.graphics.Color.red(c) +
                0.7152f * android.graphics.Color.green(c) +
                0.0722f * android.graphics.Color.blue(c)
            count++
            x += stepX
        }
        y += stepY
    }
    return if (count > 0) (sum / count / 255f).coerceIn(0f, 1f) else 0.5f
}

private fun isPowerSaveMode(context: Context): Boolean = runCatching {
    (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode ?: false
}.getOrDefault(false)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// =====================================================================================
// Progressive scroll-edge blur (ScrollEdgeBlurView port)
// =====================================================================================

/** 模糊所依附的边缘。 */
enum class ScrollEdge { TOP, BOTTOM }

/**
 * Progressive blur band at the given edge, the Compose port of the library's
 * `ScrollEdgeBlurView`. The library needed a blurred duplicate of the content plus a
 * gradient `DstIn` mask because `Paint` cannot carry a `RenderEffect`; here the band is
 * produced inside a single AGSL pass instead — the sampling radius grows towards the
 * edge (`r = maxR * t²`), so the fade is continuous and no second layer is drawn.
 *
 * The effect is attached to the layer's own `renderEffect`, the same mechanism the lens
 * uses, which means the shader's `content` input *is* the scrolling content.
 * API < 33 (or a non-blurring radius) degrades to a gradient scrim.
 */
@Composable
fun Modifier.scrollEdgeBlur(edge: ScrollEdge, bandHeight: Dp, maxBlurRadius: Dp): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || maxBlurRadius.value <= 0.5f) {
        val e = edge
        val band = bandHeight
        return this.drawWithContent {
            drawContent()
            val px = band.toPx()
            val h = size.height
            if (px <= 0f || h <= 0f) return@drawWithContent
            val colors = listOf(Color(0x55000000), Color.Transparent)
            if (e == ScrollEdge.TOP) {
                drawRect(brush = Brush.verticalGradient(colors, 0f, px))
            } else {
                drawRect(brush = Brush.verticalGradient(colors, h - px, h))
            }
        }
    }

    val cache = remember { EdgeBlurCache() }
    return this.graphicsLayer {
        val w = size.width
        val h = size.height
        val band = bandHeight.toPx()
        val radius = maxBlurRadius.toPx()
        if (w <= 0f || h <= 0f || band <= 0f) return@graphicsLayer

        // Rebuild only when the layer geometry or the band parameters actually change;
        // this block runs every frame, the shader must not be recompiled per frame.
        if (cache.effect == null || cache.width != w || cache.height != h ||
            cache.band != band || cache.radius != radius || cache.edge != edge
        ) {
            cache.effect = runCatching { buildEdgeBlurEffect(w, h, band, radius, edge) }.getOrNull()
            cache.width = w
            cache.height = h
            cache.band = band
            cache.radius = radius
            cache.edge = edge
        }
        cache.effect?.let {
            renderEffect = it.asComposeRenderEffect()
            compositingStrategy = CompositingStrategy.Offscreen
        }
    }
}

/** 缓存上一帧的几何/参数，避免每帧重编译 [RuntimeShader]。 */
private class EdgeBlurCache {
    var width = -1f
    var height = -1f
    var band = -1f
    var radius = -1f
    var edge: ScrollEdge? = null
    var effect: RenderEffect? = null
}

/**
 * Variable-radius edge blur. `t` goes 0 at the outer edge to 1 at the inner end of the
 * band, and the tap spread follows `t²` (quadratic ease, closest to the linear-blur
 * ramp the library used); inside the band content outside the band returns the
 * untouched layer, so there is no hard seam at either end.
 *
 * The pass is 1D and runs along the edge normal (vertical), which is all a top/bottom
 * scrim needs and keeps the tap count at nine per pixel. Every tap is clamped to the
 * layer so no tap can pull in pixels from outside the layer at the screen edges.
 */
private const val EDGE_BLUR_AGSL = """
uniform shader content;
uniform float2 size;
uniform float  band;
uniform float  maxR;
uniform float  dir;      // 1 = 顶部边缘；-1 = 底部边缘

half4 main(float2 coord) {
    float d = (dir > 0.0) ? coord.y : (size.y - coord.y);
    float t = clamp(1.0 - d / max(band, 1.0), 0.0, 1.0);
    float r = maxR * t * t;
    if (r <= 0.6) {
        return content.eval(coord);
    }
    // 9 抽样高斯近似（σ ≈ 1.8 个步长），沿纵向展开
    float2 v = float2(0.0, 1.0);
    float o = r * 0.55;
    float2 lo = float2(0.0);
    float2 hi = size;
    half4 sum = content.eval(coord) * 0.2270270;
    sum += content.eval(clamp(coord + v * (1.0 * o), lo, hi)) * 0.1945946;
    sum += content.eval(clamp(coord - v * (1.0 * o), lo, hi)) * 0.1945946;
    sum += content.eval(clamp(coord + v * (2.0 * o), lo, hi)) * 0.1216216;
    sum += content.eval(clamp(coord - v * (2.0 * o), lo, hi)) * 0.1216216;
    sum += content.eval(clamp(coord + v * (3.0 * o), lo, hi)) * 0.0540540;
    sum += content.eval(clamp(coord - v * (3.0 * o), lo, hi)) * 0.0540540;
    sum += content.eval(clamp(coord + v * (4.0 * o), lo, hi)) * 0.0162160;
    sum += content.eval(clamp(coord - v * (4.0 * o), lo, hi)) * 0.0162160;
    return sum;
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun buildEdgeBlurEffect(
    width: Float,
    height: Float,
    band: Float,
    maxRadius: Float,
    edge: ScrollEdge,
): RenderEffect {
    val shader = RuntimeShader(EDGE_BLUR_AGSL)
    shader.setFloatUniform("size", width, height)
    shader.setFloatUniform("band", band)
    shader.setFloatUniform("maxR", maxRadius)
    shader.setFloatUniform("dir", if (edge == ScrollEdge.TOP) 1f else -1f)
    return RenderEffect.createRuntimeShaderEffect(shader, "content")
}
