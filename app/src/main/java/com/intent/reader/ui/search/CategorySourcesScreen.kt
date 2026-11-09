package com.intent.reader.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intent.reader.R
import com.intent.reader.ui.theme.withCjkSpacing
import com.intent.reader.data.assets.RecommendedSources
import com.intent.reader.data.db.Source
import com.intent.reader.data.repo.SourceRepository.AddSourceError
import com.intent.reader.ui.components.SourceLogo
import com.intent.reader.ui.components.HeaderIconButton
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentTheme
import kotlinx.coroutines.launch

/**
 * Sub-page of the search hero: the preset feeds for one category, each with a
 * capsule subscribe toggle. Tapping a subscribed row opens the feed.
 */
@Composable
fun CategorySourcesScreen(
    categoryKey: String,
    sources: List<Source>,
    onBack: () -> Unit,
    onToggle: (String, (AddSourceError?) -> Unit) -> Unit,
    onOpenSource: (String) -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val invalidMsg = stringResource(R.string.add_source_invalid)
    val notFoundMsg = stringResource(R.string.add_source_not_found)
    val badFormatMsg = stringResource(R.string.add_source_bad_format)
    val presets = remember(categoryKey) { RecommendedSources.byCategoryOrAll(categoryKey) }
    val label = categoryLabel(categoryKey)
    val byFeedUrl = remember(sources) { sources.associateBy { it.feedUrl } }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    start = IntentDimens.pageHMarginPhone.dp,
                    end = IntentDimens.pageHMarginPhone.dp,
                    top = 24.dp,
                    bottom = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.notes_back), onBack)
            Column {
                Text(label, style = t.pageTitle, color = c.text)
                Text(
                    stringResource(R.string.category_feeds_count, presets.size),
                    style = t.micro,
                    color = c.textSecondary,
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = IntentDimens.pageHMarginPhone.dp,
                    end = IntentDimens.pageHMarginPhone.dp,
                    top = 8.dp,
                    bottom = 32.dp,
                ),
            ) {
                presets.forEach { rs ->
                    item(key = rs.feedUrl) {
                        val existing = byFeedUrl[rs.feedUrl]
                        PresetSourceRow(
                            name = rs.name,
                            logoUrl = rs.logoUrl,
                            subscribed = existing != null,
                            onClick = { existing?.let { onOpenSource(it.id) } },
                            onToggle = {
                                onToggle(rs.feedUrl) { err ->
                                    if (err != null) scope.launch {
                                        snackbar.currentSnackbarData?.dismiss()
                                        snackbar.showSnackbar(
                                            when (err) {
                                                AddSourceError.INVALID -> invalidMsg
                                                AddSourceError.NOT_FOUND -> notFoundMsg
                                                AddSourceError.BAD_FORMAT -> badFormatMsg
                                            }
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun PresetSourceRow(
    name: String,
    logoUrl: String,
    subscribed: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = subscribed, onClick = onClick)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceLogo(logoUrl, name, 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = name.withCjkSpacing(),
                style = t.body,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            // Capsule toggle: filled accent when subscribed, hairline outline when not.
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .then(
                        if (subscribed) Modifier.background(c.accent.copy(alpha = 0.14f))
                        else Modifier.border(1.dp, c.divider, RoundedCornerShape(50))
                    )
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(if (subscribed) R.string.subscribed else R.string.subscribe),
                    style = t.micro,
                    color = if (subscribed) c.accent else c.textSecondary,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
    }
}
