package com.intent.reader.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.intent.reader.R
import com.intent.reader.ui.theme.IntentTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import kotlin.math.roundToInt

enum class IntentTab { HOME, NOTES, SEARCH }

/** Main capsule: Home / Notes. Search stays in a separate circular control. */
@Composable
fun GlassTabBar(
    selected: IntentTab,
    onSelect: (IntentTab) -> Unit,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
    liquidGlass: Boolean = false,
    backdrop: Backdrop? = null,
    clearMaterial: Boolean = false,
    adaptiveTint: Boolean = true,
    overLight: Boolean = false,
    transparency: Float = 0.5f,
    reduceMotion: Boolean = false,
    split: Boolean = false,
) {
    // Do not leave invisible, focusable navigation targets over the keyboard.
    if (hidden) return
    val advanced = liquidGlass && backdrop != null
    val c = IntentTheme.colors
    val mainTabs = listOf(IntentTab.HOME, IntentTab.NOTES)
    val selectedIndex = mainTabs.indexOf(selected)
    var previousTab by remember { mutableStateOf(selected) }
    var dragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var panelPressed by remember { mutableStateOf(false) }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val position = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    // The main thumb is invisible in Search. Move it directly to the chosen
    // destination before it fades back in; only main-to-main changes slide.
    val returningFromSearch = selectedIndex >= 0 && previousTab == IntentTab.SEARCH
    LaunchedEffect(selected, dragging, dragPosition, advanced) {
        if (dragging) {
            position.snapTo(dragPosition)
        } else {
            val fromSearch = previousTab == IntentTab.SEARCH
            if (selectedIndex < 0) {
                previousTab = selected
            } else if (fromSearch) {
                position.snapTo(selectedIndex.toFloat())
                previousTab = selected
            } else {
                previousTab = selected
                position.animateTo(selectedIndex.toFloat(), if (advanced) spring(0.78f, 380f) else spring(1f, 900f))
            }
        }
    }
    val press by animateFloatAsState(
        targetValue = if (panelPressed) 1f else 0f,
        animationSpec = spring(0.7f, 450f), label = "tabPanelPress",
    )
    val track = if (advanced) rememberLayerBackdrop() else null
    val thumbAlpha by animateFloatAsState(if (selectedIndex >= 0 || dragging) 1f else 0f, tween(140), label = "mainTabSelection")
    val thumbBackdrop = if (advanced && track != null) {
        rememberCombinedBackdrop(backdrop, track)
    } else null
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // On wide screens the bar spans the whole width so the primary tabs sit under the
    // left thumb and search under the right one; compact phones keep the centred pill.
    Row(
        modifier.navigationBarsPadding()
            .padding(horizontal = if (split) 24.dp else 20.dp, vertical = 12.dp)
            .then(if (split) Modifier.fillMaxWidth() else Modifier.widthIn(max = 360.dp).fillMaxWidth())
            .selectableGroup(),
        horizontalArrangement = if (split) Arrangement.SpaceBetween else Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(Modifier.then(if (split) Modifier.width(284.dp) else Modifier.weight(1f)).height(64.dp)) {
            val slotWidth = (maxWidth - 8.dp) / mainTabs.size
            val maxPosition = mainTabs.lastIndex.toFloat()
            val slotPx = with(density) { slotWidth.toPx() }
            val insetPx = with(density) { 4.dp.toPx() }
            val displayedPosition = when {
                dragging -> dragPosition
                returningFromSearch -> selectedIndex.toFloat()
                else -> position.value
            }
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(advanced) {
                        try {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                panelPressed = true
                                do { val held = awaitPointerEvent().changes.any { it.pressed } } while (held)
                                panelPressed = false
                            }
                        } finally { panelPressed = false }
                    }
                    .pointerInput(slotPx, rtl) {
                        try {
                            detectHorizontalDragGestures(
                                onDragStart = { point ->
                                    val x = if (rtl) size.width - point.x else point.x
                                    dragPosition = ((x - insetPx) / slotPx - 0.5f).coerceIn(0f, maxPosition)
                                    dragging = true
                                },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    dragPosition = (dragPosition + amount / slotPx * if (rtl) -1 else 1)
                                        .coerceIn(0f, maxPosition)
                                },
                                onDragEnd = {
                                    currentOnSelect(mainTabs[dragPosition.roundToInt()])
                                    dragging = false
                                },
                                onDragCancel = { dragging = false },
                            )
                        } finally { dragging = false }
                    },
            ) {
                // Keep the track's clipped glass layer separate from the thumb,
                // so it cannot clip the thumb where it extends past the track.
                Box(
                    Modifier.fillMaxSize().navigationSurface(
                        advanced, backdrop, clearMaterial, adaptiveTint,
                        overLight, press, exportedBackdrop = track, transparency = transparency,
                    ),
                )
                // Export only the surface, never the thumb, to avoid sampling itself.
                Box(
                    Modifier.padding(4.dp).offset(x = slotWidth * displayedPosition)
                        .width(slotWidth).height(56.dp)
                        .graphicsLayer { alpha = thumbAlpha }
                        .navigationSurface(advanced, thumbBackdrop, clearMaterial,
                            adaptiveTint, overLight, press, thumb = true, transparency = transparency),
                )
                Row(Modifier.fillMaxSize().padding(4.dp)) {
                    mainTabs.forEach { tab ->
                        NavigationItem(
                            tab = tab,
                            selected = selected == tab,
                            tintOverride = if (advanced) c.textSecondary else null,
                            useFilledSelection = !advanced,
                            reduceMotion = reduceMotion,
                            onClick = { onSelect(tab) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
                if (advanced) {
                    // Draw an accent-colored copy only where the moving glass overlaps it.
                    Row(
                        Modifier.fillMaxSize().padding(4.dp)
                            .clearAndSetSemantics { }
                            .graphicsLayer { alpha = thumbAlpha }
                            .drawWithContent {
                                val scale = 1f + press * 0.30f
                                val center = if (rtl) size.width - slotPx * (displayedPosition + 0.5f)
                                    else slotPx * (displayedPosition + 0.5f)
                                val width = slotPx * scale
                                val height = size.height * scale
                                val top = (size.height - height) / 2f
                                val radius = height / 2f
                                val path = Path().apply {
                                    addRoundRect(RoundRect(center - width / 2f, top, center + width / 2f, top + height,
                                        CornerRadius(radius, radius)))
                                }
                                clipPath(path) { this@drawWithContent.drawContent() }
                            },
                    ) {
                        mainTabs.forEach { tab ->
                            NavigationItem(
                                tab = tab, selected = false, onClick = {},
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                tintOverride = c.accent, interactive = false,
                            )
                        }
                    }
                }
            }
        }
        val settingsInteraction = remember { MutableInteractionSource() }
        val settingsPressed by settingsInteraction.collectIsPressedAsState()
        val settingsPress by animateFloatAsState(if (settingsPressed) 1f else 0f, spring(0.7f, 450f), label = "settingsPress")
        NavigationItem(
            tab = IntentTab.SEARCH,
            selected = selected == IntentTab.SEARCH,
            onClick = { onSelect(IntentTab.SEARCH) },
            interactionSource = settingsInteraction,
            useFilledSelection = !advanced,
            reduceMotion = reduceMotion,
            modifier = Modifier.size(64.dp)
                .navigationSurface(advanced, backdrop, clearMaterial, adaptiveTint, overLight, settingsPress, transparency = transparency)
                .background(if (selected == IntentTab.SEARCH) c.accent.copy(alpha = 0.12f) else Color.Transparent, CircleShape),
        )
    }
}

/** Scaling belongs to Backdrop's layerBlock so sampled page pixels stay anchored. */
@Composable
internal fun Modifier.navigationSurface(
    advanced: Boolean,
    backdrop: Backdrop?,
    clear: Boolean,
    adaptive: Boolean,
    overLight: Boolean,
    press: Float,
    thumb: Boolean = false,
    exportedBackdrop: LayerBackdrop? = null,
    transparency: Float = 0.5f,
    readableOnPlainBackground: Boolean = false,
): Modifier {
    val c = IntentTheme.colors
    val transform: GraphicsLayerScope.() -> Unit = {
        val scale = if (advanced) 1f + press * (if (thumb) 0.30f else 0.035f)
            else 1f - press * 0.025f
        scaleX = scale
        scaleY = scale
    }
    return if (advanced && backdrop != null) {
        drawBackdrop(
            backdrop = backdrop,
            shape = { CircleShape },
            exportedBackdrop = exportedBackdrop,
            effects = {
                if (!thumb) {
                    colorControls(saturation = if (clear) 1f else 1.12f)
                    blur((8.dp - 6.dp * transparency.coerceIn(0f, 1f)).toPx())
                }
                lens(12.dp.toPx(),
                    (if (thumb) 20.dp else 14.dp).toPx(), chromaticAberration = true)
            },
            layerBlock = transform,
            highlight = { Highlight(width = if (thumb) 0.35.dp else 0.6.dp, alpha = if (thumb) 0.5f else 0.55f) },
            shadow = null,
            onDrawSurface = {
                // Header buttons sample the plain page surface. A subtle tint in
                // the opposite direction keeps their silhouette visible even on
                // a white or black page, without relying on a heavy outline.
                val tint = if (thumb) c.accent else if (readableOnPlainBackground) {
                    if (c.isDark) Color.White else Color.Black
                } else if (c.isDark) Color.Black else Color.White
                val opacity = if (thumb) 0.09f else if (readableOnPlainBackground) {
                    0.14f - 0.08f * transparency.coerceIn(0f, 1f)
                } else
                    (0.38f - 0.30f * transparency.coerceIn(0f, 1f) +
                        if (adaptive && !overLight) 0.06f else 0f).coerceIn(0f, 1f)
                drawRect(tint.copy(alpha = opacity))
            },
        ).then(if (thumb) Modifier else Modifier.border(0.6.dp,
            (if (c.isDark) Color.White else Color.Black).copy(alpha = 0.12f), CircleShape))
    } else {
        graphicsLayer(transform).clip(CircleShape)
            .background(if (thumb) c.accent.copy(alpha = 0.12f) else c.glass)
            .border(if (thumb) 0.dp else 1.dp, c.divider, CircleShape)
    }
}

@Composable
private fun NavigationItem(
    tab: IntentTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    tintOverride: Color? = null,
    interactive: Boolean = true,
    useFilledSelection: Boolean = false,
    reduceMotion: Boolean = false,
) {
    val c = IntentTheme.colors
    val label = stringResource(when (tab) {
        IntentTab.HOME -> R.string.tab_home
        IntentTab.NOTES -> R.string.nav_notes
        IntentTab.SEARCH -> R.string.tab_search
    })
    val icon: ImageVector = when (tab) {
        IntentTab.HOME -> Icons.Outlined.Home
        IntentTab.NOTES -> Icons.Outlined.Bookmarks
        IntentTab.SEARCH -> Icons.Outlined.Search
    }
    val filledIcon: ImageVector = when (tab) {
        IntentTab.HOME -> Icons.Filled.Home
        IntentTab.NOTES -> Icons.Filled.Bookmarks
        IntentTab.SEARCH -> Icons.Filled.Search
    }
    val tint by animateColorAsState(if (selected) c.accent else c.textSecondary,
        animationSpec = tween(160), label = "navigationTint")
    Column(
        if (interactive) modifier.then(
            if (useFilledSelection && !selected) Modifier.semantics { contentDescription = label } else Modifier
        ).selectable(selected, interactionSource, indication = null,
            role = Role.Tab, onClick = onClick) else modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Crossfade(
            targetState = useFilledSelection && selected,
            animationSpec = tween(if (reduceMotion) 0 else 160),
            modifier = Modifier.size(23.dp),
            label = "navigationIconSelection",
        ) { filled ->
            Icon(if (filled) filledIcon else icon, contentDescription = null, tint = tintOverride ?: tint)
        }
        if (useFilledSelection) {
            AnimatedVisibility(
                visible = selected,
                enter = if (reduceMotion) EnterTransition.None else
                    expandVertically(animationSpec = tween(160)) + fadeIn(tween(160)),
                exit = if (reduceMotion) ExitTransition.None else
                    shrinkVertically(animationSpec = tween(160)) + fadeOut(tween(120)),
            ) {
                Text(label, color = tintOverride ?: tint, style = IntentTheme.typography.micro,
                    maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            }
        } else {
            Text(label, color = tintOverride ?: tint, style = IntentTheme.typography.micro,
                maxLines = 1, modifier = Modifier.padding(top = 2.dp))
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
