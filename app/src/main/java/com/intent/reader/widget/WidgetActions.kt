package com.intent.reader.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.state.updateAppWidgetState
import com.intent.reader.MainActivity

/** Shared click actions for the widgets. */
fun openAppAction(context: Context): Action =
    actionStartActivity(Intent(context, MainActivity::class.java))

fun openArticleAction(context: Context, articleId: String): Action =
    actionStartActivity(articleIntent(context, articleId))

/** Extras do not identify PendingIntents: a distinct data URI keeps each article's tap separate. */
internal fun articleIntent(context: Context, articleId: String): Intent =
    Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .setData(android.net.Uri.Builder().scheme("intent-reader").authority("widget").appendPath(articleId).build())
        .putExtra(MainActivity.EXTRA_ARTICLE_ID, articleId)

/**
 * Bottom-right chevron: advances the article window by one. The target widget
 * is passed as a parameter so one callback serves both sizes and both styles.
 */
class CycleArticleAction : ActionCallback {

    companion object {
        val TargetKey = ActionParameters.Key<String>("cycle_target")

        fun forTarget(target: String): Action =
            actionRunCallback<CycleArticleAction>(actionParametersOf(TargetKey to target))
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val size = WidgetStateCodec
                .decodeArticles(prefs[WidgetKeys.articlesJson] ?: "[]")
                .size
            val current = prefs[WidgetKeys.startIndex] ?: 0
            prefs[WidgetKeys.startIndex] = if (size <= 1) 0 else (current + 1) % size
        }
        when (parameters[TargetKey]) {
            "large" -> TodayLargeWidget().update(context, glanceId)
            else -> TodaySmallWidget().update(context, glanceId)
        }
    }
}
