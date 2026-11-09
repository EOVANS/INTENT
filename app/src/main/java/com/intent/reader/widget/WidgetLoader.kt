package com.intent.reader.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.intent.reader.data.db.AppDatabase
import com.intent.reader.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

/**
 * Loads fresh widget data straight from Room + DataStore. The widget never
 * touches the network — artwork comes from Coil's local cache or the widget's
 * own small snapshots, which survive ordinary cache clearing.
 */
object WidgetLoader {

    data class FreshState(
        val accentColor: Long,
        val themeMode: String,
        val style: String,
        val articles: List<WidgetArticle>,
    )

    /** Today's articles (published since local midnight), newest first, capped at 8. */
    suspend fun load(context: Context): FreshState {
        val settings = SettingsRepository(context).settings.first()
        val dayStart = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val dao = AppDatabase.get(context).articleDao()
        // Early in the morning "today" is often empty; fall back to the latest
        // articles so the widget only shows the empty state when the DB truly is.
        val rows = dao.getTodayForWidget(dayStart, limit = 8)
            .ifEmpty { dao.getTodayForWidget(0L, limit = 8) }
        return FreshState(
            accentColor = settings.accentColor,
            themeMode = settings.themeMode.name,
            style = settings.widgetStyle,
            articles = rows.map {
                WidgetArticle(
                    articleId = it.id,
                    title = it.title,
                    sourceTitle = it.sourceTitle,
                    publishedAt = it.publishedAt,
                    imageUrl = it.imageUrl,
                    isRead = it.isRead,
                    sourceLogoUrl = it.sourceLogoUrl,
                )
            },
        )
    }

    /**
     * Resolves thumbnails for the first [maxCount] articles at [sizePx] pixels.
     * Network is disabled: use local caches and widget snapshots only.
     * Bitmaps are kept small because they cross a Binder transaction.
     */
    suspend fun loadThumbs(
        context: Context,
        articles: List<WidgetArticle>,
        maxCount: Int,
        sizePx: Int,
    ): Map<String, Bitmap> {
        val out = LinkedHashMap<String, Bitmap>(maxCount)
        for (article in articles.take(maxCount)) {
            val url = article.imageUrl ?: continue
            val bitmap = loadBitmap(context, url, sizePx, sizePx)
            if (bitmap != null) out[article.articleId] = bitmap
        }
        return out
    }

    /** Single bitmap at an exact size (image-style background / source logo). */
    suspend fun loadBitmap(context: Context, url: String?, widthPx: Int, heightPx: Int): Bitmap? {
        if (url.isNullOrBlank()) return null
        val request = ImageRequest.Builder(context)
            .data(url)
            .networkCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .allowHardware(false)
            .size(widthPx, heightPx)
            .build()
        val bitmap = ((context.imageLoader.execute(request) as? SuccessResult)?.drawable
            as? BitmapDrawable)?.bitmap
        val snapshots = WidgetArtworkStore(context)
        if (bitmap != null) {
            runCatching { snapshots.save(url, bitmap) }
            return bitmap
        }
        return snapshots.read(url, widthPx, heightPx)
    }

    /**
     * Bitmaps for every article, keyed by article id. The image style needs the
     * artwork of whichever article the state points at; loading all of them up
     * front keeps the picture in sync when the chevron advances the window,
     * because the composition can pick from the map by id.
     */
    suspend fun loadBitmaps(
        context: Context,
        articles: List<WidgetArticle>,
        widthPx: Int,
        heightPx: Int,
    ): Map<String, Bitmap> {
        val out = LinkedHashMap<String, Bitmap>()
        val byUrl = HashMap<String, Bitmap>()
        for (article in articles) {
            val url = article.imageUrl ?: continue
            val bitmap = byUrl.getOrPut(url) {
                loadBitmap(context, url, widthPx, heightPx) ?: NULL_BITMAP
            }
            if (bitmap !== NULL_BITMAP) out[article.articleId] = bitmap
        }
        return out
    }

    /** Logos for every article, keyed by article id. */
    suspend fun loadLogos(
        context: Context,
        articles: List<WidgetArticle>,
        sizePx: Int,
    ): Map<String, Bitmap> {
        val out = LinkedHashMap<String, Bitmap>()
        val byUrl = HashMap<String, Bitmap>()
        for (article in articles) {
            val url = article.sourceLogoUrl ?: continue
            val cropped = byUrl.getOrPut(url) {
                loadBitmap(context, url, sizePx, sizePx)?.let { circleCrop(it, sizePx) }
                    ?: NULL_BITMAP
            }
            if (cropped !== NULL_BITMAP) out[article.articleId] = cropped
        }
        return out
    }

    /** Placeholder marking a URL that resolved to nothing, so it is tried once. */
    private val NULL_BITMAP = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    /** Circle-crops a logo bitmap for the image style's top-left badge. */
    fun circleCrop(src: Bitmap, sizePx: Int): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val rect = android.graphics.RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        canvas.drawOval(rect, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        val scale = maxOf(sizePx.toFloat() / src.width, sizePx.toFloat() / src.height)
        val w = src.width * scale
        val h = src.height * scale
        canvas.drawBitmap(
            src, null,
            android.graphics.RectF((sizePx - w) / 2f, (sizePx - h) / 2f, (sizePx + w) / 2f, (sizePx + h) / 2f),
            paint,
        )
        return out
    }
}
