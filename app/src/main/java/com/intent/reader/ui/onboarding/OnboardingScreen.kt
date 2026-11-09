package com.intent.reader.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.intent.reader.R
import com.intent.reader.ui.theme.IntentFonts
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.withCjkSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PAGES = 4
private const val PARALLAX = 0.12f
private const val PAGE_H_MARGIN = 24
private const val PAGE_TOP = 64
private const val PAGE_BOTTOM = 168
private const val PAGE_BOTTOM_SETUP = 208

/**
 * Onboarding: 4 pages — brand / daily quote / feed / profile.
 *
 * Motion choreography:
 * - entrance is a staggered reveal (each block springs up ~22dp with 70-90ms
 *   offsets) that replays when a page settles under the finger;
 * - the wordmark settles from wide tracking into its final spacing;
 * - page content runs at 0.12x the pager's translation, so layers separate
 *   while swiping;
 * - the CTA, dots, counter and skip affordance all animate, and every one of
 *   them falls back to a static layout when the system animator scale is 0.
 */
@Composable
fun OnboardingScreen(
    onFinish: (userName: String) -> Unit,
) {
    val c = IntentTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { PAGES })
    var userName by remember { mutableStateOf("") }
    var notifGranted by remember { mutableStateOf(hasNotifPermission(context)) }
    val motion = remember { animationsEnabled(context) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notifGranted = granted || hasNotifPermission(context) }

    val wordmark = remember {
        runCatching { FontFamily(Font("fonts/agibot_display_bold.ttf", context.assets)) }
            .getOrDefault(IntentFonts.serifHeader)
    }

    val page = pagerState.currentPage
    val last = page == PAGES - 1
    val progress by remember {
        derivedStateOf {
            ((pagerState.currentPage + pagerState.currentPageOffsetFraction) / (PAGES - 1))
                .coerceIn(0f, 1f)
        }
    }

    BackHandler(enabled = page > 0) {
        scope.launch { pagerState.animateScrollToPage(page - 1) }
    }

    fun advance() {
        if (last) onFinish(userName.trim())
        else scope.launch { pagerState.animateScrollToPage(page + 1) }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        GridBackdrop()

        // Hairline progress: how far through the four pages the reader is.
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = PAGE_H_MARGIN.dp, end = PAGE_H_MARGIN.dp)
                .height(2.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(c.accent),
            )
        }

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            val relative = (index - pagerState.currentPage) - pagerState.currentPageOffsetFraction
            val active = pagerState.currentPage == index
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Content trails the page, so foreground and background
                        // separate instead of sliding as one flat image.
                        translationX = -relative * size.width * PARALLAX
                        val d = kotlin.math.abs(relative).coerceIn(0f, 1f)
                        alpha = 1f - d * 0.85f
                        val s = 1f - d * 0.04f
                        scaleX = s
                        scaleY = s
                    },
            ) {
                when (index) {
                    0 -> BrandPage(wordmark, active, motion)
                    1 -> QuotePage(
                        title = stringResource(R.string.onboarding_quote_title),
                        desc = stringResource(R.string.onboarding_quote_desc),
                        note = stringResource(R.string.onboarding_quote_note),
                        sample = stringResource(R.string.onboarding_quote_sample),
                        active = active,
                        motion = motion,
                    )
                    2 -> FeedPage(
                        title = stringResource(R.string.onboarding_feed_title),
                        desc = stringResource(R.string.onboarding_feed_desc),
                        active = active,
                        motion = motion,
                    )
                    else -> SetupPage(
                        title = stringResource(R.string.onboarding_sources_title),
                        desc = stringResource(R.string.onboarding_sources_desc),
                        userName = userName,
                        onNameChange = { userName = it },
                        notifGranted = notifGranted,
                        onRequestNotif = {
                            if (Build.VERSION.SDK_INT >= 33 && !hasNotifPermission(context)) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notifGranted = hasNotifPermission(context)
                            }
                        },
                        active = active,
                        motion = motion,
                    )
                }
            }
        }

        OnboardingTopBar(
            wordmark = wordmark,
            skipVisible = !last,
            onSkip = { scope.launch { pagerState.animateScrollToPage(PAGES - 1) } },
        )

        OnboardingBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            page = page,
            last = last,
            motion = motion,
            label = if (last) stringResource(R.string.onboarding_start)
            else stringResource(R.string.onboarding_next),
            onAdvance = { advance() },
        )
    }
}

/** Swiss hairline grid behind every page. */
@Composable
private fun GridBackdrop() {
    val c = IntentTheme.colors
    val step = with(LocalDensity.current) { 48.dp.toPx() }
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                var x = step
                while (x < size.width) {
                    drawLine(
                        color = c.divider.copy(alpha = 0.55f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f,
                    )
                    x += step
                }
            },
    )
}

@Composable
private fun OnboardingTopBar(
    wordmark: FontFamily,
    skipVisible: Boolean,
    onSkip: () -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = PAGE_H_MARGIN.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "INTENT",
            fontFamily = wordmark,
            fontSize = 15.sp,
            letterSpacing = 3.sp,
            color = c.text,
        )
        AnimatedVisibility(
            visible = skipVisible,
            enter = fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 2 },
            exit = fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { it / 2 },
        ) {
            Text(
                text = stringResource(R.string.onboarding_skip),
                style = t.meta,
                color = c.textSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    modifier: Modifier = Modifier,
    page: Int,
    last: Boolean,
    motion: Boolean,
    label: String,
    onAdvance: () -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = PAGE_H_MARGIN.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(PAGES) { i ->
                    val active = page == i
                    val width by animateDpAsState(
                        targetValue = if (active) 26.dp else 7.dp,
                        animationSpec = if (motion) {
                            spring<Dp>(dampingRatio = 0.7f, stiffness = 520f)
                        } else {
                            snap<Dp>()
                        },
                        label = "dotWidth",
                    )
                    Box(
                        Modifier
                            .width(width)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(if (active) c.accent else c.divider),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${page + 1} / $PAGES",
                style = t.micro.copy(letterSpacing = 1.5.sp),
                color = c.textTertiary,
            )
        }
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(if (last) c.accent else c.text)
                .clickable(onClick = onAdvance),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = label,
                animationSpec = tween(220),
                label = "ctaLabel",
            ) { text ->
                Text(
                    text = text,
                    style = t.button.copy(fontWeight = FontWeight.SemiBold),
                    color = c.bg,
                )
            }
        }
    }
}

/**
 * Entrance primitive: springs the block up from [offsetY] with a stagger
 * [delayMillis]. Hoisted [active] means the reveal replays each time the page
 * becomes the settled page, instead of firing while it is still off-screen.
 */
@Composable
private fun Reveal(
    active: Boolean,
    motion: Boolean,
    delayMillis: Int,
    modifier: Modifier = Modifier,
    offsetY: Dp = 22.dp,
    content: @Composable () -> Unit,
) {
    val a = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(active, motion) {
        if (!motion) {
            a.snapTo(1f)
            return@LaunchedEffect
        }
        if (!active) {
            a.snapTo(0f)
            return@LaunchedEffect
        }
        delay(delayMillis.toLong())
        a.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.74f, stiffness = 360f),
        )
    }
    Box(
        modifier.graphicsLayer {
            alpha = a.value
            translationY = (1f - a.value) * offsetY.toPx()
            val s = 0.97f + 0.03f * a.value
            scaleX = s
            scaleY = s
        },
    ) { content() }
}

/** Token-level reveal: words (or CJK glyphs) focus in one after another. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TokenReveal(
    text: String,
    active: Boolean,
    motion: Boolean,
    style: TextStyle,
    color: Color,
    delayMillis: Int = 0,
) {
    val tokens = remember(text) {
        if (text.contains(' ')) text.split(' ').filter { it.isNotBlank() }
        else text.toCharArray().map { it.toString() }
    }
    val gap = if (text.contains(' ')) 5.dp else 1.dp
    val progress = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(active, motion) {
        if (!motion) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        if (!active) {
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        delay(delayMillis.toLong())
        progress.animateTo(1f, tween(760, easing = FastOutSlowInEasing))
    }
    val span = 1f / (tokens.size + 2)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        tokens.forEachIndexed { i, token ->
            val t = ((progress.value - i * span * 0.72f) / (span * 2.1f)).coerceIn(0f, 1f)
            Text(
                text = token,
                style = style,
                color = color.copy(alpha = color.alpha * t),
                modifier = Modifier.graphicsLayer {
                    translationY = (1f - t) * 12.dp.toPx()
                },
            )
        }
    }
}

/** Page 0: brand — wordmark, rule, slogan. */
@Composable
private fun BrandPage(wordmark: FontFamily, active: Boolean, motion: Boolean) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val big = LocalConfiguration.current.screenWidthDp >= 600

    val tracking = remember { Animatable(if (motion) 16f else 3f) }
    val appear = remember { Animatable(if (motion) 0f else 1f) }
    val rule = remember { Animatable(if (motion) 0f else 1f) }

    LaunchedEffect(active, motion) {
        if (!motion) {
            tracking.snapTo(3f)
            appear.snapTo(1f)
            rule.snapTo(1f)
            return@LaunchedEffect
        }
        if (!active) {
            tracking.snapTo(16f)
            appear.snapTo(0f)
            rule.snapTo(0f)
            return@LaunchedEffect
        }
        launch { appear.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
        launch { tracking.animateTo(3f, tween(1100, easing = FastOutSlowInEasing)) }
        delay(420)
        rule.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = PAGE_H_MARGIN.dp, vertical = 96.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "INTENT",
            fontFamily = wordmark,
            fontSize = if (big) 88.sp else (52 * t.scale).sp,
            color = c.text,
            letterSpacing = tracking.value.sp,
            modifier = Modifier.graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 24.dp.toPx()
                val s = 0.94f + 0.06f * appear.value
                scaleX = s
                scaleY = s
            },
        )
        Box(
            Modifier
                .padding(top = 18.dp)
                .height(2.dp)
                .width((72 * rule.value).dp)
                .background(c.accent),
        )
        Spacer(Modifier.height(22.dp))
        TokenReveal(
            text = stringResource(R.string.onboarding_slogan),
            active = active,
            motion = motion,
            style = t.body,
            color = c.textSecondary,
            delayMillis = 260,
        )
        Spacer(Modifier.height(6.dp))
        TokenReveal(
            text = stringResource(R.string.onboarding_slogan_zh).withCjkSpacing(),
            active = active,
            motion = motion,
            style = t.body,
            color = c.textSecondary,
            delayMillis = 420,
        )
        Spacer(Modifier.height(28.dp))
        Reveal(active, motion, 560) {
            Text(
                text = "RSS · NO ALGORITHM",
                style = t.micro.copy(letterSpacing = 2.sp),
                color = c.textTertiary,
            )
        }
    }
}

/** Pages 1-2 shared header: index, serif title with a drawn accent rule, copy. */
@Composable
private fun FeatureHeader(
    index: String,
    title: String,
    desc: String,
    note: String?,
    active: Boolean,
    motion: Boolean,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val underline = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(active, motion) {
        if (!motion) {
            underline.snapTo(1f)
            return@LaunchedEffect
        }
        if (!active) {
            underline.snapTo(0f)
            return@LaunchedEffect
        }
        delay(260)
        underline.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }

    Column {
        Reveal(active, motion, 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = index,
                    style = t.meta.copy(letterSpacing = 2.sp),
                    color = c.accent,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .width(26.dp)
                        .height(1.dp)
                        .background(c.accent.copy(alpha = 0.45f)),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Reveal(active, motion, 90) {
            Column {
                Text(
                    text = title.withCjkSpacing(),
                    fontFamily = t.serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = (26 * t.scale).sp,
                    lineHeight = (36 * t.scale).sp,
                    color = c.text,
                )
                Spacer(Modifier.height(12.dp))
                Box(Modifier.width(64.dp).height(2.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth(underline.value)
                            .height(2.dp)
                            .background(c.accent),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Reveal(active, motion, 170) {
            Text(
                text = desc.withCjkSpacing(),
                style = t.bodySmall,
                color = c.textSecondary,
            )
        }
        if (note != null) {
            Spacer(Modifier.height(14.dp))
            Reveal(active, motion, 240) {
                Text(
                    text = note.withCjkSpacing(),
                    style = t.micro.copy(letterSpacing = 0.4.sp),
                    color = c.textTertiary,
                )
            }
        }
    }
}

/** Page 1: daily quote. */
@Composable
private fun QuotePage(
    title: String,
    desc: String,
    note: String,
    sample: String,
    active: Boolean,
    motion: Boolean,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = PAGE_H_MARGIN.dp,
                end = PAGE_H_MARGIN.dp,
                top = PAGE_TOP.dp,
                bottom = PAGE_BOTTOM.dp,
            ),
    ) {
        FeatureHeader("01", title, desc, note, active, motion)
        Spacer(Modifier.height(28.dp))
        Reveal(active, motion, 200, offsetY = 28.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.surface)
                    .border(1.dp, c.divider, RoundedCornerShape(24.dp))
                    .padding(22.dp),
            ) {
                Text(
                    text = "\u201C",
                    fontFamily = t.serif,
                    fontSize = 44.sp,
                    lineHeight = 40.sp,
                    color = c.accent,
                )
                Text(
                    text = sample.withCjkSpacing(),
                    fontFamily = t.serif,
                    fontWeight = FontWeight.Medium,
                    fontSize = (20 * t.scale).sp,
                    lineHeight = (31 * t.scale).sp,
                    color = c.text,
                )
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(18.dp).height(1.dp).background(c.textTertiary))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "INTENT · DAILY",
                        style = t.micro.copy(letterSpacing = 1.5.sp),
                        color = c.textTertiary,
                    )
                }
            }
        }
    }
}

/** Page 2: the feed, sketched as three real card proportions. */
@Composable
private fun FeedPage(
    title: String,
    desc: String,
    active: Boolean,
    motion: Boolean,
) {
    val c = IntentTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = PAGE_H_MARGIN.dp,
                end = PAGE_H_MARGIN.dp,
                top = PAGE_TOP.dp,
                bottom = PAGE_BOTTOM.dp,
            ),
    ) {
        FeatureHeader("02", title, desc, null, active, motion)
        Spacer(Modifier.height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Reveal(
                active = active,
                motion = motion,
                delayMillis = 200,
                modifier = Modifier.weight(1f),
                offsetY = 26.dp,
            ) {
                SketchImageCard(Modifier.fillMaxWidth())
            }
            Reveal(
                active = active,
                motion = motion,
                delayMillis = 280,
                modifier = Modifier.weight(1f),
                offsetY = 26.dp,
            ) {
                SketchTextCard(Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(10.dp))
        Reveal(active, motion, 360, offsetY = 26.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.card)
                    .border(1.dp, c.divider, RoundedCornerShape(20.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(c.divider, c.surface)),
                        ),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    SkeletonBar(1f)
                    Spacer(Modifier.height(7.dp))
                    SkeletonBar(0.72f)
                }
            }
        }
    }
}

@Composable
private fun SketchImageCard(modifier: Modifier) {
    val c = IntentTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.card)
            .border(1.dp, c.divider, RoundedCornerShape(20.dp))
            .padding(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(c.divider, c.surface))),
        )
        Spacer(Modifier.height(10.dp))
        SkeletonBar(0.92f)
        Spacer(Modifier.height(7.dp))
        SkeletonBar(0.58f)
    }
}

@Composable
private fun SketchTextCard(modifier: Modifier) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.card)
            .border(1.dp, c.divider, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Text(
            text = "\u201C",
            fontFamily = t.serif,
            fontSize = 34.sp,
            lineHeight = 30.sp,
            color = c.accent,
        )
        Spacer(Modifier.height(6.dp))
        SkeletonBar(0.95f)
        Spacer(Modifier.height(7.dp))
        SkeletonBar(0.7f)
        Spacer(Modifier.height(7.dp))
        SkeletonBar(0.42f)
    }
}

@Composable
private fun SkeletonBar(fraction: Float) {
    val c = IntentTheme.colors
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(8.dp)
            .clip(CircleShape)
            .background(c.text.copy(alpha = 0.14f)),
    )
}

/** Page 3: profile — name, live greeting preview, notifications. */
@Composable
private fun SetupPage(
    title: String,
    desc: String,
    userName: String,
    onNameChange: (String) -> Unit,
    notifGranted: Boolean,
    onRequestNotif: () -> Unit,
    active: Boolean,
    motion: Boolean,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = PAGE_H_MARGIN.dp,
                end = PAGE_H_MARGIN.dp,
                top = PAGE_TOP.dp,
                bottom = PAGE_BOTTOM_SETUP.dp,
            ),
    ) {
        FeatureHeader("03", title, desc, null, active, motion)
        Spacer(Modifier.height(26.dp))
        Reveal(active, motion, 220) {
            TextField(
                value = userName,
                onValueChange = onNameChange,
                placeholder = {
                    Text(
                        stringResource(R.string.onboarding_name_hint),
                        color = c.textTertiary,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = c.surface,
                    unfocusedContainerColor = c.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = c.accent,
                    focusedTextColor = c.text,
                    unfocusedTextColor = c.text,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(
            visible = userName.isNotBlank(),
            enter = fadeIn(tween(240)) + slideInVertically(),
            exit = fadeOut(tween(140)),
        ) {
            Column(Modifier.padding(top = 22.dp)) {
                Text(
                    text = stringResource(R.string.greeting_hi),
                    style = t.greetingSerif.copy(fontWeight = FontWeight.Bold),
                    color = c.text,
                )
                Text(
                    text = userName.trim().withCjkSpacing(),
                    style = t.greetingSerif.copy(fontWeight = FontWeight.Bold),
                    color = c.text,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Reveal(active, motion, 300) {
            Row(
                Modifier
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = if (notifGranted) c.accent.copy(alpha = 0.6f) else c.divider,
                        shape = CircleShape,
                    )
                    .clickable(enabled = !notifGranted, onClick = onRequestNotif)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (notifGranted) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(c.accent),
                    )
                    Spacer(Modifier.width(9.dp))
                }
                Text(
                    text = stringResource(
                        if (notifGranted) R.string.onboarding_notify_on
                        else R.string.onboarding_notify_btn,
                    ),
                    style = t.button,
                    color = if (notifGranted) c.text else c.textSecondary,
                )
            }
        }
    }
}

private fun animationsEnabled(context: Context): Boolean =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) > 0f

private fun hasNotifPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
