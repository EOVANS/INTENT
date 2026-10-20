package com.intent.reader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import com.intent.reader.ui.theme.IntentDimens
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import com.intent.reader.R
import com.intent.reader.ui.theme.IntentTheme

enum class IntentTab { HOME, SEARCH, SETTINGS }

/**
 * Shared duration for the tab bar's colour / label / indicator transitions, so a tap
 * reads as one continuous motion next to the scale spring (which settles in about the
 * same time) instead of the colour landing first while the scale is still travelling.
 */
private const val TAB_ANIM_MS = 200

/**
 * Floating glass capsule tab bar (doc 9).
 *
 * Two styles share the same layout contract (pill radius, pinned on the main pages):
 *  - classic: theme glass fill + hairline border, 56dp pill;
 *  - [liquidGlass] (Android 13+): taller 64dp pill that draws **nothing** of its own —
 *    no fill, no border — because the AGSL lens on the layer beneath is the only
 *    renderer. Stacking a Compose fill on top of it turns the refraction into a grey
 *    haze, and the hairline border reads as a black capsule outline on light themes.
 */
@Composable
fun GlassTabBar(
    selected: IntentTab,
    onSelect: (IntentTab) -> Unit,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
    liquidGlass: Boolean = false,
    onPillBounds: ((Rect) -> Unit)? = null,
    /** (position in root coordinates, isDown) — drives the pressed-liquid bulge. */
    onPress: ((Offset, Boolean) -> Unit)? = null,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val pillShape = RoundedCornerShape(if (liquidGlass) 32.dp else IntentDimens.radiusTab.dp)
    var pillTopLeft by remember { mutableStateOf(Offset.Zero) }
    val pressCb = rememberUpdatedState(onPress)

    val tabs = listOf(
        Triple(IntentTab.HOME, Icons.Outlined.Home, R.string.tab_home),
        Triple(IntentTab.SEARCH, Icons.Outlined.Search, R.string.tab_search),
        Triple(IntentTab.SETTINGS, Icons.Outlined.Settings, R.string.tab_settings),
    )

    val barAlpha by animateFloatAsState(
        targetValue = if (hidden) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabAlpha",
    )
    val barOffset by animateDpAsState(
        targetValue = if (hidden) 96.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabOffset",
    )

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .graphicsLayer {
                alpha = barAlpha
                translationY = barOffset.toPx()
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(if (liquidGlass) 64.dp else 56.dp)
                .clip(pillShape)
                .then(
                    if (liquidGlass) {
                        // The AGSL lens behind this layer is the sole renderer. Any
                        // Compose fill or stroke stacked on top of it (this used to be a
                        // white gradient plus a 1.2dp black hairline) reads as a grey
                        // haze, so the pill stays completely see-through and borderless
                        // and the refraction is the only thing you see.
                        Modifier
                    } else {
                        Modifier
                            .background(SolidColor(c.glass))
                            .border(
                                width = 1.dp,
                                color = (if (c.isDark) Color.White else Color.Black).copy(
                                    alpha = if (c.isDark) 0.15f else 0.22f
                                ),
                                shape = pillShape,
                            )
                    }
                )
                .onGloballyPositioned { coords ->
                    val bounds = coords.boundsInRoot()
                    pillTopLeft = bounds.topLeft
                    onPillBounds?.invoke(bounds)
                }
                .pointerInput(liquidGlass) {
                    if (!liquidGlass) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            // Reported once per gesture: the bulge only needs the
                            // press-down point, and moving it every frame would
                            // rebuild the shader on each pointer move.
                            val down = awaitFirstDown(requireUnconsumed = false)
                            pressCb.value?.invoke(pillTopLeft + down.position, true)
                            var change = down
                            while (change.pressed) {
                                val event = awaitPointerEvent()
                                change = event.changes.firstOrNull { it.id == down.id } ?: break
                            }
                            pressCb.value?.invoke(pillTopLeft + change.position, false)
                        }
                    }
                }
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { (tab, icon, labelRes) ->
                val isSelected = tab == selected
                val label = stringResource(labelRes)
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.94f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "tabScale",
                )
                // 强调色着色（试验）：tab 的图标与标签都取用户当前的强调色，选中态用满色、
                // 未选中态压到 55% 透明度以保留选中层级。
                // 回滚方式：把这段 iconTint 删掉，并另建一份 text/textSecondary 的颜色动画
                // 分别给 Icon 的 tint 与 Text 的 color。
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) c.accent else c.accent.copy(alpha = 0.55f),
                    animationSpec = tween(TAB_ANIM_MS),
                    label = "tabIconTint",
                )
                // The selected-tab indicator fades along with the rest of the state change
                // instead of snapping in, so one tap reads as a single continuous motion.
                val indicator by animateColorAsState(
                    targetValue = if (liquidGlass && isSelected) {
                        if (c.isDark) Color.White.copy(alpha = 0.14f)
                        else Color.Black.copy(alpha = 0.06f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(TAB_ANIM_MS),
                    label = "tabIndicator",
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(if (liquidGlass) 26.dp else IntentDimens.radiusTab.dp))
                        .background(indicator)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(tab) }
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .scale(scale),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp),
                        )
                        // AnimatedVisibility collapses the label's space as it fades, so the
                        // icon glides between the "icon only" and "icon + label" positions
                        // instead of jumping — and an unselected tab still measures as a bare
                        // icon, which keeps it vertically centred in the bar.
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(tween(TAB_ANIM_MS)) +
                                expandVertically(tween(TAB_ANIM_MS)),
                            exit = fadeOut(tween(TAB_ANIM_MS)) +
                                shrinkVertically(tween(TAB_ANIM_MS)),
                        ) {
                            Text(
                                text = label,
                                style = t.micro,
                                color = iconTint,
                                modifier = Modifier.padding(top = 1.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Scroll behavior: hides on scroll down, shows on scroll up. */
class TabScrollBehavior {
    var visible by mutableStateOf(true)
        private set

    /** Force-show; called when the user lands on a top-level tab. */
    fun show() {
        visible = true
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
            if (source == NestedScrollSource.UserInput || source == NestedScrollSource.SideEffect) {
                val dy = available.y
                if (dy < -8f) visible = false
                else if (dy > 8f) visible = true
            }
            return androidx.compose.ui.geometry.Offset.Zero
        }
    }
}

@Composable
fun rememberTabScrollBehavior(): TabScrollBehavior {
    val behavior = remember { TabScrollBehavior() }
    return behavior
}
