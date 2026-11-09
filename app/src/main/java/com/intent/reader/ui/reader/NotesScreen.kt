package com.intent.reader.ui.reader

import android.widget.Toast
import android.content.Intent
import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intent.reader.IntentApplication
import com.intent.reader.R
import com.intent.reader.notes.IslandNote
import com.intent.reader.notes.NoteArticle
import com.intent.reader.notes.NoteArticles
import com.intent.reader.notes.NoteMarkdown
import com.intent.reader.notes.NoteExport
import com.intent.reader.rss.HtmlText
import com.intent.reader.ui.components.HeaderIconButton
import com.intent.reader.ui.components.LocalHeaderMaterial
import com.intent.reader.ui.components.TextStoryCard
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.withCjkSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import androidx.core.content.FileProvider
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    articleId: String?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    userName: String = "",
    contentPadding: PaddingValues = PaddingValues(),
    onOpenArticleNotes: (String) -> Unit = {},
    cardTransitionModifier: @Composable (String) -> Modifier = { Modifier },
    popupBackdrop: Backdrop? = null,
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as IntentApplication).container.notesRepository
    val state by repository.state.collectAsState()
    val notes = remember(state.notes, articleId) { state.notes.filter { articleId == null || it.articleId == articleId } }
    val groups = remember(notes) { NoteArticles.group(notes) }
    val article by produceState<com.intent.reader.data.db.Article?>(null, articleId) {
        value = if (articleId == null) null else withContext(Dispatchers.IO) {
            (context.applicationContext as IntentApplication).container.database.articleDao().getById(articleId)
        }
    }
    val legacyCovers by produceState<Map<String, String>>(emptyMap(), groups) {
        value = withContext(Dispatchers.IO) {
            val dao = (context.applicationContext as IntentApplication).container.database.articleDao()
            groups.take(4).filter { it.imageUrl.isNullOrBlank() }.mapNotNull { group ->
                dao.getById(group.articleId)?.imageUrl?.let { group.articleId to it }
            }.toMap()
        }
    }
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val scope = rememberCoroutineScope()
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val visible = remember(groups, query) {
        val term = query.trim()
        groups.filter { group -> term.isBlank() || group.paragraphs.any { note ->
            listOf(note.articleTitle, note.sourceTitle, note.quote, note.remark).any { it.contains(term, ignoreCase = true) }
        } }
    }
    val day = LocalDate.now()
    val weekStart = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val weekCount = groups.count { group -> group.paragraphs.any { it.createdAt >= weekStart } }
    val statusTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    val navigationBottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val margin = IntentDimens.pageHMarginPhone.dp
    LaunchedEffect(repository) {
        try { repository.load() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { loadFailed = true }
    }
    val shareNotes = {
            scope.launch {
                exporting = true
                try {
                    repository.load()
                    val exportItems = repository.exportSnapshot(articleId)
                    val snapshot = exportItems.map { it.note }
                    if (snapshot.isEmpty()) throw java.io.IOException("No notes to share")
                    val hasInk = exportItems.any { it.handwriting != null }
                    val file = withContext(Dispatchers.IO) {
                        val directory = java.io.File(context.cacheDir, "note-shares")
                        if (!directory.exists() && !directory.mkdirs()) throw java.io.IOException("Cannot create share directory")
                        val file = java.io.File.createTempFile("INTENT-notes-${LocalDate.now()}-", if (hasInk) ".zip" else ".md", directory)
                        if (hasInk) NoteExport.writeZip(file, exportItems) else file.bufferedWriter(Charsets.UTF_8).use { writer ->
                            NoteArticles.group(snapshot).forEachIndexed { index, group ->
                                if (index > 0) writer.write("\n\n---\n\n")
                                val source = (context.applicationContext as IntentApplication).container.database.articleDao().getById(group.articleId)
                                writer.write(NoteMarkdown.encodeArticle(NoteArticles.order(group.paragraphs, HtmlText.toBlocks(source?.contentHtml ?: source?.summary))))
                            }
                        }
                        file
                    }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.notes-share", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = if (hasInk) "application/zip" else "text/markdown"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri("Markdown", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.notes_export)))
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { Toast.makeText(context, R.string.notes_export_error, Toast.LENGTH_LONG).show() }
                finally { exporting = false }
            }
    }
    val material = LocalHeaderMaterial.current
    val glassMenu = Build.VERSION.SDK_INT >= 33 && material.liquidGlass && popupBackdrop != null
    val menuShape = RoundedCornerShape(20.dp)
    val menuModifier = if (glassMenu) Modifier.drawBackdrop(
        backdrop = popupBackdrop!!,
        shape = { menuShape },
        effects = {
            blur((10.dp - 4.dp * material.transparency.coerceIn(0f, 1f)).toPx())
            lens(12.dp.toPx(), 14.dp.toPx(), chromaticAberration = false)
        },
        highlight = { Highlight(width = 0.6.dp, alpha = 0.55f) },
        shadow = null,
        onDrawSurface = {
            // Text-heavy menus use a more protective regular glass surface.
            val opacity = 0.48f - 0.20f * material.transparency.coerceIn(0f, 1f)
            drawRect((if (c.isDark) Color.Black else Color.White).copy(alpha = opacity))
        },
    ).border(0.6.dp, (if (c.isDark) Color.White else Color.Black)
        .copy(alpha = if (c.isDark) 0.16f else 0.10f), menuShape) else Modifier
    val actions: @Composable RowScope.() -> Unit = {
        if (articleId == null) HeaderIconButton(
            if (searching) Icons.Outlined.Close else Icons.Outlined.Search,
            stringResource(if (searching) R.string.close else R.string.notes_search),
            { searching = !searching; if (!searching) query = "" },
        )
        Box {
            HeaderIconButton(Icons.Outlined.MoreVert, stringResource(R.string.notes_more), { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false },
                modifier = menuModifier,
                containerColor = if (glassMenu) Color.Transparent else c.surface,
                shape = menuShape,
                shadowElevation = if (glassMenu) 0.dp else 3.dp,
                tonalElevation = 0.dp,
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.notes_export)) },
                    enabled = notes.isNotEmpty() && !exporting,
                    onClick = { menuOpen = false; shareNotes() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.notes_report_placeholder)) },
                    enabled = false,
                    onClick = {},
                )
            }
        }
    }
    Column(modifier.fillMaxSize().background(c.bg).imePadding()) {
        if (onBack != null) TopAppBar(
            title = { Text(stringResource(R.string.notes_this_article), style = t.meta) },
            navigationIcon = { HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.notes_back), onBack) },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg, titleContentColor = c.text),
        )
        if (articleId != null) {
            val ordered = remember(notes, article) {
                NoteArticles.order(notes, HtmlText.toBlocks(article?.contentHtml ?: article?.summary))
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = margin, end = margin, top = 20.dp, bottom = navigationBottom + 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item(key = "title") {
                    Text(groups.firstOrNull()?.latest?.articleTitle ?: article?.title.orEmpty(), style = t.pageTitle, color = c.text)
                }
                if (loadFailed || state.unreadableFiles > 0) item(key = "error") {
                    Text(stringResource(R.string.notes_load_error), style = t.meta, color = c.accent)
                }
                if (state.loaded && ordered.isEmpty()) item(key = "empty") {
                    Text(stringResource(R.string.notes_empty), style = t.bodySmall, color = c.textSecondary)
                }
                items(ordered, key = { it.id }) { note ->
                    Column(Modifier.fillMaxWidth().clickable { editingId = note.id }, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(note.quote.withCjkSpacing(), style = t.body.copy(lineHeight = t.body.lineHeight * 1.4f), color = c.text)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.width(2.dp).height(20.dp).background(c.accent))
                            Text(note.remark.ifBlank { stringResource(R.string.notes_add_remark) }.withCjkSpacing(),
                                style = t.bodySmall, color = c.textSecondary)
                        }
                        if (note.handwritingRevision != null) Text(stringResource(R.string.ink_edit), style = t.meta, color = c.textSecondary)
                    }
                }
                if (ordered.isNotEmpty()) item(key = "footer") {
                    val updated = Instant.ofEpochMilli(ordered.maxOf { it.updatedAt }).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    Text(stringResource(R.string.notes_detail_meta, updated, NoteArticles.characterCount(ordered)),
                        style = t.micro, color = c.textSecondary, modifier = Modifier.padding(top = 12.dp))
                }
            }
        } else LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = if (onBack == null) statusTop + 12.dp else 12.dp,
                bottom = maxOf(contentPadding.calculateBottomPadding(), navigationBottom) + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (onBack == null) item(key = "welcome") {
                Column(Modifier.padding(horizontal = margin)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, content = actions)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.greeting_hi), style = t.greetingSerif.copy(fontWeight = FontWeight.Bold), color = c.text)
                    if (userName.isNotBlank()) Text(userName.withCjkSpacing(), style = t.greetingSerif.copy(fontWeight = FontWeight.Bold), color = c.text)
                    Spacer(Modifier.height(8.dp))
                    Text(pluralStringResource(R.plurals.notes_week_count, weekCount, weekCount), style = t.meta, color = c.textSecondary)
                }
            }
            if (searching) item(key = "search") {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    label = { Text(stringResource(R.string.notes_search)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = margin),
                    shape = RoundedCornerShape(IntentDimens.radiusInput.dp),
                )
            }
            if (loadFailed || state.unreadableFiles > 0) item(key = "error") {
                Text(stringResource(R.string.notes_load_error), modifier = Modifier.padding(horizontal = margin), style = t.meta, color = c.accent)
            }
            if (state.loaded && visible.isEmpty()) item(key = "empty") {
                TextStoryCard(
                    title = stringResource(if (query.isBlank()) R.string.notes_empty_title else R.string.no_results),
                    preview = stringResource(if (query.isBlank()) R.string.notes_empty else R.string.notes_search_hint),
                    sourceTitle = stringResource(R.string.island_notes), sourceLogo = null,
                    onClick = { if (query.isNotBlank()) query = "" },
                    modifier = Modifier.padding(horizontal = margin).fillMaxWidth(),
                )
            }
            if (visible.isNotEmpty() && onBack == null && query.isBlank()) {
                item(key = "hero") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.notes_recent), modifier = Modifier.padding(horizontal = margin), style = t.sectionTitle, color = c.textSecondary)
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val cardWidth = (maxWidth * 0.46f).coerceIn(160.dp, 260.dp)
                            val recent = visible.take(4)
                            val hasCover = recent.any { !((it.imageUrl ?: legacyCovers[it.articleId]).isNullOrBlank()) }
                            val textHeight = 184.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                            val cardHeight = textHeight + if (hasCover) 120.dp else 0.dp
                            LazyRow(contentPadding = PaddingValues(horizontal = margin), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(recent, key = { it.articleId }) { group ->
                                    NotePreviewCard(group, Modifier.width(cardWidth).height(cardHeight).then(cardTransitionModifier(group.articleId)), legacyCovers[group.articleId]) { onOpenArticleNotes(group.articleId) }
                                }
                            }
                        }
                    }
                }
                if (visible.size > 4) item(key = "older") {
                    Text(stringResource(R.string.notes_older), modifier = Modifier.padding(horizontal = margin), style = t.sectionTitle, color = c.textSecondary)
                }
            }
            val remaining = if (onBack == null && query.isBlank()) visible.drop(4) else visible
            items(remaining, key = { it.articleId }) { group ->
                NotePreviewCard(group, Modifier.padding(horizontal = margin).then(cardTransitionModifier(group.articleId)).fillMaxWidth(), legacyCovers[group.articleId]) { onOpenArticleNotes(group.articleId) }
            }
            item(key = "local") {
                Text(stringResource(R.string.notes_local_hint), modifier = Modifier.padding(horizontal = margin), style = t.micro, color = c.textSecondary)
            }
        }
    }
    state.notes.firstOrNull { it.id == editingId }?.let { note ->
        key(note.id) {
            NoteEditor(note.quote, note.remark, note = note, draftKey = "${note.articleId}:${note.paragraphKey}", repository = repository,
                onDismiss = { editingId = null },
                onSave = { remark, ink -> repository.updateWithHandwriting(note.id, remark, ink) }, onDelete = { repository.delete(note.id) })
        }
    }
}

@Composable
private fun NotePreviewCard(group: NoteArticle, modifier: Modifier = Modifier, legacyCover: String? = null, onClick: () -> Unit) {
    val preview = group.preview.replace(Regex("\\s+"), " ").take(160)
    val latest = group.latest
    TextStoryCard(latest.articleTitle, preview, latest.sourceTitle, null, onClick, modifier, imageUrl = group.imageUrl ?: legacyCover)
}
