package com.intent.reader.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intent.reader.R
import com.intent.reader.data.assets.RecommendedSources
import com.intent.reader.domain.CardStyle
import com.intent.reader.ui.components.ArticleCard
import com.intent.reader.ui.components.SourceLogo
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentTheme
import kotlinx.coroutines.launch

/** Search page: URL adding, recommended hero strip, local article search (doc 4.2). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onAddSource: (String, (Boolean) -> Unit) -> Unit,
    onAddRecommended: (String, (Boolean) -> Unit) -> Unit,
    onOpenAllArticles: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenSource: (String) -> Unit,
    onOpenArticle: (String) -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val successMsg = stringResource(R.string.add_source_success)
    val failedMsg = stringResource(R.string.add_source_failed)
    var input by remember { mutableStateOf("") }
    // Edge-to-edge: the list has to clear the status bar itself.
    val statusBarTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }

    fun submit() {
        val v = input.trim()
        when {
            v.startsWith("http://") || v.startsWith("https://") ||
                (v.contains(".") && !v.contains(" ")) -> {
                onAddSource(v) { ok ->
                    scope.launch {
                        if (ok) {
                            input = ""
                            snackbar.showSnackbar(successMsg)
                        } else {
                            snackbar.showSnackbar(failedMsg)
                        }
                    }
                }
            }
            v.isNotBlank() -> onQueryChange(v)
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = IntentDimens.pageHMarginPhone.dp,
                end = IntentDimens.pageHMarginPhone.dp,
                top = statusBarTop + 24.dp,
                bottom = 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(stringResource(R.string.tab_search), style = t.pageTitle)
            }
            item {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        if (it.isBlank()) onQueryChange("")
                    },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(IntentDimens.radiusInput.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Uri,
                    ),
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.text,
                        unfocusedBorderColor = c.divider,
                        cursorColor = c.text,
                        focusedTextColor = c.text,
                        unfocusedTextColor = c.text,
                    ),
                )
            }

            // Search results (when querying)
            if (state.query.isNotBlank()) {
                if (state.results.isEmpty()) {
                    item {
                        Text(stringResource(R.string.no_results), style = t.bodySmall, color = c.textSecondary)
                    }
                } else {
                    state.results.forEach { a ->
                        item(key = "res_" + a.id) {
                            ArticleCard(
                                style = if (a.imageUrl.isNullOrBlank()) CardStyle.D else CardStyle.C,
                                article = a,
                                sourceTitle = "",
                                sourceLogo = null,
                                onClick = { onOpenArticle(a.id) },
                            )
                        }
                    }
                }
            } else {
                // Recommended hero strip (doc 4.2.2)
                item {
                    Text(stringResource(R.string.recommended_sources), style = t.sectionTitle)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.heroBg)
                            .padding(vertical = 14.dp, horizontal = 10.dp)
                    ) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            RecommendedSources.all.forEach { rs ->
                                HeroSource(
                                    name = rs.name,
                                    logoUrl = rs.logoUrl,
                                    subscribed = state.sources.any { it.feedUrl == rs.feedUrl },
                                    onClick = { onAddRecommended(rs.feedUrl) {} },
                                )
                            }
                        }
                    }
                }
                // User-imported feeds (added by URL / OPML, i.e. not built-in picks)
                val imported = state.sources.filter { !it.isBuiltIn }
                if (imported.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.imported_sources), style = t.sectionTitle)
                    }
                    imported.forEach { s ->
                        item(key = "imported_" + s.id) {
                            ImportedSourceRow(
                                title = s.title,
                                logoUrl = s.logoUrl,
                                onClick = { onOpenSource(s.id) },
                            )
                        }
                    }
                }
                // Entries (doc 4.2.3)
                item { EntryRow(stringResource(R.string.all_articles)) { onOpenAllArticles() } }
                item { EntryRow(stringResource(R.string.my_sources)) { onOpenSources() } }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun HeroSource(name: String, logoUrl: String, subscribed: Boolean, onClick: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick),
    ) {
        SourceLogo(logoUrl, name, 44.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = name,
            style = t.micro,
            color = c.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(if (subscribed) R.string.subscribed else R.string.subscribe),
            style = t.micro,
            color = c.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun ImportedSourceRow(title: String, logoUrl: String?, onClick: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceLogo(logoUrl, title, 28.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = t.body,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text("→", style = t.body, color = c.textSecondary)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
    }
}

@Composable
private fun EntryRow(label: String, onClick: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = t.body, color = c.text)
            Text("→", style = t.body, color = c.textSecondary)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
    }
}
