package com.intent.reader.widget

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.intent.reader.R

/**
 * 4×2 "今日速览" widget: accent rail + a window of article cards + next-page
 * chevron. RemoteViews has no horizontally scrolling collection, so Glance
 * offers no LazyRow: the cards are a fixed window (2 cards, 3 when the widget
 * is stretched wide) and the chevron cycles the window through today's list.
 */
class TodaySmallWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val fresh = WidgetLoader.load(context)
        val size = fresh.articles.size
        val startIndex = if (size == 0) 0 else Math.floorMod(
            androidx.glance.appwidget.state.getAppWidgetState(
                context, PreferencesGlanceStateDefinition, id,
            )[WidgetKeys.startIndex] ?: 0,
            size,
        )
        updateAppWidgetState(context, id) { prefs ->
            prefs[WidgetKeys.accentColor] = fresh.accentColor
            prefs[WidgetKeys.themeMode] = fresh.themeMode
            prefs[WidgetKeys.style] = fresh.style
            prefs[WidgetKeys.articlesJson] = WidgetStateCodec.encodeArticles(fresh.articles)
            prefs[WidgetKeys.startIndex] = startIndex
        }
        if (fresh.style == "image") {
            val featured = fresh.articles.getOrNull(startIndex)
            val bg = WidgetLoader.loadBitmap(context, featured?.imageUrl, 640, 320)
            val logo = WidgetLoader.loadBitmap(context, featured?.sourceLogoUrl, 96, 96)
                ?.let { WidgetLoader.circleCrop(it, 96) }
            provideContent {
                ImageStyleContent(currentState<Preferences>().toWidgetSnapshot(), bg, logo, large = false)
            }
        } else {
            val thumbs = WidgetLoader.loadThumbs(context, fresh.articles, maxCount = 3, sizePx = 96)
            provideContent {
                SmallWidgetContent(currentState<Preferences>().toWidgetSnapshot(), thumbs)
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val demo = listOf(
            WidgetArticle("p1", "示例文章标题", "示例来源", System.currentTimeMillis(), null, false),
            WidgetArticle("p2", "另一条更新", "INTENT", System.currentTimeMillis() - 3600_000, null, true),
        )
        provideContent {
            SmallWidgetContent(
                WidgetSnapshot(0xFFE02B2B, "FOLLOW_SYSTEM", "classic", demo, 0),
                emptyMap(),
            )
        }
    }
}

@Composable
private fun SmallWidgetContent(state: WidgetSnapshot, thumbs: Map<String, Bitmap>) {
    val context = LocalContext.current
    val colors = WidgetColors(state.accentColor, state.themeMode)
    Row(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(colors.surface)
            .then(
                if (Build.VERSION.SDK_INT < 31) GlanceModifier.cornerRadius(28.dp)
                else GlanceModifier,
            ),
    ) {
        // Accent rail: wordmark. Tap opens the app.
        Box(
            GlanceModifier
                .width(64.dp)
                .fillMaxHeight()
                .background(colors.accent)
                .clickable(openAppAction(context)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "INTENT",
                style = TextStyle(
                    color = colors.onAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                ),
            )
        }

        if (state.articles.isEmpty()) {
            Box(
                GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .clickable(openAppAction(context)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    context.getString(R.string.widget_empty),
                    style = TextStyle(color = colors.onSurfaceVariant, fontSize = 13.sp),
                )
            }
        } else {
            val size = state.articles.size
            val maxVisible = if (LocalSize.current.width >= 300.dp) 3 else 2
            val window = (0 until minOf(maxVisible, size))
                .map { state.articles[(state.startIndex + it) % size] }
            Row(
                GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                window.forEach { article ->
                    SmallArticleCard(
                        article = article,
                        thumb = thumbs[article.articleId],
                        colors = colors,
                        context = context,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    )
                }
            }
            // Next-page chevron, pinned to the bottom-right corner (>= 48dp).
            // Hidden when there's nothing to cycle to.
            if (size > 1) {
                Column(GlanceModifier.width(48.dp).fillMaxHeight()) {
                    Spacer(GlanceModifier.defaultWeight())
                    Box(
                        GlanceModifier
                            .size(48.dp)
                            .semantics {
                                contentDescription = context.getString(R.string.widget_next)
                            }
                            .clickable(CycleArticleAction.forTarget("small")),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_next),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colors.accent),
                            modifier = GlanceModifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallArticleCard(
    article: WidgetArticle,
    thumb: Bitmap?,
    colors: WidgetColors,
    context: Context,
    modifier: GlanceModifier,
) {
    val time = formatWidgetTime(article.publishedAt)
    val a11y = buildString {
        append(article.sourceTitle).append("，")
        append(article.title).append("，").append(time)
        if (!article.isRead) append("，").append(context.getString(R.string.widget_unread))
    }
    Row(
        modifier
            .semantics { contentDescription = a11y }
            .clickable(openArticleAction(context, article.articleId))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (thumb != null) {
            Image(
                provider = ImageProvider(thumb),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier
                    .size(44.dp)
                    .cornerRadius(10.dp),
            )
            Spacer(GlanceModifier.width(8.dp))
        }
        Column(GlanceModifier.defaultWeight()) {
            Text(
                article.title,
                maxLines = 2,
                style = TextStyle(
                    color = colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = if (article.isRead) FontWeight.Normal else FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                "${article.sourceTitle} · $time",
                maxLines = 1,
                style = TextStyle(
                    color = colors.onSurfaceVariant,
                    fontSize = 10.sp,
                ),
            )
        }
    }
}
