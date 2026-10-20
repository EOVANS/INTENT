package com.intent.reader.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.intent.reader.data.db.Source
import com.intent.reader.ui.components.SourceLogo
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Subscription management: logo, name, unread count, last update;
 * click -> source settings sheet, long-press -> actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    sources: List<Source>,
    unreadBySource: Map<String, Int>,
    onBack: () -> Unit,
    onOpenSource: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onSetNotify: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    var menuFor by remember { mutableStateOf<Source?>(null) }
    var renameFor by remember { mutableStateOf<Source?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopAppBar(
            title = { Text(stringResource(R.string.my_sources), style = t.pageTitle) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
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
                    onClick = { onOpenSource(s.id) },
                    onLongClick = { menuFor = s },
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
            }
        }
    }

    menuFor?.let { src ->
        DropdownMenu(expanded = true, onDismissRequest = { menuFor = null }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename)) },
                onClick = {
                    renameFor = src
                    renameText = src.title
                    menuFor = null
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.notify_switch) + ": " + if (src.notifyEnabled) "on" else "off") },
                onClick = {
                    onSetNotify(src.id, !src.notifyEnabled)
                    menuFor = null
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.cancel_sub)) },
                onClick = {
                    onRemove(src.id)
                    menuFor = null
                },
            )
        }
    }

    renameFor?.let { src ->
        RenameDialog(
            initial = renameText,
            onConfirm = {
                onRename(src.id, it)
                renameFor = null
            },
            onDismiss = { renameFor = null },
        )
    }
}

@Composable
private fun RenameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename)) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun SourceRow(
    source: Source,
    unread: Int,
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
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
        }
        Text("→", style = t.body, color = c.textSecondary)
    }
}
