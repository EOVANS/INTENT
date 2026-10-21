package com.intent.reader.widget

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.intent.reader.R

private val White = ColorProvider(Color.White)
private val White70 = ColorProvider(Color(0xB3FFFFFF))
private val BadgeBg = ColorProvider(Color(0x66000000))
private val ChevronBg = ColorProvider(Color(0x59000000))

/**
 * "大图" style for both widget sizes: full-bleed article artwork, a dark
 * gradient scrim at the bottom with an accent "|" bar + white title, and a
 * source badge (circular logo + name) pinned to the top-left corner.
 * [large] selects 4×4 vs 4×2 metrics. The chevron cycles the featured article.
 */
@Composable
fun ImageStyleContent(
    state: WidgetSnapshot,
    bg: Bitmap?,
    logo: Bitmap?,
    large: Boolean,
) {
    val context = LocalContext.current
    val colors = WidgetColors(state.accentColor, state.themeMode)
    val size = state.articles.size
    val article = if (size == 0) null else state.articles[Math.floorMod(state.startIndex, size)]

    if (article == null) {
        // Empty: fall back to the classic empty state (no artwork to show).
        Box(
            GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(colors.surface)
                .then(
                    if (Build.VERSION.SDK_INT < 31) GlanceModifier.cornerRadius(28.dp)
                    else GlanceModifier,
                )
                .clickable(openAppAction(context)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                context.getString(R.string.widget_empty),
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = 14.sp),
            )
        }
        return
    }

    val time = formatWidgetTime(article.publishedAt)
    val a11y = buildString {
        append(article.sourceTitle).append("，")
        append(article.title).append("，").append(time)
        if (!article.isRead) append("，").append(context.getString(R.string.widget_unread))
    }

    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .semantics { contentDescription = a11y }
            .then(
                if (Build.VERSION.SDK_INT < 31) GlanceModifier.cornerRadius(28.dp)
                else GlanceModifier,
            ),
        contentAlignment = Alignment.TopStart,
    ) {
        if (bg != null) {
            Image(
                provider = ImageProvider(bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize(),
            )
        } else {
            // No cached artwork: keep the layout on a quiet placeholder fill.
            Box(GlanceModifier.fillMaxSize().background(colors.surfaceVariant)) {}
        }

        Column(
            GlanceModifier
                .fillMaxSize()
                .clickable(openArticleAction(context, article.articleId)),
        ) {
            // Top-left source badge: circular logo + name on a dark pill.
            Row(GlanceModifier.padding(10.dp)) {
                Box(
                    GlanceModifier
                        .background(BadgeBg)
                        .cornerRadius(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        GlanceModifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (logo != null) {
                            Image(
                                provider = ImageProvider(logo),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = GlanceModifier
                                    .size(16.dp)
                                    .cornerRadius(8.dp),
                            )
                            Spacer(GlanceModifier.width(5.dp))
                        }
                        Text(
                            article.sourceTitle,
                            maxLines = 1,
                            style = TextStyle(
                                color = White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
            }

            Spacer(GlanceModifier.defaultWeight())

            // Bottom block: scrim + accent bar + title (+ cycle chevron).
            Box(GlanceModifier.fillMaxWidth().background(ImageProvider(R.drawable.widget_scrim))) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(
                        GlanceModifier
                            .defaultWeight()
                            .padding(start = 14.dp, end = 6.dp, top = 28.dp, bottom = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                GlanceModifier
                                    .width(4.dp)
                                    .height(if (large) 20.dp else 16.dp)
                                    .background(colors.accent)
                                    .cornerRadius(2.dp),
                            ) {}
                            Spacer(GlanceModifier.width(8.dp))
                            Text(
                                article.title,
                                maxLines = 2,
                                style = TextStyle(
                                    color = White,
                                    fontSize = if (large) 16.sp else 13.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        Spacer(GlanceModifier.height(3.dp))
                        Text(
                            time,
                            maxLines = 1,
                            style = TextStyle(color = White70, fontSize = 10.sp),
                        )
                    }
                    if (size > 1) {
                        Box(
                            GlanceModifier
                                .size(48.dp)
                                .semantics {
                                    contentDescription = context.getString(R.string.widget_next)
                                }
                                .clickable(
                                    CycleArticleAction.forTarget(if (large) "large" else "small"),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                GlanceModifier
                                    .size(32.dp)
                                    .background(ChevronBg)
                                    .cornerRadius(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_next),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(White),
                                    modifier = GlanceModifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
