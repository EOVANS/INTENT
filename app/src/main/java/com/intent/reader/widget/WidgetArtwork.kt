package com.intent.reader.widget

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.glance.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class WidgetArtwork(
    val thumbs: Map<String, Bitmap> = emptyMap(),
    val images: Map<String, Bitmap> = emptyMap(),
    val logos: Map<String, Bitmap> = emptyMap(),
)

/** Reload assets when a live Glance session receives fresh data, not just at session creation. */
@Composable
internal fun widgetArtwork(snapshot: WidgetSnapshot, thumbSize: Int, width: Int, height: Int): WidgetArtwork {
    val context = LocalContext.current
    val assets by produceState(WidgetArtwork(), snapshot.articles, snapshot.artworkRevision) {
        value = withContext(Dispatchers.IO) {
            WidgetArtwork(
                WidgetLoader.loadThumbs(context, snapshot.articles, 8, thumbSize),
                WidgetLoader.loadBitmaps(context, snapshot.articles, width, height),
                WidgetLoader.loadLogos(context, snapshot.articles, 96),
            )
        }
    }
    return assets
}
