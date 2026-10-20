package com.intent.reader.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Grid columns per window size class (doc 8). */
fun articleColumns(widthClass: WindowWidthSizeClass): Int = when (widthClass) {
    WindowWidthSizeClass.Medium -> 3
    WindowWidthSizeClass.Expanded -> 4
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
 * - Tablets: A/B take one column, everything else spans two.
 */
fun computeSpans(rows: List<HomeRowUi>, widthClass: WindowWidthSizeClass): List<Int> {
    val total = articleColumns(widthClass)
    val compact = widthClass == WindowWidthSizeClass.Compact
    val spans = rows.map { row ->
        when (row) {
            is HomeRowUi.Aggregate -> if (compact) total else 2.coerceAtMost(total)
            is HomeRowUi.Single ->
                if (isHalfWidthStyle(row.a.style)) 1
                else if (compact) total else 2.coerceAtMost(total)
        }
    }.toMutableList()

    // Odd run of half-width cards would leave the second column empty.
    if (total == 2) {
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
    onSetRead: (String, Boolean) -> Unit,
    onOpenSource: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val c = IntentTheme.colors
    val isTablet = widthClass != WindowWidthSizeClass.Compact
    val hMargin = if (isTablet) IntentDimens.pageHMarginTablet.dp else IntentDimens.pageHMarginPhone.dp
    // The feed is edge-to-edge, so the grid has to clear the status bar itself.
    val statusBarTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    val spans = remember(state.rows, widthClass) { computeSpans(state.rows, widthClass) }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize()) {
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
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(articleColumns(widthClass)),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = hMargin,
                    end = hMargin,
                    top = contentPadding.calculateTopPadding() + statusBarTop + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 96.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(IntentDimens.cardSpacing.dp),
                verticalArrangement = Arrangement.spacedBy(IntentDimens.cardSpacing.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                    GreetingHeader(
                        userName = state.userName,
                        avatarPath = state.avatarPath,
                        quote = state.quote,
                        onOpenArticle = onOpenArticle,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
                state.rows.forEachIndexed { index, row ->
                    when (row) {
                        is HomeRowUi.Single -> {
                            item(key = row.a.article.id, span = { GridItemSpan(spans[index]) }) {
                                LongPressCard(
                                    article = row.a.article,
                                    onSetRead = onSetRead,
                                ) {
                                    ArticleCard(
                                        style = row.a.style,
                                        article = row.a.article,
                                        sourceTitle = row.a.sourceTitle,
                                        sourceLogo = row.a.sourceLogo,
                                        onClick = { onOpenArticle(row.a.article.id) },
                                    )
                                }
                            }
                        }
                        is HomeRowUi.Aggregate -> {
                            item(
                                key = "agg_" + (row.items.firstOrNull()?.article?.id ?: row.hashCode()),
                                span = { GridItemSpan(spans[index]) },
                            ) {
                                CardE(
                                    sourceTitle = row.sourceTitle,
                                    sourceLogo = row.sourceLogo,
                                    items = row.items.map { it.article },
                                    onViewAll = {
                                        onOpenSource(row.items.firstOrNull()?.article?.sourceId ?: "")
                                    },
                                    onArticle = { onOpenArticle(it.id) },
                                )
                            }
                        }
                    }
                }
            }
            }

            // Top status-bar fade: a vertical gradient scrim from solid bg ->
            // transparent gives the illusion of a blur transition as content
            // scrolls under the system status bar.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to c.bg,
                                0.6f to c.bg.copy(alpha = 0.85f),
                                1f to c.bg.copy(alpha = 0f),
                            ),
                        )
                    ),
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
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    Box {
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
private fun UserAvatar(path: String, name: String) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Box(
        modifier = Modifier
            .size(52.dp)
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
                style = t.greetingSerif.copy(fontWeight = FontWeight.Bold),
                color = c.textSecondary,
            )
        }
    }
}

/** Serif greeting header with red-underlined daily quote (doc 4.1.1). */
@Composable
fun GreetingHeader(
    userName: String,
    avatarPath: String = "",
    quote: DailyQuote?,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val hi = stringResource(R.string.greeting_hi)
    val todayPrefix = stringResource(R.string.today_prefix)

    val dateText = remember {
        val d = LocalDate.now()
        if (Locale.getDefault().language == "zh") {
            "${d.monthValue}月/${d.dayOfMonth}日"
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
                // User name on the next line, same bold serif
                if (userName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = userName.withCjkSpacing(),
                        style = t.greetingSerif.copy(fontWeight = FontWeight.Bold),
                        color = c.text,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            UserAvatar(path = avatarPath, name = userName)
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

        QuoteBlock(quote = quote, onOpenArticle = onOpenArticle)

        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .width(40.dp)
                .height(1.dp)
                .background(c.divider)
        )
        Spacer(Modifier.height(20.dp))
    }
}

/** Quote text with red underline highlights + inline capsule arrow button. */
@Composable
fun QuoteBlock(
    quote: DailyQuote?,
    onOpenArticle: (String) -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val density = LocalDensity.current
    val underlineWidth = with(density) { 1.5.dp.toPx() }
    val underlineGap = with(density) { 2.dp.toPx() }

    val original = quote?.text ?: ""
    if (original.isEmpty()) return
    val spaced = original.withCjkSpacing()
    val highlights = remember(original, quote?.highlights) {
        decodeAndRemap(original, spaced, quote?.highlights)
    }

    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotated: AnnotatedString = buildAnnotatedString {
        append(spaced)
        append(" ")
        appendInlineContent("arrow", "[→]")
    }

    val inlineContent = mapOf(
        "arrow" to InlineTextContent(
            // Match the visual capsule (28dp) so the placeholder size, alignment,
            // and rendered content all agree — no more vertical offset.
            Placeholder(1.4.em, 1.4.em, PlaceholderVerticalAlign.Center)
        ) {
            CapsuleArrow(
                enabled = quote?.articleId != null,
                onClick = { quote?.articleId?.let(onOpenArticle) },
            )
        },
    )

    Text(
        text = annotated,
        style = t.quoteSerif,
        color = c.text,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        inlineContent = inlineContent,
        onTextLayout = { layout = it },
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val l = layout ?: return@drawBehind
                highlights.forEach { r ->
                    if (r.start >= spaced.length || r.end > spaced.length) return@forEach
                    val startLine = l.getLineForOffset(r.start)
                    val endLine = l.getLineForOffset((r.end - 1).coerceAtLeast(r.start))
                    for (line in startLine..endLine) {
                        val lineStart = l.getLineStart(line)
                        val lineEnd = l.getLineEnd(line)
                        val from = maxOf(r.start, lineStart)
                        val to = minOf(r.end, lineEnd)
                        if (to <= from) continue
                        val x1 = l.getHorizontalPosition(from, true)
                        val x2 = l.getHorizontalPosition(to, true)
                        val y = l.getLineBaseline(line) + underlineGap
                        drawLine(
                            color = c.accent,
                            start = Offset(x1, y),
                            end = Offset(x2, y),
                            strokeWidth = underlineWidth,
                        )
                    }
                }
            },
    )

    Spacer(Modifier.height(6.dp))

    // Meta line: source | summary — hidden for fallback quotes (no articleId)
    val sourceTitle = quote?.sourceTitle
    if (sourceTitle != null && quote?.articleId != null) {
        val summary = quote.articleSummary ?: ""
        Text(
            text = "$sourceTitle | $summary".trimEnd(' ', '|').withCjkSpacing(),
            style = t.sourceSerif,
            color = c.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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

/** Capsule arrow button at the end of the quote (doc 4.1.1). */
@Composable
private fun CapsuleArrow(enabled: Boolean, onClick: () -> Unit) {
    val c = IntentTheme.colors
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (c.isDark) Color(0xFF1F1F1F) else Color.White)
            .border(
                1.dp,
                (if (c.isDark) Color.White else Color.Black).copy(alpha = 0.18f),
                CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowRightAlt,
            contentDescription = null,
            tint = if (c.isDark) Color.White else Color.Black,
            modifier = Modifier.size(18.dp),
        )
    }
}
