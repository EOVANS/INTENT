package com.intent.reader.ui.reader

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.withCjkSpacing
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Reader View: title + plain-text body + source + time (doc 4.1.3).
 * Includes TTS read-aloud and per-minute reading timer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    onBack: () -> Unit,
    onOpenInBrowser: (String) -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val context = LocalContext.current
    val article = state.article
    // One empty line below every paragraph: the body line height (already 1.4x)
    // so the gap is a true blank line at the current font/display scale.
    val blankLine = with(androidx.compose.ui.platform.LocalDensity.current) {
        (t.body.lineHeight.value * 1.4f * fontScale).dp
    }

    // TTS honours the engine / rate / pitch picked in settings. The instance is
    // rebuilt whenever the chosen engine changes, so switching engines (or a
    // missing default engine) actually takes effect.
    val scope = rememberCoroutineScope()
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var speaker by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    DisposableEffect(state.ttsEngine) {
        val listener = TextToSpeech.OnInitListener { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        val engine = TextToSpeech(context, listener, state.ttsEngine.ifBlank { null })
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                mainHandler.post { speaking = false }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { speaking = false }
            }
        })
        speaker = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            speaker = null
            ttsReady = false
            speaking = false
        }
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
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
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = c.text)
                }
            },
            actions = {
                if (article != null) {
                    IconButton(onClick = {
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
                    }) {
                        Icon(
                            imageVector = if (speaking) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(
                                if (speaking) R.string.reader_stop_tts else R.string.reader_read_aloud
                            ),
                            tint = c.text,
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = c.bg, titleContentColor = c.textSecondary,
            ),
        )

        if (article == null) {
            Box(Modifier.fillMaxSize())
            return@Column
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                text = stringResource(R.string.live_update_progress, state.minutesToday, state.goalMinutes),
                style = t.micro,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(16.dp))

            // Body: interleave paragraphs with inline images (rounded corners)
            val blocks = remember(article.contentHtml, article.summary) {
                com.intent.reader.rss.HtmlText.toBlocks(article.contentHtml ?: article.summary)
            }
            blocks.forEach { block ->
                when (block) {
                    is com.intent.reader.rss.HtmlText.Block.Para -> {
                        // Split long paragraphs on sentence boundaries for breathing room
                        block.text.split(Regex("(?<=[。！？.!?])\\s+"))
                            .filter { it.isNotBlank() }
                            .forEach { para ->
                                Text(
                                    text = para.withCjkSpacing(),
                                    style = t.body.copy(lineHeight = t.body.lineHeight * 1.4f),
                                    color = c.text,
                                )
                                Spacer(Modifier.height(blankLine))
                            }
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
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = block.alt,
                                style = t.micro,
                                color = c.textSecondary,
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
            }
        }
    }
}
