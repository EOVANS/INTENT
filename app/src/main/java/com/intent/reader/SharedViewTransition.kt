package com.intent.reader

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import com.itgsa.animation.sharedview.ViewAnimationConfig
import com.itgsa.animation.sharedview.ViewAnimationExtras
import com.itgsa.animation.sharedview.ViewAnimationListener
import com.itgsa.animation.sharedview.ViewAnimationManager
import com.itgsa.animation.sharedview.ViewAnimationType
import java.util.Locale

/** Bridges a Compose card snapshot to the SDK's Activity-based bitmap API. */
internal object SharedViewTransition {
    private data class Target(val window: Rect, val screen: Rect, val width: Int, val height: Int)
    private val targets = mutableMapOf<String, Target>()
    private var activeArticleId: String? = null
    private var activeSafeTop = 0
    private var activeSafeBottom = 0

    fun updateTarget(id: String, window: Rect, screen: Rect, width: Int, height: Int) {
        targets[id] = Target(Rect(window), Rect(screen), width, height)
    }

    fun removeTarget(id: String) { targets.remove(id) }

    fun isSupported(): Boolean = try {
        val version = ViewAnimationManager.isViewAnimationSupported()
        if (BuildConfig.DEBUG) android.util.Log.d("SharedViewTransition", "Protocol version: $version")
        version > 0
    } catch (_: LinkageError) { false } catch (_: RuntimeException) { false }

    private fun useWindowCoordinates(): Boolean {
        val value = ViewAnimationManager.getViewAnimationExtras()
            ?.get(ViewAnimationExtras.EXTRA_BOUNDS) ?: return false
        val parts = value.split('|')
        val token = parts.getOrNull(1)?.trim()?.uppercase(Locale.ROOT)
        return when (token) {
            "WINDOW" -> true
            "SCREEN" -> false
            else -> value.uppercase(Locale.ROOT).contains("WINDOW")
        }
    }

    private val listener = object : ViewAnimationListener {
        override fun onAnimationStart(animMode: Int) = Unit
        override fun onAnimationEnd(animMode: Int) = Unit
        override fun onAnimationCancel(animMode: Int) = Unit
    }

    private fun visibleTarget(target: Target, safeTop: Int, safeBottom: Int): Target? {
        val visible = Rect(target.window)
        if (!visible.intersect(0, safeTop.coerceAtLeast(0), target.width, (target.height - safeBottom).coerceAtMost(target.height))) return null
        val screen = Rect(visible).apply {
            offset(target.screen.left - target.window.left, target.screen.top - target.window.top)
        }
        return Target(visible, screen, target.width, target.height)
    }

    /** Returns false through [done] on any failure so navigation can use Compose. */
    fun open(activity: MainActivity, id: String, radiusPx: Float, safeTop: Int, safeBottom: Int, backgroundColor: Int, isNote: Boolean = false, done: (Boolean) -> Unit) {
        val targetKey = if (isNote) "note:$id" else id
        val original = targets[targetKey]
        val decor = activity.window.decorView
        val target = original?.let { visibleTarget(it, safeTop, safeBottom) }
        if (target == null || target.width != decor.width || target.height != decor.height ||
            target.window.width() <= 0 || target.window.height() <= 0) {
            done(false)
            return
        }
        val bitmap = try {
            Bitmap.createBitmap(target.window.width(), target.window.height(), Bitmap.Config.ARGB_8888)
        } catch (_: RuntimeException) { done(false); return }
        try {
            PixelCopy.request(activity.window, target.window, bitmap, { result ->
                if (result != PixelCopy.SUCCESS || activity.isFinishing || activity.isDestroyed) {
                    bitmap.recycle()
                    done(false)
                } else {
                    val started = try {
                        val bounds = if (useWindowCoordinates()) target.window else target.screen
                        val radius = radiusPx.coerceAtMost(minOf(target.window.width(), target.window.height()) / 2f)
                        val config = ViewAnimationConfig().setColor(backgroundColor).toBundle()
                        val options = ViewAnimationManager.makeViewAnimationOptions(bitmap, bounds, radius, config, listener)
                        if (options == null) false else {
                            activeArticleId = targetKey
                            activeSafeTop = safeTop
                            activeSafeBottom = safeBottom
                            activity.openingKitReader = true
                            val intent = Intent(activity, ReaderActivity::class.java)
                                .putExtra(MainActivity.EXTRA_ARTICLE_ID, id)
                                .putExtra(MainActivity.EXTRA_NOTES, isNote)
                            activity.startActivity(intent, options.toBundle())
                            true
                        }
                    } catch (_: LinkageError) { false } catch (_: RuntimeException) { false }
                    if (!started) {
                        activeArticleId = null
                        activity.openingKitReader = false
                        bitmap.recycle()
                    }
                    done(started)
                }
            }, Handler(Looper.getMainLooper()))
        } catch (_: RuntimeException) {
            bitmap.recycle()
            done(false)
        }
    }

    fun beforeFinish(activity: ReaderActivity) {
        val articleId = activity.intent.getStringExtra(MainActivity.EXTRA_ARTICLE_ID)
        val id = if (activity.intent.getBooleanExtra(MainActivity.EXTRA_NOTES, false)) "note:$articleId" else articleId
        if (id != activeArticleId) return
        val target = targets[id]?.let { visibleTarget(it, activeSafeTop, activeSafeBottom) }
        try {
            if (target != null && target.width == activity.window.decorView.width && target.height == activity.window.decorView.height) {
                val bounds = if (useWindowCoordinates()) target.window else target.screen
                ViewAnimationManager.overrideViewAnimationTransition(activity, ViewAnimationConfig().setBounds(bounds).toBundle())
            } else {
                val supported = ViewAnimationManager.getViewAnimationExtras()?.get(ViewAnimationExtras.EXTRA_BACK_ANIM_TYPE)
                val values = supported?.split('|')
                if (supported != null && (values == null || values.size < 2 ||
                        values[1].split(',').any { it.trim() == "VIEW_ANIM_FOLLOW_SYSTEM" })) {
                    val config = ViewAnimationConfig().setBackAnimType(ViewAnimationType.VIEW_ANIM_FOLLOW_SYSTEM)
                    ViewAnimationManager.overrideViewAnimationTransition(activity, config.toBundle())
                }
            }
        } catch (_: LinkageError) { } catch (_: RuntimeException) { }
        activeArticleId = null
    }
}

class ReaderActivity : MainActivity() {
    override fun finish() {
        SharedViewTransition.beforeFinish(this)
        super.finish()
    }
}
