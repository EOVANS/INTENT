package com.intent.reader.data

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Clears image / disk caches without ever touching the Room database or the
 * DataStore settings. The size reported by the settings row is measured with
 * the same walk used after clearing, so the two numbers agree.
 */
object CacheCleaner {

    /** Coil 2.x keeps its DiskLruCache here; the folder is cleared via its API. */
    private const val COIL_DIR = "image_cache"

    suspend fun sizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        cacheBytes(context)
    }

    /** Returns the bytes freed. Safe to call from a background scope. */
    @OptIn(ExperimentalCoilApi::class)
    suspend fun clear(context: Context): Long = withContext(Dispatchers.IO) {
        val before = cacheBytes(context)
        runCatching { context.imageLoader.memoryCache?.clear() }
        // DiskLruCache holds its journal open: clear through the API instead of
        // deleting the directory underneath a live cache.
        runCatching { context.imageLoader.diskCache?.clear() }
        wipeExcept(context.cacheDir, COIL_DIR)
        wipeExcept(context.externalCacheDir, null)
        val after = cacheBytes(context)
        (before - after).coerceAtLeast(0L)
    }

    private fun cacheBytes(context: Context): Long =
        dirSize(context.cacheDir) + dirSize(context.externalCacheDir)

    private fun dirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun wipeExcept(dir: File?, keepName: String?) {
        if (dir == null || !dir.isDirectory) return
        dir.listFiles()?.forEach { child ->
            if (keepName != null && child.name == keepName) return@forEach
            runCatching { child.deleteRecursively() }
        }
    }
}
