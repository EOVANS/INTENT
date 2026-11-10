package com.intent.reader.widget

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

/**
 * One entry point for "re-render every home-screen widget now". Called after
 * the accent color / theme mode is saved, after each feed sync, and when the
 * user switches the widget style.
 *
 * Glance's `update()` only re-runs `provideContent` for a live session, so the
 * state is written first and the rendering branch reads it from there. That is
 * what makes a style switch apply immediately instead of after a process
 * restart.
 */
object IntentWidgets {

    suspend fun refreshAll(context: Context) {
        push(context, WidgetLoader.load(context))
    }

    /**
     * Applies the "创建桌面卡片" consent switch: disabling the providers hides
     * them from the launcher picker and stops placed widgets from refreshing.
     */
    suspend fun setCreationEnabled(context: Context, enabled: Boolean) {
        val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        listOf(TodayLargeWidgetReceiver::class.java, TodaySmallWidgetReceiver::class.java).forEach { receiver ->
            runCatching {
                context.packageManager.setComponentEnabledSetting(
                    ComponentName(context, receiver), state, PackageManager.DONT_KILL_APP,
                )
            }
        }
        if (enabled) refreshAll(context)
    }

    /** Switches the style and repaints right away (no app restart needed). */
    suspend fun setStyle(context: Context, style: String) {
        push(context, WidgetLoader.load(context).copy(style = style))
        publishPickerPreviews(context)
    }

    private suspend fun push(context: Context, fresh: WidgetLoader.FreshState) {
        // No widgets placed yet -> nothing to push; failures must never break
        // the caller (settings save / sync worker).
        runCatching { pushOne(context, TodayLargeWidget(), fresh) }
        runCatching { pushOne(context, TodaySmallWidget(), fresh) }
    }

    private suspend fun <W : GlanceAppWidget> pushOne(
        context: Context,
        widget: W,
        fresh: WidgetLoader.FreshState,
    ) {
        val ids: List<GlanceId> =
            runCatching { GlanceAppWidgetManager(context).getGlanceIds(widget.javaClass) }
                .getOrDefault(emptyList())
        for (id in ids) {
            runCatching {
                updateAppWidgetState(context, id) { it.putWidgetData(fresh) }
                widget.update(context, id)
            }
        }
    }

    /**
     * Publishes generated (live RemoteViews) previews to the system widget
     * picker. Only available on API 35+; rate-limited by the system, so this
     * is called once from application start and after a style switch.
     */
    suspend fun publishPickerPreviews(context: Context) {
        if (Build.VERSION.SDK_INT < 35) return
        val manager = GlanceAppWidgetManager(context)
        runCatching { manager.setWidgetPreviews(TodayLargeWidgetReceiver::class) }
        runCatching { manager.setWidgetPreviews(TodaySmallWidgetReceiver::class) }
    }
}
