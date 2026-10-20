package com.intent.reader.sync

import com.intent.reader.data.db.Source
import com.intent.reader.rss.FeedParser
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP feed fetching with conditional GET (ETag / Last-Modified) to save traffic.
 */
class FeedFetcher(private val client: OkHttpClient) {

    sealed interface FetchResult {
        data class Ok(
            val feed: FeedParser.ParsedFeed,
            val etag: String?,
            val lastModified: String?,
        ) : FetchResult

        data object NotModified : FetchResult
        data class Failed(val message: String) : FetchResult
    }

    fun fetch(source: Source): FetchResult = try {
        val builder = Request.Builder()
            .url(source.feedUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
        source.etag?.let { builder.header("If-None-Match", it) }
        source.lastModified?.let { builder.header("If-Modified-Since", it) }
        val response = client.newCall(builder.build()).execute()
        response.use { resp ->
            when {
                resp.code == 304 -> FetchResult.NotModified
                !resp.isSuccessful -> FetchResult.Failed("HTTP ${resp.code}")
                else -> {
                    val body = resp.body ?: return FetchResult.Failed("empty body")
                    val etag = resp.header("ETag")
                    val lastModified = resp.header("Last-Modified")
                    when (val parsed = FeedParser.parse(body.charStream())) {
                        is FeedParser.ParseResult.Ok -> FetchResult.Ok(parsed.feed, etag, lastModified)
                        is FeedParser.ParseResult.Error -> FetchResult.Failed(parsed.message)
                    }
                }
            }
        }
    } catch (e: IOException) {
        FetchResult.Failed(e.message ?: "network error")
    } catch (e: Exception) {
        FetchResult.Failed(e.message ?: "unexpected error")
    }

    companion object {
        const val USER_AGENT = "INTENT/1.0 (Android RSS reader)"

        fun defaultClient(cacheDir: java.io.File?): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .cache(
                cacheDir?.let {
                    okhttp3.Cache(it, 20L * 1024 * 1024) // 20 MB HTTP cache
                }
            )
            .build()
    }
}
