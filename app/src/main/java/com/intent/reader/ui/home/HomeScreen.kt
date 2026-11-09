package com.intent.reader.ui.home

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import com.intent.reader.ui.components.HeaderIconButton
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.intent.reader.R
import com.intent.reader.data.db.Article
import com.intent.reader.data.db.DailyQuote
import com.intent.reader.data.repo.HighlightRange
import com.intent.reader.domain.CardStyle
import com.intent.reader.ui.components.ArticleCard
import com.intent.reader.ui.components.CardE
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.withCjkSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Grid columns per window size class (doc 8). */
fun articleColumns(widthClass: WindowWidthSizeClass): Int = when (widthClass) {
    WindowWidthSizeClass.Medium -> 2
    WindowWidthSizeClass.Expanded -> 2
    else -> 2
}

/** True for styles that occupy a single column on phones. */
private fun isHalfWidthStyle(style: CardStyle): Boolean =
    style == CardStyle.A || style == CardStyle.B

/**
 * Column span for every row, in feed order.
 *
 * - Phones (2 columns): A/B take one column, C/D/E take the whole row. A trailing
 *   orphan A/B — i.e. the last card of an odd-length run of half-width cards —
 *   is promoted to the full row so the right column is never left empty.
 * - Medium screens: every card takes one of two columns.
 * - Expanded screens also use two independently flowing columns.
 */
fun computeSpans(rows: List<HomeRowUi>, widthClass: WindowWidthSizeClass): List<Int> {
    val total = articleColumns(widthClass)
    val compact = widthClass == WindowWidthSizeClass.Compact
    val spans = rows.map { row ->
        when (row) {
            is HomeRowUi.Aggregate -> if (compact) total else if (total == 2) 1 else 2
            is HomeRowUi.Single ->
                if (isHalfWidthStyle(row.a.style)) 1
                else if (compact) total else if (total == 2) 1 else 2
        }
    }.toMutableList()

    // Odd run of half-width cards would leave the second column empty.
    if (compact && total == 2) {
        var i = 0
        while (i < spans.size) {
            if (spans[i] == 1) {
                var j = i
                while (j < spans.size && spans[j] == 1) j++
                if ((j - i) % 2 == 1) spans[j - 1] = total
                i = j
            } else {
                i++
            }
        }
    }
    return spans
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    widthClass: WindowWidthSizeClass,
    onRefresh: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenCard: (String) -> Unit = onOpenArticle,
    cardTransitionModifier: @Composable (String) -> Modifier = { Modifier },
    onSetRead: (String, Boolean) -> Unit,
    onOpenSource: (String) -> Unit,
    onRefreshAiQuote: () -> Unit,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollToTopTick: Int = 0,
) {
    val c = IntentTheme.colors
    val isTablet = widthClass != WindowWidthSizeClass.Compact
    val hMargin = if (isTablet) IntentDimens.pageHMarginTablet.dp else IntentDimens.pageHMarginPhone.dp
    // The feed is edge-to-edge, so the grid has to clear the status bar itself.
    val statusBarTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    val spans = remember(state.rows, widthClass) { computeSpans(state.rows, widthClass) }
    val gridState = rememberLazyStaggeredGridState()
    val scope = rememberCoroutineScope()
    val refreshState = rememberPullToRefreshState()
    val pull = refreshState.distanceFraction.coerceIn(0f, 3f)
    val density = LocalDensity.current

    // Re-tapping the Home tab (or tapping the status-bar area) scrolls the feed
    // back to the greeting header.
    var consumedScrollToTopTick by rememberSaveable { mutableIntStateOf(scrollToTopTick) }
    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick != consumedScrollToTopTick) {
            consumedScrollToTopTick = scrollToTopTick
            gridState.animateScrollToItem(0)
        }
    }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        state = refreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            if (pull > 0f || state.refreshing) {
                Column(
                    modifier = Modifier.align(Alignment.TopCenter)
                        .padding(top = statusBarTop + 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (state.refreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = c.textSecondary, strokeWidth = 2.dp)
                    } else {
                        Box(Modifier.width(24.dp).height((24f + 52f * pull).dp)
                            .border(2.dp, c.textSecondary.copy(alpha = (0.35f + pull * 0.35f).coerceAtMost(0.75f)), RoundedCornerShape(50)))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(if (state.refreshing) R.string.home_refreshing else if (pull >= 1f) R.string.home_release_refresh else R.string.home_pull_refresh),
                        style = IntentTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = with(density) { (pull * 100.dp.toPx()) } }) {
            if (state.rows.isEmpty() && state.loaded) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = hMargin),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.empty_feed_title), style = IntentTheme.typography.pageTitle)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.empty_feed_desc),
                    style = IntentTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
                HeaderIconButton(Icons.Outlined.Settings, stringResource(R.string.tab_settings), onOpenSettings)
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(articleColumns(widthClass)),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = hMargin,
                    end = hMargin,
                    top = contentPadding.calculateTopPadding() + statusBarTop + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 96.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(IntentDimens.cardSpacing.dp),
                verticalItemSpacing = IntentDimens.cardSpacing.dp,
            ) {
                item(span = StaggeredGridItemSpan.FullLine, key = "header") {
                    GreetingHeader(
                        userName = state.userName,
                        greetingEmoji = state.greetingEmoji,
                        avatarPath = state.avatarPath,
                        quotes = state.quotes,
                        reduceAnimations = state.reduceAnimations,
                        onOpenArticle = onOpenArticle,
                        modifier = Modifier.padding(top = 24.dp),
                        aiQuote = state.aiQuote,
                        onRefreshAiQuote = onRefreshAiQuote,
                        onOpenSettings = onOpenSettings,
                    )
                }
                state.rows.forEachIndexed { index, row ->
                    when (row) {
                        is HomeRowUi.Single -> {
                            item(key = row.a.article.id, span = if (spans[index] > 1) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane) {
                                LongPressCard(
                                    article = row.a.article,
                                    onSetRead = onSetRead,
                                    modifier = cardTransitionModifier(row.a.article.id),
                                ) {
                                    ArticleCard(
                                        style = row.a.style,
                                        article = row.a.article,
                                        sourceTitle = row.a.sourceTitle,
                                        sourceLogo = row.a.sourceLogo,
                                        onClick = { onOpenCard(row.a.article.id) },
                                        aligned = !isTablet && spans[index] == 1,
                                    )
                                }
                            }
                        }
                        is HomeRowUi.Aggregate -> {
                            item(
                                key = "agg_" + (row.items.firstOrNull()?.article?.id ?: row.hashCode()),
                                span = if (spans[index] > 1) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane,
                            ) {
                                CardE(
                                    sourceTitle = row.sourceTitle,
                                    sourceLogo = row.sourceLogo,
                                    items = row.items.map { it.article },
                                    onViewAll = {
                                        onOpenSource(row.items.firstOrNull()?.article?.sourceId ?: "")
                                    },
                                    onArticle = { onOpenCard(it.id) },
                                    articleTransitionModifier = { cardTransitionModifier(it.id) },
                                )
                            }
                        }
                    }
                }
            }
            }

            // Top status-bar fade: a vertical gradient scrim from solid bg ->
            // transparent gives the illusion of a blur transition as content
            // scrolls under the system status bar. Tapping the strip scrolls the
            // feed back to the top (like tapping the iOS status bar); drags still
            // fall through to pull-to-refresh. The strip is taller than the
            // visual fade needs because the system status bar swallows taps over
            // its own footprint — the extra height keeps a usable hit area just
            // below it.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to c.bg,
                                0.6f to c.bg.copy(alpha = 0.85f),
                                1f to c.bg.copy(alpha = 0f),
                            ),
                        )
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        scope.launch { gridState.animateScrollToItem(0) }
                    },
            )
        }
    }
}

/** Long-press menu: read/unread, share (doc 4.1.3). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LongPressCard(
    article: Article,
    onSetRead: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier) {
        Box(
            Modifier.combinedClickable(
                onClick = {},
                onLongClick = { menuOpen = true },
            )
        ) { content() }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (article.isRead) R.string.mark_unread else R.string.mark_read)) },
                onClick = {
                    menuOpen = false
                    onSetRead(article.id, !article.isRead)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share)) },
                onClick = {
                    menuOpen = false
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "${article.title}\n${article.link}")
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(intent, article.title)
                    )
                },
            )
        }
    }
}

/** Circular user avatar shown in the top-right corner of the greeting header. */
@Composable
private fun UserAvatar(path: String, name: String, size: androidx.compose.ui.unit.Dp = 52.dp) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(c.heroBg)
            .border(1.dp, c.divider, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (path.isNotBlank() && java.io.File(path).exists()) {
            coil.compose.AsyncImage(
                model = java.io.File(path),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Fallback: first letter of the user name, or a neutral dot.
            Text(
                text = name.trim().take(1).ifBlank { "·" },
                style = (if (size < 40.dp) t.cardTitle else t.greetingSerif).copy(fontWeight = FontWeight.Bold),
                color = c.textSecondary,
            )
        }
    }
}

/** Serif greeting header with red-underlined daily quote (doc 4.1.1). */
@Composable
fun GreetingHeader(
    userName: String,
    greetingEmoji: String = "",
    avatarPath: String = "",
    quotes: List<DailyQuote>,
    reduceAnimations: Boolean,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
    aiQuote: HomeAiQuoteState = HomeAiQuoteState(),
    onRefreshAiQuote: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val hi = stringResource(R.string.greeting_first_line)
    val todayPrefix = stringResource(R.string.today_prefix)

    val dateText = run {
        val d = LocalDate.now()
        if (Locale.getDefault().language == "zh") {
            "${d.monthValue} 月 ${d.dayOfMonth} 日"
        } else {
            d.format(DateTimeFormatter.ofPattern("MMM/d", Locale.ENGLISH))
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // "Hi," on its own line, bold serif
                Text(
                    text = hi,
                    style = t.greetingSerif.copy(fontWeight = FontWeight.Bold),
                    color = c.text,
                )
                Text(
                    text = stringResource(R.string.greeting_welcome_line) +
                        if (greetingEmoji.isBlank()) "" else " $greetingEmoji",
                    style = t.greetingSerif.copy(fontWeight = FontWeight.Bold),
                    color = c.text,
                )
                // User name on the next line, same bold serif
                if (userName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = userName.withCjkSpacing(),
                            style = t.greetingSerif.copy(fontWeight = FontWeight.Bold),
                            color = c.text,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        UserAvatar(path = avatarPath, name = userName, size = 36.dp)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            HeaderIconButton(Icons.Outlined.Settings, stringResource(R.string.tab_settings), onOpenSettings)
        }
        Spacer(Modifier.height(8.dp))
        // Date row: "今天是" in tertiary grey + date inside a blurred grey capsule
        // with orange text.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = todayPrefix.withCjkSpacing(),
                style = t.dateSerif,
                color = c.textTertiary,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(c.textTertiary.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = dateText,
                    style = t.dateSerif.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFFB45309), // amber-700, doc accent-adjacent warm tone
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        QuoteBlock(
            quotes = quotes,
            reduceAnimations = reduceAnimations,
            onOpenArticle = onOpenArticle,
        )

        if (aiQuote.enabled) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (aiQuote.loading) R.string.ai_loading
                        else if (aiQuote.quote != null) R.string.ai_quote_ready else R.string.ai_quote),
                    style = t.micro, color = c.textSecondary, modifier = Modifier.weight(1f),
                )
                TextButton(enabled = !aiQuote.loading, onClick = onRefreshAiQuote) {
                    Text(stringResource(R.string.ai_quote_retry), style = t.micro)
                }
            }
            aiQuote.message?.let { Text(stringResource(it), style = t.micro, color = c.textSecondary) }
        }

        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.divider)
        )
        Spacer(Modifier.height(20.dp))
    }
}

// ---------- Daily quote: rotation + focus-in animation (金句动效文档) ----------

private const val QUOTE_ROTATE_MS = 5_000L
private const val TOKEN_DURATION_MS = 360
private const val TOKEN_STAGGER_MAX_MS = 40
// Stagger budget keeps a long sentence's last word focusing within ~1.2s so the
// whole sequence stays inside the 1.5–2s envelope.
private const val TOKEN_STAGGER_BUDGET_MS = 1200
private const val META_DELAY_MS = 250
private const val META_DURATION_MS = 500
private const val UNDERLINE_EXTRA_DELAY_MS = 100L

/**
 * Rotating quote block: advances to the next quote every 5s; each new quote
 * plays the focus-in (镜头对焦) entrance — words deblur left to right, the
 * capsule arrow springs in, the meta line follows, and the red keyword
 * underline fades in last.
 */
@Composable
fun QuoteBlock(
    quotes: List<DailyQuote>,
    reduceAnimations: Boolean,
    onOpenArticle: (String) -> Unit,
) {
    if (quotes.isEmpty()) return
    val context = LocalContext.current
    // Honour both the in-app reduce-animations switch and the system's
    // animator-duration-scale=0 ("remove animations").
    val systemAnimationsOn = remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
    val animationsEnabled = !reduceAnimations && systemAnimationsOn
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var index by rememberSaveable { mutableIntStateOf(0) }
    var displayedText by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(quotes, lifecycle) {
        if (index >= quotes.size) index = 0
        if (quotes.size <= 1) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(QUOTE_ROTATE_MS)
                index = (index + 1) % quotes.size
            }
        }
    }
    val quote = quotes.getOrElse(index) { quotes.first() }
    // Preserve the current quote on tab restoration without replaying dozens of
    // per-token blur animations. Newly rotated quotes still animate normally.
    val playEntrance = remember(quote.text, animationsEnabled) { animationsEnabled && displayedText != quote.text }
    LaunchedEffect(quote.text) { displayedText = quote.text }

    // Keying on the text replays the whole focus sequence for each rotation.
    key(quote.text, animationsEnabled) {
        FocusQuote(
            quote = quote,
            animationsEnabled = playEntrance,
            onOpenArticle = onOpenArticle,
        )
    }
}

/** One word/character of the quote plus its range in the spaced string. */
private data class QuoteToken(val text: String, val start: Int, val end: Int)

/**
 * Splits the quote into animatable tokens: latin words stay whole (trailing
 * space attached), CJK characters animate individually, punctuation merges
 * into the preceding token so commas/periods focus together with their word.
 */
private fun tokenizeQuote(text: String): List<QuoteToken> {
    fun isLatinWordChar(ch: Char): Boolean =
        (ch.isLetterOrDigit() && ch.code < 128) || ch == '\'' || ch == '’' || ch == '-' || ch == '&'

    val out = mutableListOf<QuoteToken>()
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        when {
            ch == ' ' -> {
                if (out.isNotEmpty()) {
                    val last = out.removeAt(out.lastIndex)
                    out.add(QuoteToken(last.text + " ", last.start, i + 1))
                }
                i++
            }
            isLatinWordChar(ch) -> {
                var j = i
                while (j < text.length && isLatinWordChar(text[j])) j++
                out.add(QuoteToken(text.substring(i, j), i, j))
                i = j
            }
            else -> {
                if (!ch.isLetterOrDigit() && out.isNotEmpty()) {
                    val last = out.removeAt(out.lastIndex)
                    out.add(QuoteToken(last.text + ch, last.start, i + 1))
                } else {
                    out.add(QuoteToken(ch.toString(), i, i + 1))
                }
                i++
            }
        }
    }
    return out
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusQuote(
    quote: DailyQuote,
    animationsEnabled: Boolean,
    onOpenArticle: (String) -> Unit,
) {
    val c = IntentTheme.colors
    val original = quote.text
    val spaced = original.withCjkSpacing()
    val highlights = remember(original, quote.highlights) {
        decodeAndRemap(original, spaced, quote.highlights)
    }
    val tokens = remember(spaced, highlights) {
        tokenizeQuote(spaced).map { tk ->
            tk to highlights.any { it.start < tk.end && tk.start < it.end }
        }
    }
    val staggerMs = if (tokens.isEmpty()) 0
    else minOf(TOKEN_STAGGER_MAX_MS, TOKEN_STAGGER_BUDGET_MS / tokens.size)
    val focusDoneAtMs = (tokens.size - 1).coerceAtLeast(0) * staggerMs + TOKEN_DURATION_MS

    var focusDone by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(Unit) {
        if (focusDone) return@LaunchedEffect
        delay(focusDoneAtMs + UNDERLINE_EXTRA_DELAY_MS)
        focusDone = true
    }
    val underlineAlpha by animateFloatAsState(
        targetValue = if (focusDone) 1f else 0f,
        animationSpec = tween(250),
        label = "quoteUnderline",
    )

    FlowRow(modifier = Modifier.fillMaxWidth()) {
        tokens.forEachIndexed { i, (tk, highlighted) ->
            FocusToken(
                token = tk,
                delayMs = i * staggerMs,
                animationsEnabled = animationsEnabled,
                highlighted = highlighted,
                underlineAlpha = underlineAlpha,
            )
        }
        if (quote.articleId != null) {
            FocusCapsuleArrow(
                animationsEnabled = animationsEnabled,
                focusDone = focusDone,
                onClick = { onOpenArticle(quote.articleId) },
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }

    // Meta line: source | summary — hidden for fallback quotes (no articleId).
    val sourceTitle = quote.sourceTitle
    if (sourceTitle != null && quote.articleId != null) {
        Spacer(Modifier.height(6.dp))
        MetaFocusLine(
            text = "$sourceTitle | ${quote.articleSummary ?: ""}"
                .trimEnd(' ', '|').withCjkSpacing(),
            delayMs = META_DELAY_MS,
            animationsEnabled = animationsEnabled,
        )
    }
}

/**
 * Overlapping token focus: transparent unbounded blur 12dp → 0, alpha 0 → 1.
 * Rectangle/Clamp blur extends glyph-edge pixels across each token's box, making
 * CJK characters look like mosaic tiles. Decal sampling lets them dissolve softly.
 * On API 30 (no RenderEffect) falls back to alpha + a slight rise.
 */
@Composable
private fun FocusToken(
    token: QuoteToken,
    delayMs: Int,
    animationsEnabled: Boolean,
    highlighted: Boolean,
    underlineAlpha: Float,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val progress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value >= 1f) return@LaunchedEffect
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(TOKEN_DURATION_MS, easing = FastOutSlowInEasing))
    }
    val p = progress.value
    val canBlur = Build.VERSION.SDK_INT >= 31
    var baseline by remember { mutableStateOf(0f) }

    Text(
        text = token.text,
        style = t.quoteSerif,
        color = c.text,
        onTextLayout = { baseline = it.getLineBaseline(0).toFloat() },
        modifier = Modifier
            .then(
                if (canBlur && animationsEnabled) {
                    Modifier.blur(
                        radius = (12f * (1f - p)).dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    )
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                alpha = when {
                    !animationsEnabled -> 1f
                    else -> p
                }
                if (animationsEnabled && !canBlur) {
                    translationY = (1f - p) * 8.dp.toPx()
                }
            }
            .drawBehind {
                if (highlighted && underlineAlpha > 0f && baseline > 0f) {
                    val y = baseline + 2.dp.toPx()
                    val thickness = 2.dp.toPx()
                    val steps = 16
                    val path = androidx.compose.ui.graphics.Path()
                    // Small, smooth changes in pressure give the line a pencil feel.
                    fun edge(x: Float, side: Float): Float {
                        val wave = kotlin.math.sin(x / 5.dp.toPx())
                        val halfWidth = thickness * (0.5f + 0.15f * wave)
                        return y + side * halfWidth
                    }
                    path.moveTo(0f, edge(0f, -1f))
                    for (i in 1..steps) {
                        val x = size.width * i / steps
                        path.lineTo(x, edge(x, -1f))
                    }
                    for (i in steps downTo 0) {
                        val x = size.width * i / steps
                        path.lineTo(x, edge(x, 1f))
                    }
                    path.close()
                    drawPath(path, color = c.accent, alpha = underlineAlpha)
                }
            },
    )
}

/**
 * Capsule arrow: springs in with the first word (scale 0.8 → 1, alpha 0 → 1),
 * starts as a translucent grey disc and settles to the normal capsule once the
 * quote is fully in focus.
 */
@Composable
private fun FocusCapsuleArrow(
    animationsEnabled: Boolean,
    focusDone: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = IntentTheme.colors
    val scale = remember { Animatable(if (animationsEnabled) 0.8f else 1f) }
    val alpha = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!animationsEnabled) return@LaunchedEffect
        launch {
            scale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        launch { alpha.animateTo(1f, tween(300)) }
    }
    val normalBg = if (c.isDark) Color(0xFF1F1F1F) else Color.White
    val bg by animateColorAsState(
        targetValue = if (focusDone) normalBg else c.textSecondary.copy(alpha = 0.30f),
        animationSpec = tween(300),
        label = "quoteArrowBg",
    )
    val tint by animateColorAsState(
        targetValue = if (focusDone) {
            if (c.isDark) Color.White else Color.Black
        } else {
            c.textSecondary
        },
        animationSpec = tween(300),
        label = "quoteArrowTint",
    )

    Box(
        modifier = modifier
            .padding(start = 4.dp)
            .size(28.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
            .clip(CircleShape)
            .background(bg)
            .border(
                1.dp,
                (if (c.isDark) Color.White else Color.Black).copy(alpha = 0.18f),
                CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowRightAlt,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Meta line: transparent, unbounded focus (blur 8dp → 0, alpha 0 → 1). */
@Composable
private fun MetaFocusLine(
    text: String,
    delayMs: Int,
    animationsEnabled: Boolean,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val progress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value >= 1f) return@LaunchedEffect
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(META_DURATION_MS, easing = FastOutSlowInEasing))
    }
    val p = progress.value
    val canBlur = Build.VERSION.SDK_INT >= 31

    Text(
        text = text,
        style = t.sourceSerif,
        color = c.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .then(
                if (canBlur && animationsEnabled) {
                    Modifier.blur(
                        radius = (8f * (1f - p)).dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    )
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                alpha = when {
                    !animationsEnabled -> 1f
                    else -> p
                }
                if (animationsEnabled && !canBlur) {
                    translationY = (1f - p) * 6.dp.toPx()
                }
            },
    )
}

/** Decodes JSON highlights; re-locates keywords inside the spaced text when offsets shift. */
private fun decodeAndRemap(
    original: String,
    spaced: String,
    raw: String?,
): List<HighlightRange> {
    val ranges: List<HighlightRange> = try {
        if (raw.isNullOrBlank()) emptyList()
        else kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString<List<HighlightRange>>(raw)
    } catch (_: Exception) {
        emptyList()
    }
    if (original == spaced || ranges.isEmpty()) return ranges
    return ranges.mapNotNull { r ->
        if (r.start !in original.indices || r.end > original.length || r.end <= r.start) return@mapNotNull null
        val kw = original.substring(r.start, r.end)
        val idx = spaced.indexOf(kw)
        if (idx >= 0) HighlightRange(idx, idx + kw.length) else null
    }
}
