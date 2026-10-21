package com.intent.reader.ui.settings

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.intent.reader.R
import com.intent.reader.ui.theme.IntentTheme

private const val URL_CHANGELOG =
    "https://app.notion.com/p/400ad4a3cd7e80a59f92e57ab904719e?source=copy_link"
private const val URL_PRIVACY =
    "https://app.notion.com/p/400ad4a3cd7e804daaf5dd4dbf3d0567?source=copy_link"
private const val URL_TERMS =
    "https://app.notion.com/p/400ad4a3cd7e804ca3aefd1aec969112?source=copy_link"

/**
 * About: app logo, the INTENT wordmark in AgibotDisplay, a Beta capsule tag,
 * and legal entries that open in an in-app WebView (never an external browser).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, onOpenPage: (title: String, url: String) -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val context = LocalContext.current
    // Uploaded AgibotDisplay-Bold, bundled as an asset; fall back to the
    // greeting serif if the asset ever goes missing.
    val fallbackFamily = t.pageTitle.fontFamily ?: FontFamily.Default
    val wordmark = remember {
        runCatching { FontFamily(Font("fonts/agibot_display_bold.ttf", context.assets)) }
            .getOrDefault(fallbackFamily)
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopAppBar(
            title = { Text(stringResource(R.string.about), style = t.pageTitle) },
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
                        .clip(RoundedCornerShape(22.dp)),
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
                    Text("Beta", style = t.meta, color = c.accent)
                }
            }
            Spacer(Modifier.height(40.dp))

            AboutEntry(stringResource(R.string.changelog)) {
                onOpenPage(context.getString(R.string.changelog), URL_CHANGELOG)
            }
            AboutEntry(stringResource(R.string.privacy_policy)) {
                onOpenPage(context.getString(R.string.privacy_policy), URL_PRIVACY)
            }
            AboutEntry(stringResource(R.string.terms_of_use)) {
                onOpenPage(context.getString(R.string.terms_of_use), URL_TERMS)
            }
        }
    }
}

@Composable
private fun AboutEntry(label: String, onClick: () -> Unit) {
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
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
    }
}

/** In-app web page (Notion changelog / privacy / terms) with a back bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPageScreen(title: String, url: String, onBack: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopAppBar(
            title = { Text(title, style = t.sectionTitle) },
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
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(url)
                }
            },
        )
    }
}
