package com.intent.reader.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Rect as AndroidRect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.PixelCopy
import android.view.Surface
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Compose port of the QWEA0/Liquid-Glass-Android lens pipeline.
 *
 * The library renders through View `RenderNode`s and a recorded backdrop; Compose
 * cannot sample a backdrop, so the effect is instead attached to the content layer
 * itself: a full-screen, origin-(0,0) NavHost. The shader's `content` input is the
 * layer's own picture, so the layer-local coordinate space is exactly the root
 * coordinate space reported by `boundsInRoot()`.
 *
 * Consequences of that difference (deliberate, see the shader below):
 *  - no recording `margin`: `margin` stays 0 and the `sampleLo`/`sampleHi` clamps are
 *    dropped (the `content` input always covers the whole layer);
 *  - no view-rectangle clip (`viewSize`), which the library only needed to close the
 *    flat edges of shapes extended past the view;
 *  - outside the shape we return `content.eval(coord)` (the untouched layer) instead
 *    of `half4(0.0)`, because here the layer *is* the app's UI.
 */

// =====================================================================================
// AGSL lens shader
// =====================================================================================

/**
 * The optical model, kept line-for-line faithful to [LENS_AGSL] in the library:
 * SDF coverage → numerical-gradient normal → bevel profile (inverse-power or square)
 * → refraction → per-channel dispersion → touch bump → vibrancy saturation →
 * adaptive tint / global tint → glass-tint absorption → two-lobe rim highlight.
 */
private const val LENS_AGSL = """
uniform shader content;
uniform float4 shape1;      // (cx, cy, halfW, halfH)
uniform float  radius1;     // 主形状圆角
uniform float4 shape2;      // 副形状；z <= 0.5 表示禁用
uniform float  radius2;
uniform float  blendK;      // smin 融合宽度；<= 0.5 时用 min()
uniform float  bevel;       // 玻璃厚度（斜面带宽，px）
uniform float  refractPx;   // 贴边折射位移（px）
uniform float  falloff;     // > 0 逆幂剖面；0 = 平方斜面
uniform float  dispersion;
uniform float  blurPx;      // 玻璃内部的高斯模糊半径（px）
uniform float2 lightDir;    // 光线行进方向（view 坐标系 y 向下）
uniform float  specStrength;
uniform float  rimBandMax;  // 迎光侧辉光带宽度上限（px）
uniform float4 tintColor;   // 全局染色（自适应染色关闭时生效）
uniform float  adaptiveTint;
uniform float  adaptiveAmt; // 自适应抬亮强度
uniform float4 glassTint;   // 本体染色（straight-alpha；a = 0 关闭）
uniform float  satFactor;
uniform float  press;
uniform float2 touchPos;
uniform float  touchAmp;

float sdRoundedBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

float sminPoly(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

float lensSDF(float2 p) {
    float d = sdRoundedBox(p - shape1.xy, shape1.zw, radius1);
    if (shape2.z > 0.5) {
        float d2 = sdRoundedBox(p - shape2.xy, shape2.zw, radius2);
        if (blendK > 0.5) {
            d = sminPoly(d, d2, blendK);
        } else {
            d = min(d, d2);
        }
    }
    return d;
}

// 9 抽样高斯（3x3, sigma≈1, 权重归一化）。必须在这里做，而不是在管线里挂一个
// 真正的模糊：本层的 content 就是整屏 UI，链式模糊会让形状之外的内容也变糊。
half4 blurSample(float2 c, float r) {
    if (r <= 0.6) {
        return content.eval(c);
    }
    float o = r * 0.75;
    float2 d = float2(o, o) * 0.7071;
    half4 s = content.eval(c) * 0.2042;
    s += content.eval(c + float2( o, 0.0)) * 0.1238;
    s += content.eval(c - float2( o, 0.0)) * 0.1238;
    s += content.eval(c + float2(0.0,  o)) * 0.1238;
    s += content.eval(c - float2(0.0,  o)) * 0.1238;
    s += content.eval(c + d) * 0.0751;
    s += content.eval(c - d) * 0.0751;
    s += content.eval(c + float2(d.x, -d.y)) * 0.0751;
    s += content.eval(c - float2(d.x, -d.y)) * 0.0751;
    return s;
}

half4 main(float2 coord) {
    float2 p = coord;
    float d = lensSDF(p);

    // 覆盖率：1.5px 抗锯齿羽化
    float cov = clamp(0.5 - d / 1.5, 0.0, 1.0);
    if (cov <= 0.004) {
        return content.eval(coord);
    }

    // SDF 数值梯度 → 屏幕空间外法线
    float2 n = float2(
        lensSDF(p + float2(1.0, 0.0)) - lensSDF(p - float2(1.0, 0.0)),
        lensSDF(p + float2(0.0, 1.0)) - lensSDF(p - float2(0.0, 1.0))
    );
    float nLen = length(n);
    if (nLen > 0.0001) {
        n = n / nLen;
    } else {
        n = float2(0.0, -1.0);
    }

    // 厚度剖面：t=1 平坦内部，t=0 边缘
    float t = clamp(-d / max(bevel, 1.0), 0.0, 1.0);
    float edge = 1.0 - t;
    float slope;
    if (falloff > 0.001) {
        // 引力透镜式逆幂衰减：(1 + 4t)^-falloff 归一化到 [0, 1]
        float gB = pow(5.0, -falloff);
        slope = (pow(1.0 + 4.0 * t, -falloff) - gB) / (1.0 - gB);
    } else {
        // 平方斜面：弯折沿整条斜面带均匀铺开
        slope = edge * edge;
    }

    // 折射：沿法线向内采样（refractDir = -1，与 iOS 一致）
    float refr = refractPx * (1.0 + 0.6 * press);
    float2 offset = n * (-1.0 * slope * refr);

    // 触摸局部液态凸起（手指下方的局部放大：采样向触点收缩）
    if (touchAmp > 0.001) {
        float2 tp = p - touchPos;
        float tr = length(tp);
        float sigma = max(min(shape1.z, shape1.w), 1.0);
        float bump = touchAmp * exp(-(tr * tr) / (sigma * sigma * 0.30));
        if (tr > 1.0) {
            offset -= (tp / tr) * (bump * refr * 0.5);
        }
    }

    // 色散：三通道折射量不同（蓝光弯折最多）
    float2 cR = coord + offset * (1.0 - dispersion * slope);
    float2 cG = coord + offset;
    float2 cB = coord + offset * (1.0 + dispersion * slope);

    float3 col = float3(
        blurSample(cR, blurPx).r,
        blurSample(cG, blurPx).g,
        blurSample(cB, blurPx).b
    );

    // 饱和度（vibrancy 曲线：低饱和多提、高饱和少提、高光保护）
    float lum = dot(col, float3(0.2126, 0.7152, 0.0722));
    if (satFactor <= 1.0) {
        col = mix(float3(lum), col, satFactor);
    } else {
        float satNow = max(col.r, max(col.g, col.b)) - min(col.r, min(col.g, col.b));
        float room = 1.0 - smoothstep(0.2, 0.85, satNow);
        float hl = 1.0 - smoothstep(0.75, 0.98, lum);
        float amount = 1.0 + (satFactor - 1.0) * mix(0.3, 1.0, room * hl);
        col = clamp(mix(float3(lum), col, amount), float3(0.0), float3(1.0));
    }

    // 自适应染色：只朝「更浅」方向调整——暗背景抬亮成浅色玻璃（深色页面下玻璃自动
    // 变浅），亮背景保持原样，这样浅色页面下玻璃依然是通透的，不会被压暗发灰。
    // 抬亮只在背景确实偏暗时触发（0.10~0.38 的过渡带），中等亮度的页面不参与，
    // 否则玻璃会比页面本身还亮，看着像糊了一层白雾。
    if (adaptiveTint > 0.5) {
        float lumT = dot(col, float3(0.2126, 0.7152, 0.0722));
        float e = smoothstep(0.10, 0.38, lumT);
        col = mix(col, float3(1.0), adaptiveAmt * (1.0 - e));
    } else {
        col = mix(col, tintColor.rgb, tintColor.a);
    }

    // 本体染色：按「有色介质」建模——吸收 + 少量散射
    if (glassTint.a > 0.002) {
        float lumTint = dot(col, float3(0.2126, 0.7152, 0.0722));
        float3 absorbed = col * mix(float3(1.0), glassTint.rgb, 0.85);
        float3 scattered = glassTint.rgb * (0.38 * (1.0 - lumTint));
        col = mix(col, clamp(absorbed + scattered, float3(0.0), float3(1.0)), glassTint.a);
    }

    // 光照：两道对称角度瓣（迎光侧 + 背光侧内壁反射）驱动的贴边亮线 + 迎光侧辉光
    float facing = dot(n, -lightDir);
    float lobeF = pow(max(facing, 0.0), 4.5);
    float lobeB = pow(max(-facing, 0.0), 4.5);

    float bandW = clamp(bevel * 0.3, 2.0, rimBandMax);
    float glowIn = clamp((-d - 1.0) / 2.0, 0.0, 1.0);
    float glow = glowIn * pow(clamp(1.0 - (-d - 3.0) / bandW, 0.0, 1.0), 1.5) * cov;
    float hair = clamp(1.0 - abs(d + 1.0) / 2.0, 0.0, 1.0) * cov;
    float spec = (hair * 0.70 * (lobeF + lobeB) + glow * 0.10 * lobeF)
                 * specStrength * (1.0 - 35.0 * 0.01 * press);
    col += float3(spec);

    col = clamp(col, float3(0.0), float3(1.0));
    return half4(half3(col * cov), half(cov));
}
"""

// =====================================================================================
// Public API
// =====================================================================================

/** Glass material variant (matches the library's `GlassMaterial`). */
enum class GlassMaterial {
    /** Strong blur + per-pixel adaptive tint; tuned for legibility. */
    REGULAR,

    /** Weak blur + fixed dimming layer; keeps the content underneath as the subject. */
    CLEAR,
}

/** 主/副玻璃几何，图层局部 px；`smoothing` = smin 融合宽度（0 表示用 `min()`）。 */
data class GlassShape(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val cornerRadius: Float,
)

/** 透镜渲染参数（值语义；与上一份不等时才重建 RenderEffect）。 */
data class GlassLensParams(
    val blurRadius: Float = 0f,
    val primary: GlassShape,
    val secondary: GlassShape? = null,
    val smoothing: Float = 0f,
    val material: GlassMaterial = GlassMaterial.REGULAR,
    val adaptiveTint: Boolean = true,   // per-pixel backdrop-luminance tinting
    val bevel: Float,            // px，玻璃「厚度」
    val refract: Float,          // px，贴边折射位移
    val falloff: Float = 2f,     // >0 逆幂剖面；0 = 平方斜面
    val dispersion: Float = 0.10f,
    val lightDirX: Float = -0.45f,
    val lightDirY: Float = -0.9f,
    val spec: Float = 0.7f,
    val glassTint: Int = 0,      // straight-alpha ARGB；alpha 即染色强度，0 = 不染色
    val saturation: Float = 100f,
    val press: Float = 0f,       // 0..1
    val touchX: Float = 0f,
    val touchY: Float = 0f,
    val touchAmp: Float = 0f,
    val isOverLight: Boolean = false,
)

/**
 * Attaches the AGSL lens to this layer. Returns `this` unchanged when [params] is
 * null/degenerate, on API < 33, or if the shader fails to compile (never throws into
 * the caller's UI).
 */
@Composable
fun Modifier.liquidGlassLens(params: GlassLensParams?): Modifier {
    if (params == null) return this
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    val primary = params.primary
    if (primary.halfWidth <= 0f || primary.halfHeight <= 0f) return this
    val effect = remember(params) {
        runCatching { buildLensEffect(params) }.getOrNull()
    } ?: return this
    return this.graphicsLayer {
        renderEffect = effect.asComposeRenderEffect()
        compositingStrategy = CompositingStrategy.Offscreen
    }
}

/**
 * Backwards-compatible entry point: maps the pill bounds reported by
 * `boundsInRoot()` onto [GlassLensParams]. Same signature as before, so existing
 * call sites keep compiling.
 */
@Composable
fun Modifier.liquidGlassBackdrop(rect: Rect?, dark: Boolean): Modifier {
    if (rect == null || rect.width <= 0f || rect.height <= 0f) return this
    val params = GlassLensParams(
        blurRadius = rect.height * 0.12f,
        primary = GlassShape(
            centerX = rect.center.x,
            centerY = rect.center.y,
            halfWidth = rect.width * 0.5f,
            halfHeight = rect.height * 0.5f,
            cornerRadius = rect.height * 0.5f,
        ),
        material = GlassMaterial.REGULAR,
        bevel = rect.height * 0.45f,
        refract = rect.height * 0.42f,
        dispersion = 0.16f,
        spec = if (dark) 0.30f else 0.45f,
        isOverLight = !dark,
    )
    return liquidGlassLens(params)
}

// =====================================================================================
// Effect construction
// =====================================================================================

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun buildLensEffect(params: GlassLensParams): RenderEffect {
    val shader = RuntimeShader(LENS_AGSL)
    val primary = params.primary
    val shortSide = min(primary.halfWidth, primary.halfHeight).coerceAtLeast(1f)
    // 小控件按短边收（库里的 adaptiveLensScale，只作用于没有 API 参数的高光带宽）
    val rimBandMax = (shortSide * 0.05f).coerceIn(2f, 6f)

    val material = params.material
    val blurScale = if (material == GlassMaterial.CLEAR) 0.35f else 1.0f
    val adaptiveTint = params.adaptiveTint
    // 自适应抬亮：Clear 追求极限通透，抬亮更克制
    val adaptiveAmt = if (material == GlassMaterial.CLEAR) 0.22f else 0.30f
    val specBoost = if (material == GlassMaterial.CLEAR) 1.2f else 1.0f
    // 全局染色（自适应染色关闭时生效）：亮背景下稍强的白色薄雾
    val baseTint = if (params.isOverLight) 0x1AFFFFFF else 0x10FFFFFF

    shader.setFloatUniform(
        "shape1",
        primary.centerX,
        primary.centerY,
        max(primary.halfWidth, 1f),
        max(primary.halfHeight, 1f),
    )
    shader.setFloatUniform("radius1", primary.cornerRadius)

    val secondary = params.secondary
    if (secondary != null && secondary.halfWidth > 0f && secondary.halfHeight > 0f) {
        shader.setFloatUniform(
            "shape2",
            secondary.centerX,
            secondary.centerY,
            secondary.halfWidth,
            secondary.halfHeight,
        )
        shader.setFloatUniform("radius2", secondary.cornerRadius)
        shader.setFloatUniform("blendK", max(params.smoothing, 0f))
    } else {
        // halfWidth <= 0.5 disables the secondary shape inside the shader
        shader.setFloatUniform("shape2", 0f, 0f, -1f, -1f)
        shader.setFloatUniform("radius2", 0f)
        shader.setFloatUniform("blendK", 0f)
    }

    shader.setFloatUniform("bevel", params.bevel)
    shader.setFloatUniform("refractPx", params.refract)
    shader.setFloatUniform("falloff", params.falloff)
    shader.setFloatUniform("dispersion", params.dispersion)
    shader.setFloatUniform("blurPx", params.blurRadius * blurScale)
    shader.setFloatUniform("lightDir", params.lightDirX, params.lightDirY)
    shader.setFloatUniform("specStrength", params.spec * specBoost)
    shader.setFloatUniform("rimBandMax", rimBandMax)
    setArgbUniform(shader, "tintColor", baseTint)
    shader.setFloatUniform("adaptiveTint", if (adaptiveTint) 1f else 0f)
    shader.setFloatUniform("adaptiveAmt", adaptiveAmt)
    setArgbUniform(shader, "glassTint", params.glassTint)
    shader.setFloatUniform("satFactor", params.saturation / 100f)
    shader.setFloatUniform("press", params.press.coerceIn(0f, 1f))
    shader.setFloatUniform("touchPos", params.touchX, params.touchY)
    shader.setFloatUniform("touchAmp", params.touchAmp)

    // No chained RenderEffect here: the shader's `content` input is the whole layer,
    // so blurring it upstream would soften every pixel outside the shape as well.
    // The glass's blur lives inside the shader ([blurSample]) and only applies to the
    // samples taken within the lens.
    return RenderEffect.createRuntimeShaderEffect(shader, "content")
}

private fun setArgbUniform(shader: RuntimeShader, name: String, argb: Int) {
    shader.setFloatUniform(
        name,
        ((argb shr 16) and 0xFF) / 255f,
        ((argb shr 8) and 0xFF) / 255f,
        (argb and 0xFF) / 255f,
        ((argb ushr 24) and 0xFF) / 255f,
    )
}

// =====================================================================================
// Sensor-driven light direction
// =====================================================================================

/** 默认光源方向（无传感器 / 设备平放时），与 [GlassLensParams] 默认值一致。 */
private val DEFAULT_GLASS_LIGHT = Offset(-0.45f, -0.9f)

/** 低通滤波系数（SENSOR_DELAY_GAME ≈ 50Hz） */
private const val LIGHT_SMOOTHING = 0.14f

/** 变化超过该值才提交新方向（避免静止时的无意义重组） */
private const val LIGHT_NOTIFY_EPSILON = 0.004f

/**
 * Gravity-sensor driven screen-space light direction, same semantics as the library's
 * `LightSourceController`: remap the gravity vector by display rotation, fall back to
 * the default direction while the device is (near) flat, low-pass filter, and only
 * publish changes past a threshold. Lifecycle aware; returns the default when
 * [enabled] is false or no gravity sensor is available.
 */
@Composable
fun rememberGlassLightDirection(enabled: Boolean): Offset {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var direction by remember { mutableStateOf(DEFAULT_GLASS_LIGHT) }
    val filtered = remember { FloatArray(2) }

    DisposableEffect(enabled, lifecycleOwner) {
        val sensorManager = context.applicationContext
            .getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || sensorManager == null || sensor == null) {
            return@DisposableEffect onDispose { }
        }

        filtered[0] = DEFAULT_GLASS_LIGHT.x
        filtered[1] = DEFAULT_GLASS_LIGHT.y

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val gx = event.values[0]
                val gy = event.values[1]

                // 按屏幕方向重映射到「当前朝向」坐标（x 右，y 上）
                val rotation = view.display?.rotation ?: Surface.ROTATION_0
                val ux: Float
                val uy: Float
                when (rotation) {
                    Surface.ROTATION_90 -> { ux = -gy; uy = gx }
                    Surface.ROTATION_180 -> { ux = -gx; uy = -gy }
                    Surface.ROTATION_270 -> { ux = gy; uy = -gx }
                    else -> { ux = gx; uy = gy }
                }

                // 屏幕平面内的重力分量大小 → 倾斜程度（0 = 平放，1 = 竖直）
                val planar = sqrt(ux * ux + uy * uy)
                val tilt = (planar / SensorManager.GRAVITY_EARTH).coerceIn(0f, 1f)

                // 光从「世界上方」来 → dir = -up（y 翻转到 view 坐标系后为 (-ux, uy)）
                var tx = DEFAULT_GLASS_LIGHT.x
                var ty = DEFAULT_GLASS_LIGHT.y
                if (planar > 0.5f) {
                    val inv = 1f / planar
                    val sx = -ux * inv
                    val sy = uy * inv
                    tx = sx * tilt + DEFAULT_GLASS_LIGHT.x * (1f - tilt)
                    ty = sy * tilt + DEFAULT_GLASS_LIGHT.y * (1f - tilt)
                }
                var len = sqrt(tx * tx + ty * ty)
                if (len > 1e-4f) {
                    tx /= len
                    ty /= len
                }

                // 低通滤波
                var nx = filtered[0] + (tx - filtered[0]) * LIGHT_SMOOTHING
                var ny = filtered[1] + (ty - filtered[1]) * LIGHT_SMOOTHING
                len = sqrt(nx * nx + ny * ny)
                if (len > 1e-4f) {
                    nx /= len
                    ny /= len
                }
                filtered[0] = nx
                filtered[1] = ny

                val current = direction
                if (abs(nx - current.x) > LIGHT_NOTIFY_EPSILON ||
                    abs(ny - current.y) > LIGHT_NOTIFY_EPSILON
                ) {
                    direction = Offset(nx, ny)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START ->
                    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
                Lifecycle.Event.ON_STOP -> sensorManager.unregisterListener(listener)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.unregisterListener(listener)
        }
    }

    return direction
}

// =====================================================================================
// Backdrop luminance (PixelCopy based replacement for BackdropLuminanceMeter)
// =====================================================================================

/** EMA/滞回状态，跨采样周期保留。 */
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
