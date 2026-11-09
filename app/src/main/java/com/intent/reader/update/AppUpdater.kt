package com.intent.reader.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.intent.reader.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** One downloadable build attached to a GitHub release. */
data class UpdateAsset(
    val url: String,
    val size: Long,
    /** "sha256:<hex>" as reported by GitHub, when present. */
    val digest: String?,
)

/** Everything the update UI needs from the latest published release. */
data class UpdateRelease(
    val tag: String,
    val pageUrl: String,
    /** Release notes; empty when the release has no body. */
    val notes: String,
    val apk: UpdateAsset?,
)

/**
 * Checks for and downloads a new INTENT build straight from GitHub Releases.
 * Downloading stays inside the app: the APK is streamed into the cache directory
 * and then handed to the system package installer.
 */
object AppUpdater {
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/EOVANS/INTENT/releases/latest"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /** Latest release, or null when the network or payload is unavailable. */
    suspend fun fetchLatest(): UpdateRelease? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "INTENT/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val json = JSONObject(response.body?.string().orEmpty())
                val assets = json.optJSONArray("assets")
                var apk: UpdateAsset? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val item = assets.optJSONObject(i) ?: continue
                        val name = item.optString("name")
                        if (!name.endsWith(".apk", ignoreCase = true)) continue
                        val candidate = UpdateAsset(
                            url = item.optString("browser_download_url"),
                            size = item.optLong("size"),
                            digest = item.optString("digest").takeIf { it.isNotBlank() },
                        )
                        // Prefer the INTENT.<version>.apk convention, otherwise keep the first APK.
                        if (apk == null || name.startsWith("INTENT.")) apk = candidate
                    }
                }
                UpdateRelease(
                    tag = json.optString("tag_name"),
                    pageUrl = json.optString("html_url"),
                    notes = json.optString("body").orEmpty().trim(),
                    apk = apk,
                )
            }
        }.getOrNull()
    }

    /**
     * Streams [release]'s APK into cache/updates, reporting progress in 0..1.
     * Fails when the transfer is interrupted or the digest does not match.
     */
    suspend fun download(
        context: Context,
        release: UpdateRelease,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val asset = release.apk ?: error("no apk asset")
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(dir, "INTENT.${release.tag.removePrefix("v")}.apk")
            val request = Request.Builder()
                .url(asset.url)
                .header("User-Agent", "INTENT/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("http ${response.code}")
                val body = response.body ?: error("empty body")
                val total = body.contentLength().takeIf { it > 0 } ?: asset.size
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var copied = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            if (total > 0) onProgress((copied.toFloat() / total).coerceIn(0f, 1f))
                        }
                        output.flush()
                        if (total > 0 && copied != total) error("incomplete download")
                    }
                }
            }
            val expected = asset.digest?.substringAfter("sha256:", "")?.lowercase()?.takeIf { it.isNotBlank() }
            if (expected != null) {
                val actual = sha256(target)
                if (actual != expected) {
                    target.delete()
                    error("digest mismatch")
                }
            }
            target
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Android 8+ requires the user to allow installing apps from INTENT. */
    fun needsInstallPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** Hands the downloaded APK to the system installer; false when nothing can handle it. */
    fun install(context: Context, file: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.notes-share", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
