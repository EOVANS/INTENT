package com.intent.reader.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intent.reader.R
import com.intent.reader.data.db.Source
import com.intent.reader.ui.components.SourceLogo
import com.intent.reader.ui.components.HeaderIconButton
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Subscription management: logo, name, unread count, last update.
 * Click opens the source; long-press enters batch selection with circular
 * delete / cancel actions in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    sources: List<Source>,
    unreadBySource: Map<String, Int>,
    onBack: () -> Unit,
    onOpenSource: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    var selected by remember { mutableStateOf<Set<String>?>(null) }
    val sel = selected

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopAppBar(
            title = {
                Text(
                    if (sel == null) stringResource(R.string.my_sources)
                    else stringResource(R.string.selected_count_fmt).format(sel.size),
                    style = t.pageTitle,
                )
            },
            navigationIcon = {
                HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.notes_back),
                    onClick = { if (sel == null) onBack() else selected = null })
            },
            actions = {
                if (sel != null) {
                    HeaderIconButton(
                        icon = Icons.Outlined.DeleteOutline,
                        label = stringResource(R.string.delete),
                        enabled = sel.isNotEmpty(),
                        onClick = { sel.forEach(onRemove); selected = null },
                    )
                    HeaderIconButton(Icons.Outlined.Close, stringResource(android.R.string.cancel),
                        onClick = { selected = null })
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = c.bg,
                titleContentColor = c.text,
            ),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = IntentDimens.pageHMarginPhone.dp,
                vertical = 8.dp,
            ),
        ) {
            items(sources, key = { it.id }) { s ->
                SourceRow(
                    source = s,
                    unread = unreadBySource[s.id] ?: 0,
                    selectionMode = sel != null,
                    selected = sel?.contains(s.id) == true,
                    onClick = {
                        when {
                            sel == null -> onOpenSource(s.id)
                            sel.contains(s.id) -> selected = sel - s.id
                            else -> selected = sel + s.id
                        }
                    },
                    onLongClick = {
                        if (sel == null) selected = setOf(s.id)
                    },
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SourceRow(
    source: Source,
    unread: Int,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val lastSync = remember(source.lastFetchedAt) {
        if (source.lastFetchedAt <= 0) "" else
            Instant.ofEpochMilli(source.lastFetchedAt).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectionMode) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (selected) c.accent else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (selected) c.accent else c.textSecondary,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Text("✓", color = Color.White, fontSize = 12.sp)
            }
        }
        SourceLogo(source.logoUrl, source.title, 32.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = source.title,
                style = t.body,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(stringResource(R.string.unread_count_fmt).format(unread))
                    if (lastSync.isNotEmpty()) append(" · $lastSync")
                },
                style = t.meta,
                color = c.textSecondary,
            )
            source.lastError?.let { err ->
                Text(
                    text = stringResource(R.string.sync_failed_fmt).format(err),
                    style = t.micro,
                    color = c.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!selectionMode) Text("→", style = t.body, color = c.textSecondary)
    }
}
