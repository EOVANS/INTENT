package com.intent.reader.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import java.io.File
import java.security.MessageDigest
import kotlin.math.roundToInt

/** Small widget snapshots survive ordinary cache clearing and never enter cloud backup. */
internal class WidgetArtworkStore(context: Context) {
    private val directory = File(context.noBackupFilesDir, "widget-artwork")

    fun read(url: String, width: Int, height: Int): Bitmap? = synchronized(lock) {
        runCatching {
            val file = File(directory, name(url))
            if (!file.exists()) return@synchronized null
            val bitmap = BitmapFactory.decodeFile(file.path) ?: return@synchronized null
            fit(bitmap, width, height)
        }.getOrNull()
    }

    fun save(url: String, bitmap: Bitmap) = synchronized(lock) {
        directory.mkdirs()
        val file = AtomicFile(File(directory, name(url)))
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        if (file.baseFile.exists()) BitmapFactory.decodeFile(file.baseFile.path, bounds)
        // A thumbnail or logo pass must not overwrite a larger article snapshot.
        if (bounds.outWidth >= bitmap.width && bounds.outHeight >= bitmap.height) return@synchronized
        val output = file.startWrite()
        try {
            check(fit(bitmap, 640, 640).compress(Bitmap.CompressFormat.PNG, 100, output))
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    fun retain(urls: Set<String>) = synchronized(lock) {
        val names = urls.map(::name).toSet()
        directory.listFiles()?.filter { it.name !in names }?.forEach { it.delete() }
    }

    private fun name(url: String): String = MessageDigest.getInstance("SHA-256")
        .digest(url.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) } + ".png"

    private fun fit(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val scale = minOf(1f, width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
        return if (scale == 1f) bitmap else Bitmap.createScaledBitmap(bitmap,
            (bitmap.width * scale).roundToInt().coerceAtLeast(1), (bitmap.height * scale).roundToInt().coerceAtLeast(1), true)
    }

    companion object { private val lock = Any() }
}
