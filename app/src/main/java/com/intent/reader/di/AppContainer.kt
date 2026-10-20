package com.intent.reader.di

import android.content.Context
import com.intent.reader.data.db.AppDatabase
import com.intent.reader.data.prefs.SettingsRepository
import com.intent.reader.data.repo.QuoteRepository
import com.intent.reader.data.repo.SourceRepository
import com.intent.reader.sync.FeedFetcher
import okhttp3.OkHttpClient

/**
 * Lightweight manual DI container (Hilt optional per doc; manual keeps APK lean).
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.get(appContext) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val okHttpClient: OkHttpClient by lazy {
        FeedFetcher.defaultClient(
            java.io.File(appContext.cacheDir, "http_cache").apply { mkdirs() }
        )
    }

    val feedFetcher: FeedFetcher by lazy { FeedFetcher(okHttpClient) }

    val sourceRepository: SourceRepository by lazy {
        SourceRepository(appContext, database, okHttpClient)
    }

    val quoteRepository: QuoteRepository by lazy { QuoteRepository(database) }

    val readingTracker: com.intent.reader.data.repo.ReadingTracker by lazy {
        com.intent.reader.data.repo.ReadingTracker(database)
    }
}
