package com.intent.reader.widget

import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll

/**
 * One entry point for "re-render every home-screen widget now". Called after
 * the accent color / theme mode is saved, and after each feed sync.
 */
object IntentWidgets {

    suspend fun refreshAll(context: Context) {
        // No widgets placed yet -> updateAll is a no-op; failures must never
        // break the caller (settings save / sync worker).
        runCatching { TodayLargeWidget().updateAll(context) }
        runCatching { TodaySmallWidget().updateAll(context) }
    }

    /**
     * Publishes generated (live RemoteViews) previews to the system widget
     * picker. Only available on API 35+; rate-limited by the system, so this
     * is called once from application start and failures are ignored.
     */
    suspend fun publishPickerPreviews(context: Context) {
        if (Build.VERSION.SDK_INT < 35) return
        val manager = GlanceAppWidgetManager(context)
        runCatching { manager.setWidgetPreviews(TodayLargeWidgetReceiver::class) }
        runCatching { manager.setWidgetPreviews(TodaySmallWidgetReceiver::class) }
    }
}
