package com.intent.reader

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.intent.reader.data.CacheCleaner
import com.intent.reader.widget.WidgetArtworkStore
import com.intent.reader.widget.WidgetLoader
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class WidgetArtworkStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val store get() = WidgetArtworkStore(context)
    private val url = "https://example.invalid/widget-photo.png"

    @Before fun setup() { store.retain(emptySet()) }
    @After fun cleanup() { store.retain(emptySet()) }

    @Test fun widgetSnapshotSurvivesOrdinaryCacheClearing() = runBlocking {
        store.save(url, Bitmap.createBitmap(320, 160, Bitmap.Config.ARGB_8888))
        val temporary = File(context.cacheDir, "ordinary-image.tmp").apply { writeText("image cache") }

        CacheCleaner.clear(context)

        assertFalse(temporary.exists())
        assertNotNull(store.read(url, 320, 160))
    }

    @Test(timeout = 20_000) fun offlineWidgetCanResolveArtworkAfterCoilCacheWasCleared() = runBlocking {
        store.save(url, Bitmap.createBitmap(320, 160, Bitmap.Config.ARGB_8888))
        CacheCleaner.clear(context)

        val request = async(Dispatchers.IO) { WidgetLoader.loadBitmap(context, url, 160, 80) }
        val bitmap = withTimeout(10_000) {
            // Coil dispatches lifecycle work to Android's main looper, which Robolectric pauses.
            while (!request.isCompleted) {
                shadowOf(Looper.getMainLooper()).idle()
                delay(10)
            }
            request.await()
        }

        assertNotNull(bitmap)
        assertTrue(bitmap!!.width <= 160)
        assertTrue(bitmap.height <= 80)
    }

    @Test fun storedImagesAreBoundedAndThumbnailPassCannotDowngradeArtwork() {
        store.save(url, Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888))
        store.save(url, Bitmap.createBitmap(96, 64, Bitmap.Config.ARGB_8888))

        val bitmap = store.read(url, 1200, 800)!!

        assertEquals(640, bitmap.width)
        assertTrue(bitmap.height <= 640)
        assertTrue(bitmap.width > 96)
    }

    @Test fun retainDropsStaleImagesAndNeverReturnsAnotherArticlesPicture() {
        val other = "https://example.invalid/another-photo.png"
        store.save(url, Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888))
        store.save(other, Bitmap.createBitmap(60, 30, Bitmap.Config.ARGB_8888))

        store.retain(setOf(other))

        assertNull(store.read(url, 80, 40))
        assertNotNull(store.read(other, 60, 30))
        assertNull(store.read("https://example.invalid/unknown.png", 80, 40))
    }
}
