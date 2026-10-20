package com.intent.reader.sync

import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Site metadata discovery: feed auto-discovery and favicon lookup.
 * Favicon priority: /apple-touch-icon.png -> <link rel="icon"> -> /favicon.ico.
 */
object SiteDiscovery {

    data class SiteInfo(
        val feedUrl: String?,
        val siteUrl: String?,
        val title: String?,
        val iconUrl: String?,
    )

    fun fetch(client: OkHttpClient, url: String): Result<SiteInfo> = try {
        val resp = client.newCall(
            Request.Builder().url(normalizeUrl(url)).header("User-Agent", FeedFetcher.USER_AGENT).build()
        ).execute()
        resp.use { r ->
            if (!r.isSuccessful) return Result.failure(IllegalStateException("HTTP ${r.code}"))
            val contentType = r.header("Content-Type") ?: ""
            val body = r.body?.string().orEmpty()
            if (contentType.contains("html") || body.contains("<html", ignoreCase = true) ||
                body.contains("<!DOCTYPE", ignoreCase = true)
            ) {
                val feedUrl = discoverFeedLink(body) // absolute-ized by caller when relative
                Result.success(
                    SiteInfo(
                        feedUrl = feedUrl?.let { absolutize(normalizeUrl(url), it) },
                        siteUrl = normalizeUrl(url),
                        title = Regex("(?is)<title[^>]*>(.*?)</title>").find(body)
                            ?.groupValues?.get(1)?.trim(),
                        iconUrl = discoverIcon(body)?.let { absolutize(normalizeUrl(url), it) }
                            ?: defaultFavicon(normalizeUrl(url)),
                    )
                )
            } else {
                // Probably already a feed
                Result.success(SiteInfo(feedUrl = normalizeUrl(url), siteUrl = null, title = null, iconUrl = null))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Finds <link rel="alternate" type="application/rss+xml|atom+xml"> */
    fun discoverFeedLink(html: String): String? {
        val linkRegex = Regex("(?is)<link[^>]+>")
        for (tag in linkRegex.findAll(html)) {
            val t = tag.value
            val type = Regex("type\\s*=\\s*[\"']([^\"']+)[\"']").find(t)?.groupValues?.get(1) ?: continue
            if (!type.contains("rss+xml", true) && !type.contains("atom+xml", true)) continue
            val rel = Regex("rel\\s*=\\s*[\"']([^\"']+)[\"']").find(t)?.groupValues?.get(1)
            if (rel != null && !rel.contains("alternate", true)) continue
            val href = Regex("href\\s*=\\s*[\"']([^\"']+)[\"']").find(t)?.groupValues?.get(1) ?: continue
            return href
        }
        return null
    }

    fun discoverIcon(html: String): String? {
        val linkRegex = Regex("(?is)<link[^>]+>")
        var fallback: String? = null
        for (tag in linkRegex.findAll(html)) {
            val t = tag.value
            if (!t.contains("rel", true)) continue
            val rel = Regex("rel\\s*=\\s*[\"']([^\"']+)[\"']").find(t)?.groupValues?.get(1) ?: continue
            val href = Regex("href\\s*=\\s*[\"']([^\"']+)[\"']").find(t)?.groupValues?.get(1) ?: continue
            val relLower = rel.lowercase()
            when {
                relLower.contains("apple-touch-icon") -> return href
                relLower.contains("icon") && fallback == null -> fallback = href
            }
        }
        return fallback
    }

    fun defaultFavicon(siteUrl: String): String = origin(siteUrl) + "/favicon.ico"

    fun appleTouchIcon(siteUrl: String): String = origin(siteUrl) + "/apple-touch-icon.png"

    fun normalizeUrl(url: String): String {
        val u = url.trim()
        return if (u.startsWith("http://") || u.startsWith("https://")) u else "https://$u"
    }

    fun origin(url: String): String = Regex("(https?://[^/]+)").find(url)?.value ?: url

    fun absolutize(base: String, href: String): String {
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        if (href.startsWith("//")) return "https:$href"
        if (href.startsWith("/")) return origin(base) + href
        return base.trimEnd('/') + "/" + href
    }
}
