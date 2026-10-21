package com.intent.reader.widget

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun formatWidgetTime(ts: Long): String =
    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).format(timeFormatter)

/** 4×4 "今日文章" widget: accent header + today's article list + view-all footer. */
class TodayLargeWidget : GlanceAppWidget() {

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
            val bg = WidgetLoader.loadBitmap(context, featured?.imageUrl, 480, 480)
            val logo = WidgetLoader.loadBitmap(context, featured?.sourceLogoUrl, 96, 96)
                ?.let { WidgetLoader.circleCrop(it, 96) }
            provideContent {
                ImageStyleContent(currentState<Preferences>().toWidgetSnapshot(), bg, logo, large = true)
            }
        } else {
            val thumbs = WidgetLoader.loadThumbs(context, fresh.articles, maxCount = 5, sizePx = 128)
            provideContent {
                LargeWidgetContent(currentState<Preferences>().toWidgetSnapshot(), thumbs)
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val demo = listOf(
            WidgetArticle("p1", "示例文章标题：今天的第一条更新", "示例来源", System.currentTimeMillis(), null, false),
            WidgetArticle("p2", "另一条稍早的更新，已读状态", "INTENT", System.currentTimeMillis() - 3600_000, null, true),
            WidgetArticle("p3", "第三条新闻标题会占据两行空间以展示截断效果", "预览", System.currentTimeMillis() - 7200_000, null, false),
        )
        provideContent {
            LargeWidgetContent(
                WidgetSnapshot(0xFFE02B2B, "FOLLOW_SYSTEM", "classic", demo, 0),
                emptyMap(),
            )
        }
    }
}

@Composable
private fun LargeWidgetContent(state: WidgetSnapshot, thumbs: Map<String, Bitmap>) {
    val context = LocalContext.current
    val colors = WidgetColors(state.accentColor, state.themeMode)
    Column(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(colors.surface)
            .then(
                if (Build.VERSION.SDK_INT < 31) GlanceModifier.cornerRadius(28.dp)
                else GlanceModifier,
            ),
    ) {
        // Accent header: wordmark + today's count. Tap opens the app.
        Row(
            GlanceModifier
                .fillMaxWidth()
                .background(colors.accent)
                .clickable(openAppAction(context))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "INTENT",
                GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = colors.onAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                ),
            )
            Text(
                context.getString(R.string.widget_today_count, state.articles.size),
                style = TextStyle(
                    color = colors.onAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }

        if (state.articles.isEmpty()) {
            Box(
                GlanceModifier
                    .defaultWeight()
                    .fillMaxWidth()
                    .clickable(openAppAction(context)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    context.getString(R.string.widget_empty),
                    style = TextStyle(color = colors.onSurfaceVariant, fontSize = 14.sp),
                )
            }
        } else {
            LazyColumn(GlanceModifier.defaultWeight().fillMaxWidth()) {
                itemsIndexed(
                    state.articles,
                    itemId = { _, a -> a.articleId.hashCode().toLong() },
                ) { index, article ->
                    Column(GlanceModifier.fillMaxWidth()) {
                        val time = formatWidgetTime(article.publishedAt)
                        val a11y = buildString {
                            append(article.sourceTitle).append("，")
                            append(article.title).append("，").append(time)
                            if (!article.isRead) {
                                append("，").append(context.getString(R.string.widget_unread))
                            }
                        }
                        Row(
                            GlanceModifier
                                .fillMaxWidth()
                                .semantics { contentDescription = a11y }
                                .clickable(openArticleAction(context, article.articleId))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(GlanceModifier.defaultWeight()) {
                                Text(
                                    article.title,
                                    maxLines = 2,
                                    style = TextStyle(
                                        color = colors.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = if (article.isRead) FontWeight.Normal
                                        else FontWeight.Bold,
                                    ),
                                )
                                Spacer(GlanceModifier.height(4.dp))
                                Text(
                                    "${article.sourceTitle} · $time",
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = colors.onSurfaceVariant,
                                        fontSize = 11.sp,
                                    ),
                                )
                            }
                            thumbs[article.articleId]?.let { bmp ->
                                Spacer(GlanceModifier.width(8.dp))
                                Image(
                                    provider = ImageProvider(bmp),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = GlanceModifier
                                        .size(56.dp)
                                        .cornerRadius(12.dp),
                                )
                            }
                        }
                        if (index < state.articles.lastIndex) {
                            Box(
                                GlanceModifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .background(colors.outline),
                            ) {}
                        }
                    }
                }
            }
        }

        // Footer: view-all entry point.
        Box(
            GlanceModifier
                .fillMaxWidth()
                .clickable(openAppAction(context))
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                context.getString(R.string.widget_view_all),
                style = TextStyle(
                    color = colors.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}
