package com.intent.reader.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intent.reader.R
import com.intent.reader.data.db.Article
import com.intent.reader.data.db.Source
import com.intent.reader.ui.components.ArticleImage
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.withCjkSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Article list row used by All-articles / Source detail / aggregate views. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArticleListRow(
    article: Article,
    sourceTitle: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val time = remember(article.publishedAt) {
        Instant.ofEpochMilli(article.publishedAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("M/d HH:mm"))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (sourceTitle != null) {
                Text(
                    text = sourceTitle,
                    style = t.micro,
                    color = c.textSecondary,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = article.title.withCjkSpacing(),
                style = t.cardTitle.copy(
                    fontWeight = if (!article.isRead) androidx.compose.ui.text.font.FontWeight.SemiBold
                    else androidx.compose.ui.text.font.FontWeight.Normal,
                    color = if (!article.isRead) c.text else c.textSecondary,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = time,
                style = t.micro,
                color = c.textSecondary,
            )
        }
        ArticleImage(article.imageUrl, Modifier.size(72.dp), corner = 12)
    }
}

/** All articles page with filters: source / read state / keyword (doc 4.2.3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllArticlesScreen(
    articles: List<Article>,
    sources: Map<String, Source>,
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onSetRead: (String, Boolean) -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    var filterUnreadOnly by remember { mutableStateOf(false) }
    var filterSource by remember { mutableStateOf<String?>(null) }

    val filtered = remember(articles, filterUnreadOnly, filterSource) {
        articles.filter { a ->
            (!filterUnreadOnly || !a.isRead) &&
                (filterSource == null || a.sourceId == filterSource)
        }
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopAppBar(
            title = { Text(stringResource(R.string.all_articles), style = t.pageTitle) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = c.bg, titleContentColor = c.text,
            ),
        )
        // filter chips
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = IntentDimens.pageHMarginPhone.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !filterUnreadOnly && filterSource == null,
                onClick = { filterUnreadOnly = false; filterSource = null },
                label = { Text(stringResource(R.string.filter_all)) },
                colors = chipColors(),
            )
            FilterChip(
                selected = filterUnreadOnly,
                onClick = { filterUnreadOnly = !filterUnreadOnly },
                label = { Text(stringResource(R.string.filter_unread)) },
                colors = chipColors(),
            )
            sources.values.sortedBy { it.title }.forEach { s ->
                FilterChip(
                    selected = filterSource == s.id,
                    onClick = { filterSource = if (filterSource == s.id) null else s.id },
                    label = { Text(s.title, maxLines = 1) },
                    colors = chipColors(),
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = IntentDimens.pageHMarginPhone.dp,
                vertical = 8.dp,
            ),
        ) {
            items(filtered, key = { it.id }) { a ->
                ArticleListRow(
                    article = a,
                    sourceTitle = sources[a.sourceId]?.title,
                    onClick = { onOpenArticle(a.id) },
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
            }
        }
    }
}

/** Source detail: this source's article list + header. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceDetailScreen(
    source: Source?,
    articles: List<Article>,
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopAppBar(
            title = { Text(source?.title ?: "", style = t.pageTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = c.bg, titleContentColor = c.text,
            ),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = IntentDimens.pageHMarginPhone.dp,
                vertical = 8.dp,
            ),
        ) {
            items(articles, key = { it.id }) { a ->
                ArticleListRow(article = a, sourceTitle = null, onClick = { onOpenArticle(a.id) })
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
            }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = IntentTheme.colors.text,
    selectedLabelColor = IntentTheme.colors.bg,
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    labelColor = IntentTheme.colors.textSecondary,
)
