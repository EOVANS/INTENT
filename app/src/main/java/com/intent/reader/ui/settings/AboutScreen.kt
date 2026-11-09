package com.intent.reader.ui.settings

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.intent.reader.BuildConfig
import com.intent.reader.R
import com.intent.reader.ui.components.HeaderIconButton
import com.intent.reader.ui.theme.IntentTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val SITE_BASE = "https://intentsonnet.pages.dev"
private const val URL_INTENT_VOICE = "https://cynosure-atelier.pages.dev/"
private const val URL_OFFICIAL_WEBSITE = "https://intentread.pages.dev/"
private const val URL_CHANGELOG = "$SITE_BASE/changelog.html"
private const val URL_PRIVACY = "$SITE_BASE/privacy.html"
private const val URL_TERMS = "$SITE_BASE/terms.html"
private const val URL_HELP = "$SITE_BASE/help.html"
private const val URL_LATEST_RELEASE =
    "https://api.github.com/repos/EOVANS/INTENT/releases/latest"

/** Latest published release, as reported by GitHub. */
private data class LatestRelease(val tag: String, val url: String)

/**
 * About: app logo, the INTENT wordmark in AgibotDisplay, a Release capsule tag,
 * the current version, an update check, and legal entries that open in an
 * in-app WebView (never an external browser).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, onOpenPage: (title: String, url: String) -> Unit, onOpenDeveloper: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    // Uploaded AgibotDisplay-Bold, bundled as an asset; fall back to the
    // greeting serif if the asset ever goes missing.
    val fallbackFamily = t.pageTitle.fontFamily ?: FontFamily.Default
    val wordmark = remember {
        runCatching { FontFamily(Font("fonts/agibot_display_bold.ttf", context.assets)) }
            .getOrDefault(fallbackFamily)
    }

    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf<LatestRelease?>(null) }
    val checkingLabel = stringResource(R.string.checking_update)
    val checkLabel = stringResource(R.string.check_update)
    val upToDate = stringResource(R.string.update_latest)
    val checkFailed = stringResource(R.string.update_failed)

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopAppBar(
            title = { Text(stringResource(R.string.about), style = t.pageTitle) },
            navigationIcon = {
                // Inside the wide two-pane layout the section index stays visible, so the
                // page does not need an arrow of its own.
                if (!LocalSettingsPaneEmbedded.current) {
                    HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.notes_back), onBack)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = c.bg,
                titleContentColor = c.text,
            ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(36.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = R.mipmap.ic_launcher,
                    contentDescription = null,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .pointerInput(onOpenDeveloper) {
                            detectTapGestures(onDoubleTap = { onOpenDeveloper() }, onLongPress = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenDeveloper()
                            })
                        },
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "INTENT",
                    fontFamily = wordmark,
                    fontSize = 30.sp,
                    color = c.text,
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(c.accent.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text("Release", style = t.meta, color = c.accent)
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = t.meta,
                    color = c.textSecondary,
                )
            }
            Spacer(Modifier.height(32.dp))

            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.about_thanks_title), style = t.body, fontWeight = FontWeight.Bold, color = c.text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.about_thanks_body), style = t.bodySmall, color = c.textSecondary, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(horizontal = 16.dp)) {
            AboutEntry(if (checking) checkingLabel else checkLabel) {
                if (checking) return@AboutEntry
                checking = true
                scope.launch {
                    val latest = fetchLatestRelease()
                    checking = false
                    when {
                        latest == null -> Toast
                            .makeText(context, checkFailed, Toast.LENGTH_SHORT)
                            .show()
                        isNewerVersion(latest.tag, BuildConfig.VERSION_NAME) -> available = latest
                        else -> Toast
                            .makeText(context, upToDate, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            AboutEntry(stringResource(R.string.visit_official_website)) {
                onOpenPage(context.getString(R.string.visit_official_website), URL_OFFICIAL_WEBSITE)
            }
            AboutEntry(stringResource(R.string.about_intent_voice), showDivider = false) {
                onOpenPage(context.getString(R.string.about_intent_voice), URL_INTENT_VOICE)
            }
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(horizontal = 16.dp)) {
            AboutEntry(stringResource(R.string.help_center)) {
                onOpenPage(context.getString(R.string.help_center), URL_HELP)
            }
            AboutEntry(stringResource(R.string.changelog)) {
                onOpenPage(context.getString(R.string.changelog), URL_CHANGELOG)
            }
            AboutEntry(stringResource(R.string.privacy_policy)) {
                onOpenPage(context.getString(R.string.privacy_policy), URL_PRIVACY)
            }
            AboutEntry(stringResource(R.string.terms_of_use), showDivider = false) {
                onOpenPage(context.getString(R.string.terms_of_use), URL_TERMS)
            }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                "Copyright © 2026 Cynosure/Atelier All rights reserved.",
                modifier = Modifier.fillMaxWidth(),
                style = t.meta,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    available?.let { release ->
        AlertDialog(
            onDismissRequest = { available = null },
            containerColor = c.surface,
            title = {
                Text(
                    stringResource(R.string.update_available_title),
                    style = t.sectionTitle,
                    color = c.text,
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.update_available_message,
                        release.tag,
                        BuildConfig.VERSION_NAME,
                    ),
                    style = t.body,
                    color = c.textSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        available = null
                        onOpenPage(release.tag, release.url)
                    },
                ) {
                    Text(stringResource(R.string.update_action), color = c.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { available = null }) {
                    Text(stringResource(R.string.close), color = c.textSecondary)
                }
            },
        )
    }
}

@Composable
private fun AboutEntry(label: String, showDivider: Boolean = true, onClick: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp)) {
            Text(label, style = t.body, color = c.text)
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
    }
}

@Composable
fun DeveloperScreen(onBack: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val context = LocalContext.current
    val app = context.applicationContext as com.intent.reader.IntentApplication
    val settings by app.container.settingsRepository.settings.collectAsState(initial = com.intent.reader.data.prefs.SettingsState())
    SettingsScaffold(title = stringResource(R.string.developer_title), onBack = onBack) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(80.dp).clip(CircleShape).background(c.card), contentAlignment = Alignment.Center) {
                Image(painter = painterResource(R.drawable.developer_avatar), contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.width(200.dp).height(72.dp)) {
                val w = size.width
                val h = size.height
                val signature = Path().apply {
                    // Flowing capital J followed by connected lowercase a, y, d, e and n.
                    moveTo(w * .10f, h * .25f)
                    cubicTo(w * .20f, h * .13f, w * .35f, h * .14f, w * .40f, h * .20f)
                    cubicTo(w * .35f, h * .16f, w * .31f, h * .20f, w * .29f, h * .34f)
                    cubicTo(w * .27f, h * .51f, w * .25f, h * .76f, w * .17f, h * .78f)
                    cubicTo(w * .11f, h * .80f, w * .10f, h * .69f, w * .16f, h * .64f)
                    moveTo(w * .28f, h * .63f)
                    cubicTo(w * .31f, h * .45f, w * .39f, h * .45f, w * .38f, h * .56f)
                    cubicTo(w * .37f, h * .68f, w * .30f, h * .70f, w * .30f, h * .63f)
                    cubicTo(w * .30f, h * .53f, w * .37f, h * .48f, w * .39f, h * .49f)
                    cubicTo(w * .37f, h * .68f, w * .42f, h * .67f, w * .45f, h * .54f)
                    cubicTo(w * .44f, h * .70f, w * .48f, h * .70f, w * .52f, h * .51f)
                    cubicTo(w * .49f, h * .70f, w * .47f, h * .89f, w * .40f, h * .94f)
                    cubicTo(w * .36f, h * .97f, w * .37f, h * .89f, w * .42f, h * .85f)
                    cubicTo(w * .49f, h * .77f, w * .54f, h * .69f, w * .57f, h * .57f)
                    cubicTo(w * .60f, h * .45f, w * .66f, h * .47f, w * .65f, h * .56f)
                    cubicTo(w * .64f, h * .68f, w * .57f, h * .70f, w * .57f, h * .62f)
                    cubicTo(w * .57f, h * .55f, w * .63f, h * .49f, w * .65f, h * .50f)
                    cubicTo(w * .67f, h * .39f, w * .69f, h * .23f, w * .70f, h * .20f)
                    cubicTo(w * .68f, h * .37f, w * .64f, h * .65f, w * .67f, h * .66f)
                    cubicTo(w * .70f, h * .67f, w * .73f, h * .60f, w * .75f, h * .56f)
                    cubicTo(w * .80f, h * .47f, w * .81f, h * .52f, w * .77f, h * .58f)
                    cubicTo(w * .74f, h * .63f, w * .74f, h * .68f, w * .79f, h * .65f)
                    cubicTo(w * .83f, h * .62f, w * .84f, h * .55f, w * .85f, h * .52f)
                    cubicTo(w * .83f, h * .66f, w * .83f, h * .69f, w * .86f, h * .59f)
                    cubicTo(w * .90f, h * .47f, w * .94f, h * .46f, w * .92f, h * .57f)
                    cubicTo(w * .90f, h * .67f, w * .94f, h * .65f, w * .98f, h * .58f)
                }
                drawPath(signature, color = c.text, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(stringResource(R.string.developer_owner), stringResource(R.string.developer_king), "PRESIDENT").forEach { label ->
                    Text(label, style = t.micro, color = c.text, modifier = Modifier.clip(CircleShape).background(c.card).padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
        SectionLabel(stringResource(R.string.developer_team))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(horizontal = 16.dp, vertical = 8.dp)) {
            val staff = listOf(
                Triple("Terra", R.string.developer_senior_dev, "OpenAI"),
                Triple("Astra", R.string.developer_architect, "OpenAI"),
                Triple("Deepseek 4.1 Flash", R.string.developer_architect, "DeepSeek"),
                Triple("Fable", R.string.developer_senior_engineer, "Anthropic"),
                Triple("Seedcode", R.string.developer_assistant, "ByteDance"),
                Triple("Qwen 3.8 Max", R.string.developer_marketing, "Alibaba"),
            )
            staff.forEach { (name, role, company) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(c.card).border(1.dp, c.divider, CircleShape), contentAlignment = Alignment.Center) {
                        val logo = when (company) {
                            "OpenAI" -> R.drawable.brand_openai
                            "DeepSeek" -> R.drawable.brand_deepseek
                            "Anthropic" -> R.drawable.brand_anthropic
                            "ByteDance" -> R.drawable.brand_bytedance
                            else -> R.drawable.brand_alibabacloud
                        }
                        Icon(painterResource(logo), contentDescription = company, tint = c.text, modifier = Modifier.size(27.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        DeveloperShimmerName(name, settings.reduceAnimations)
                        Spacer(Modifier.height(3.dp))
                        Text(stringResource(R.string.developer_from, company), style = t.meta, color = c.textSecondary)
                        Spacer(Modifier.height(3.dp))
                        Text(stringResource(role), style = t.meta, color = c.textSecondary)
                    }
                }
            }
        }
        Text(stringResource(R.string.developer_rights), style = t.micro, color = c.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 20.dp))
    }
}

@Composable
private fun DeveloperShimmerName(name: String, reduceAnimations: Boolean) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    if (reduceAnimations) {
        Text(name, style = t.body, fontWeight = FontWeight.SemiBold, color = c.text)
        return
    }
    var width by remember { mutableIntStateOf(1) }
    val transition = rememberInfiniteTransition(label = "developer name shimmer")
    val progress by transition.animateFloat(
        initialValue = -0.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(animation = tween(2600, delayMillis = 900, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "light sweep",
    )
    val center = width * progress
    val brush = Brush.linearGradient(
        colors = listOf(c.text, Color(0xFF438BFF), c.text),
        start = Offset(center - width * 0.28f, 0f),
        end = Offset(center + width * 0.28f, 0f),
    )
    Text(name, style = t.body.copy(brush = brush), fontWeight = FontWeight.SemiBold, modifier = Modifier.onSizeChanged { width = it.width.coerceAtLeast(1) })
}

/** Reads the latest GitHub release; null when the network or payload fails. */
private suspend fun fetchLatestRelease(): LatestRelease? =
    withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(URL_LATEST_RELEASE)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "INTENT/${BuildConfig.VERSION_NAME}")
                .build()
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val json = JSONObject(response.body?.string().orEmpty())
                val tag = json.optString("tag_name")
                LatestRelease(tag, json.optString("html_url"))
            }
        }.getOrNull()
    }

/** True when [latest] (e.g. "v1.2") is a newer dotted version than [current]. */
private fun isNewerVersion(latest: String, current: String): Boolean {
    fun numbers(value: String) = Regex("\\d+").findAll(value).map { it.value.toInt() }.toList()
    val a = numbers(latest)
    val b = numbers(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/** In-app web page with a retryable error state for main-frame failures. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPageScreen(title: String, url: String, onBack: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var loadAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(loadAttempt, loading) {
        if (loading) {
            delay(20_000)
            if (loading) {
                failed = true
                loading = false
                webView?.stopLoading()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopAppBar(
            title = { Text(title, style = t.sectionTitle) },
            navigationIcon = {
                HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.notes_back), onBack)
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg, titleContentColor = c.text),
        )
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, pageUrl, favicon)
                                if (!failed) loading = true
                            }
                            override fun onPageFinished(view: WebView, pageUrl: String?) {
                                super.onPageFinished(view, pageUrl)
                                loading = false
                            }
                            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                super.onReceivedError(view, request, error)
                                if (request.isForMainFrame) { failed = true; loading = false }
                            }
                            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                                super.onReceivedHttpError(view, request, response)
                                if (request.isForMainFrame && response.statusCode >= 400) { failed = true; loading = false }
                            }
                        }
                        webView = this
                        loadUrl(url)
                    }
                },
            )
            if (failed) {
                Column(
                    Modifier.fillMaxSize().background(c.bg).padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Icon(Icons.Filled.CloudOff, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(76.dp))
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.web_load_failed), style = t.body, color = c.textSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            failed = false
                            loading = true
                            loadAttempt++
                            webView?.reload()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = c.text, contentColor = c.bg),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.web_retry))
                    }
                }
            } else if (loading) {
                Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.accent)
                }
            }
        }
    }
}



