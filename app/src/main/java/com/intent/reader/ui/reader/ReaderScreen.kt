package com.intent.reader.ui.reader

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intent.reader.R
import com.intent.reader.ui.components.HeaderIconButton
import com.intent.reader.ui.components.LocalHeaderMaterial
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.withCjkSpacing
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Reader View: title + plain-text body + source + time (doc 4.1.3).
 * Includes TTS read-aloud and per-minute reading timer.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpenInBrowser: (String) -> Unit,
    onShare: (String) -> Unit,
    onAiSummary: () -> Unit,
    onRegenerateSummary: () -> Unit,
    onDismissSummary: () -> Unit,
    onOpenNotes: () -> Unit,
    onSaveNote: suspend (com.intent.reader.notes.NoteParagraph, String) -> Unit,
    onDeleteNote: suspend (String) -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val context = LocalContext.current
    val article = state.article
    var selectedParagraph by remember(article?.id) { mutableStateOf<com.intent.reader.notes.NoteParagraph?>(null) }
    val notesByParagraph = remember(state.notes) { state.notes.associateBy { it.paragraphKey } }
    // One empty line below every paragraph: the body line height (already 1.4x)
    // so the gap is a true blank line at the current font/display scale.
    val blankLine = with(androidx.compose.ui.platform.LocalDensity.current) {
        (t.body.lineHeight.value * 1.4f * fontScale).dp
    }

    // TTS honours the engine / rate / pitch picked in settings. The instance is
    // rebuilt whenever the chosen engine changes, so switching engines (or a
    // missing default engine) actually takes effect.
    val scope = rememberCoroutineScope()
    var speaker by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    DisposableEffect(state.ttsEngine) {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var disposed = false // Read/written only on the main thread.
        val listener = TextToSpeech.OnInitListener { status ->
            mainHandler.post {
                if (!disposed) ttsReady = status == TextToSpeech.SUCCESS
            }
        }
        val engine = TextToSpeech(context.applicationContext, listener, state.ttsEngine.ifBlank { null })
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                mainHandler.post { if (!disposed) speaking = false }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { if (!disposed) speaking = false }
            }
        })
        speaker = engine
        onDispose {
            disposed = true
            engine.setOnUtteranceProgressListener(null)
            engine.stop()
            engine.shutdown()
            mainHandler.removeCallbacksAndMessages(null)
            speaker = null
            ttsReady = false
            speaking = false
        }
    }

    val material = LocalHeaderMaterial.current
    val statusTop = with(androidx.compose.ui.platform.LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    val toolbarHeight = statusTop + 64.dp
    val toolbar: @Composable () -> Unit = {
        TopAppBar(
            title = {
                Text(
                    state.source?.title ?: "",
                    style = t.meta,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.notes_back), onBack)
            },
            actions = {
                if (article != null) {
                    HeaderIconButton(Icons.Outlined.Bookmarks, stringResource(R.string.notes_this_article), onOpenNotes)
                    HeaderIconButton(Icons.Outlined.AutoAwesome, stringResource(R.string.ai_summary), onAiSummary, tint = c.accent)
                    HeaderIconButton(
                        icon = if (speaking) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        label = stringResource(if (speaking) R.string.reader_stop_tts else R.string.reader_read_aloud),
                        onClick = {
                            if (speaking) {
                                speaker?.stop()
                                speaking = false
                            } else {
                                scope.launch {
                                    // The engine initialises asynchronously; wait for it
                                    // instead of the old fixed 600 ms delay.
                                    var waited = 0
                                    while (!ttsReady && waited < 4000) {
                                        kotlinx.coroutines.delay(100)
                                        waited += 100
                                    }
                                    val engine = speaker
                                    if (ttsReady && engine != null) {
                                        engine.setSpeechRate(state.ttsRate)
                                        engine.setPitch(state.ttsPitch)
                                        val plain = com.intent.reader.rss.HtmlText.toPlain(
                                            article.contentHtml ?: article.summary ?: article.title
                                        )
                                        engine.speak(plain, TextToSpeech.QUEUE_FLUSH, null, "reader")
                                        speaking = true
                                    }
                                }
                            }
                        },
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent, titleContentColor = c.textSecondary,
            ),
        )
    }

    Box(modifier.fillMaxSize().background(c.bg)) {
    Column(Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(toolbarHeight))

        if (article == null) {
            Box(Modifier.fillMaxSize())
            return@Column
        }

        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = IntentDimens.pageHMarginPhone.dp, vertical = 8.dp),
        ) {
            Text(
                text = article.title.withCjkSpacing(),
                style = t.pageTitle,
                color = c.text,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val time = java.time.Instant.ofEpochMilli(article.publishedAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                Text(
                    text = listOfNotNull(
                        state.source?.title,
                        article.author?.takeIf { it.isNotBlank() },
                        time
                    ).joinToString(" · "),
                    style = t.meta,
                    color = c.textSecondary,
                )
            }
            Spacer(Modifier.height(4.dp))
            // reading timer indicator
            Text(
                text = stringResource(R.string.reader_today_progress, state.minutesToday, state.secondsToday, state.goalMinutes),
                style = t.micro,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.notes_reader_hint), style = t.micro, color = c.textSecondary)
            if (state.notesError) Text(stringResource(R.string.notes_load_error), style = t.micro, color = c.accent)
            Spacer(Modifier.height(12.dp))

            // Body: interleave paragraphs with inline images (rounded corners)
            val blocks = remember(article.contentHtml, article.summary) {
                com.intent.reader.rss.HtmlText.toBlocks(article.contentHtml ?: article.summary)
            }
            val paragraphs = remember(blocks) { com.intent.reader.notes.NoteParagraphs.from(blocks) }
            val highlightAction = stringResource(R.string.notes_highlight_action)
            blocks.forEachIndexed { index, block ->
                when (block) {
                    is com.intent.reader.rss.HtmlText.Block.Para -> {
                        val paragraph = paragraphs.getValue(index)
                        val note = notesByParagraph[paragraph.key]
                        Text(
                            text = paragraph.text.withCjkSpacing(),
                            style = t.body.copy(lineHeight = t.body.lineHeight * 1.4f),
                            color = c.text,
                            modifier = Modifier.fillMaxWidth()
                                .background(if (note != null) c.accent.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
                                .combinedClickable(
                                    enabled = state.notesReady,
                                    onClick = { if (note != null) selectedParagraph = paragraph },
                                    onLongClickLabel = highlightAction,
                                    onLongClick = { selectedParagraph = paragraph },
                                ),
                        )
                        if (note?.remark?.isNotBlank() == true) Text(
                            stringResource(R.string.notes_has_remark), style = t.micro, color = c.accent,
                            modifier = Modifier.clickable { selectedParagraph = paragraph }.padding(top = 4.dp),
                        )
                        Spacer(Modifier.height(blankLine))
                    }
                    is com.intent.reader.rss.HtmlText.Block.Img -> {
                        coil.compose.AsyncImage(
                            model = block.url,
                            contentDescription = block.alt,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(IntentDimens.radiusCard.dp))
                                .background(c.heroBg),
                        )
                        if (!block.alt.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            // Caption: centered small secondary text so it reads as
                            // an annotation, not another body paragraph.
                            Text(
                                text = block.alt,
                                style = t.micro,
                                color = c.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            if (article.link.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                // Capsule button with a spring "bounce" on press instead of a
                // rectangular ripple.
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.94f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "openInBrowserBounce",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .background(c.surface)
                        .border(1.dp, c.divider, CircleShape)
                        .clickable(interactionSource = interaction, indication = null) {
                            onOpenInBrowser(article.link)
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.reader_open_in_browser) + " →",
                        style = t.button,
                        color = c.text,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(CircleShape)
                        .background(c.surface)
                        .border(1.dp, c.divider, CircleShape)
                        .clickable { onShare(article.link) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.reader_share), style = t.button, color = c.text)
                }
            }
        }
    }
    // Keep actions available while text scrolls underneath, with the same
    // fading treatment as the Home status edge instead of an opaque safe area.
    Box(Modifier.fillMaxWidth().height(toolbarHeight + 24.dp).background(
        androidx.compose.ui.graphics.Brush.verticalGradient(
            0f to c.bg,
            0.72f to c.bg.copy(alpha = 0.96f),
            1f to c.bg.copy(alpha = 0f),
        ),
    ))
    toolbar()
    }

    selectedParagraph?.let { paragraph ->
        val note = notesByParagraph[paragraph.key]
        key(article?.id, paragraph.key) {
            NoteEditor(paragraph.text, note?.remark.orEmpty(), onDismiss = { selectedParagraph = null },
                onSave = { onSaveNote(paragraph, it) },
                onDelete = note?.let { { onDeleteNote(it.id) } },
            )
        }
    }

    if (state.summaryVisible && article != null) {
        ModalBottomSheet(onDismissRequest = onDismissSummary, containerColor = c.bg) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.ai_summary), style = t.pageTitle, color = c.text)
                Text(
                    stringResource(if (com.intent.reader.ai.AiRepository.articleText(article).excerptOnly)
                        R.string.ai_excerpt_only else R.string.ai_content_scope),
                    style = t.meta, color = c.textSecondary,
                )
                if (state.summaryLoading) Text(stringResource(R.string.ai_loading), style = t.body, color = c.textSecondary)
                state.aiSummary?.let { Text(it, style = t.body, color = c.text) }
                state.summaryError?.let { Text(stringResource(it), style = t.meta, color = c.textSecondary) }
                Text(stringResource(R.string.ai_output_hint), style = t.micro, color = c.textSecondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(enabled = !state.summaryLoading, onClick = onRegenerateSummary) {
                        Text(stringResource(R.string.ai_regenerate))
                    }
                    TextButton(onClick = onDismissSummary) { Text(stringResource(R.string.close)) }
                }
            }
        }
    }
}
