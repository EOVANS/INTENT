package com.intent.reader.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.intent.reader.R
import com.intent.reader.data.db.Article
import com.intent.reader.domain.CardStyle
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.withCjkSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Source badge: logo + name, top-left of every card (doc 5). */
@Composable
fun SourceBadge(
    name: String,
    logoUrl: String?,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SourceLogo(logoUrl, name, 16.dp)
        Text(
            text = name,
            style = IntentTheme.typography.micro,
            color = if (dark) Color.White.copy(alpha = 0.85f) else IntentTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Source logo with favicon caching and initial fallback (doc 3.5). */
@Composable
fun SourceLogo(logoUrl: String?, name: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cachedPath by produceState<String?>(null, logoUrl, size) {
        value = withContext(Dispatchers.IO) {
            if (logoUrl.isNullOrBlank()) return@withContext null
            val key = logoUrl.hashCode().toString(16) + ".png"
            val f = File(File(context.cacheDir, "favicons").apply { mkdirs() }, key)
            try {
                if (!f.exists() || f.length() == 0L) {
                    val req = okhttp3.Request.Builder().url(logoUrl).build()
                    val resp = okhttp3.OkHttpClient().newCall(req).execute()
                    resp.use { r ->
                        if (r.isSuccessful) r.body?.byteStream()?.use { input ->
                            f.outputStream().use { input.copyTo(it) }
                        } ?: return@withContext null
                    }
                }
                if (f.length() > 0) f.absolutePath else null
            } catch (_: Exception) {
                null
            }
        }
    }
    val accent = IntentTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(accent.heroBg),
        contentAlignment = Alignment.Center,
    ) {
        if (cachedPath != null) {
            AsyncImage(
                model = cachedPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "•",
                style = MaterialTheme.typography.labelSmall,
                color = accent.textSecondary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Article image with graceful degradation (never shows a broken placeholder). */
@Composable
fun ArticleImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    corner: Int = 0,
) {
    if (url.isNullOrBlank()) return
    Box(modifier = modifier.clip(RoundedCornerShape(corner.dp))) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
        )
    }
}

@Composable
private fun baseCardModifier(onClick: () -> Unit, description: String): Modifier {
    val c = IntentTheme.colors
    return Modifier
        .clip(RoundedCornerShape(IntentDimens.radiusCard.dp))
        .background(c.card)
        .border(1.dp, c.divider, RoundedCornerShape(IntentDimens.radiusCard.dp))
        .clickable(onClick = onClick)
        .semantics { contentDescription = description }
}

private fun buildDescription(sourceTitle: String, article: Article): String =
    "$sourceTitle: ${article.title}"

/** Style A: big image top, white text area below (doc 5.1). */
@Composable
fun CardA(article: Article, sourceTitle: String, sourceLogo: String?, onClick: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        modifier = baseCardModifier(onClick, buildDescription(sourceTitle, article))
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            ArticleImage(article.imageUrl, Modifier.fillMaxSize())
        }
        Column(Modifier.padding(IntentDimens.cardInnerPadding.dp)) {
            SourceBadge(sourceTitle, sourceLogo, dark = false)
            Spacer(Modifier.height(8.dp))
            Text(
                text = article.title.withCjkSpacing(),
                style = t.cardTitle.copy(
                    fontWeight = if (!article.isRead) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (!article.isRead) c.text else c.textSecondary,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Style B: full-bleed image + gradient scrim + white title (doc 5.2). */
@Composable
fun CardB(article: Article, sourceTitle: String, sourceLogo: String?, onClick: () -> Unit) {
    BoxWithConstraints(
        modifier = baseCardModifier(onClick, buildDescription(sourceTitle, article))
    ) {
        // When promoted to the full row (orphan of an odd half-width run) a 3:4
        // portrait crop would be absurdly tall, so widen it to 16:9 instead.
        val imageRatio = if (maxWidth >= 320.dp) 16f / 9f else 3f / 4f
        ArticleImage(
            article.imageUrl,
            Modifier.fillMaxWidth().aspectRatio(imageRatio),
        )
        // Progressive blur: a duplicate of the image sits exactly on top of the
        // base, blurred and masked by a vertical gradient so the blur fades in
        // toward the bottom edge. API 31+ only — on API 30 blur() is a no-op, so
        // the plain dark scrim below still provides the contrast transition.
        if (Build.VERSION.SDK_INT >= 31) {
            Box(
                Modifier
                    .matchParentSize()
                    .blur(26.dp)
                    .drawWithContent {
                        drawContent()
                        // Transparent at the top (blur erased), opaque at the
                        // bottom (blur kept) -> soft fade into the text.
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.35f to Color.Transparent,
                                1f to Color.Black,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
            ) {
                ArticleImage(article.imageUrl, Modifier.fillMaxSize())
            }
        }
        // dark scrim bottom 40% — keeps the white title readable on any image
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.25f),
                        1f to Color.Black.copy(alpha = 0.7f),
                    )
                )
        )
        // top scrim for badge contrast
        Box(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.2f),
                        1f to Color.Transparent,
                    )
                )
        )
        SourceBadge(
            sourceTitle, sourceLogo, dark = true,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )
        Text(
            text = article.title.withCjkSpacing(),
            style = IntentTheme.typography.cardTitle.copy(
                color = Color.White,
                fontWeight = if (!article.isRead) FontWeight.SemiBold else FontWeight.Normal,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp),
        )
    }
}

/** Style C: horizontal text-left image-right (doc 5.3). */
@Composable
fun CardC(article: Article, sourceTitle: String, sourceLogo: String?, onClick: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Row(
        modifier = baseCardModifier(onClick, buildDescription(sourceTitle, article))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            SourceBadge(sourceTitle, sourceLogo, dark = false)
            Spacer(Modifier.height(6.dp))
            Text(
                text = article.title.withCjkSpacing(),
                style = t.cardTitle.copy(
                    fontWeight = if (!article.isRead) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (!article.isRead) c.text else c.textSecondary,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            article.summary?.let {
                Text(
                    text = it.withCjkSpacing(),
                    style = t.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        ArticleImage(
            article.imageUrl,
            Modifier.size(96.dp),
            corner = 12,
        )
    }
}

/** Style D: text-only horizontal card (doc 5.4). */
@Composable
fun CardD(article: Article, sourceTitle: String, sourceLogo: String?, onClick: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        modifier = baseCardModifier(onClick, buildDescription(sourceTitle, article))
            .padding(IntentDimens.cardInnerPadding.dp)
    ) {
        SourceBadge(sourceTitle, sourceLogo, dark = false)
        Spacer(Modifier.height(8.dp))
        Text(
            text = article.title.withCjkSpacing(),
            style = t.cardTitle.copy(
                fontWeight = if (!article.isRead) FontWeight.SemiBold else FontWeight.Normal,
                color = if (!article.isRead) c.text else c.textSecondary,
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        article.summary?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it.withCjkSpacing(),
                style = t.bodySmall,
                color = c.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Style E: same-source daily aggregate (doc 5.5). */
@Composable
fun CardE(
    sourceTitle: String,
    sourceLogo: String?,
    items: List<Article>,
    onViewAll: () -> Unit,
    onArticle: (Article) -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        modifier = baseCardModifier(onViewAll, "$sourceTitle, ${items.size}")
            .padding(vertical = 6.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = IntentDimens.cardInnerPadding.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceBadge(sourceTitle, sourceLogo, dark = false, modifier = Modifier.weight(1f))
            Text(
                text = items.size.toString(),
                style = t.meta,
                color = c.textSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(c.heroBg)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
        items.forEachIndexed { i, a ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onArticle(a) }
                    .padding(horizontal = IntentDimens.cardInnerPadding.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = a.title.withCjkSpacing(),
                    style = t.cardTitle.copy(
                        fontSize = t.cardTitle.fontSize * 0.95f,
                        fontWeight = if (!a.isRead) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (!a.isRead) c.text else c.textSecondary,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                ArticleImage(a.imageUrl, Modifier.size(56.dp), corner = 8)
            }
            if (i < items.size - 1) {
                Box(
                    Modifier
                        .padding(horizontal = IntentDimens.cardInnerPadding.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(c.divider)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

/** Dispatches the right card composable for a style. */
@Composable
fun ArticleCard(
    style: CardStyle,
    article: Article,
    sourceTitle: String,
    sourceLogo: String?,
    onClick: () -> Unit,
) {
    when (style) {
        CardStyle.A -> CardA(article, sourceTitle, sourceLogo, onClick)
        CardStyle.B -> CardB(article, sourceTitle, sourceLogo, onClick)
        CardStyle.C -> CardC(article, sourceTitle, sourceLogo, onClick)
        CardStyle.D -> CardD(article, sourceTitle, sourceLogo, onClick)
        CardStyle.E -> CardE(sourceTitle, sourceLogo, listOf(article), onClick) {}
    }
}
