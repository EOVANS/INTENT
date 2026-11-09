package com.intent.reader.di

import android.content.Context
import com.intent.reader.data.db.AppDatabase
import com.intent.reader.data.prefs.SettingsRepository
import com.intent.reader.data.repo.QuoteRepository
import com.intent.reader.data.repo.SourceRepository
import com.intent.reader.sync.FeedFetcher
import com.intent.reader.sync.FeedSyncer
import okhttp3.OkHttpClient

/**
 * Lightweight manual DI container (Hilt optional per doc; manual keeps APK lean).
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val notesRepository by lazy {
        com.intent.reader.notes.NotesRepository(java.io.File(appContext.noBackupFilesDir, "island-notes"))
    }

    val aiConfigStore by lazy { com.intent.reader.ai.AiConfigStore(appContext) }
    val aiRepository by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false).build()
        com.intent.reader.ai.AiRepository(
            { aiConfigStore.config.value }, com.intent.reader.ai.AiClient(client),
            java.io.File(appContext.noBackupFilesDir, "ai-results"),
        )
    }

    val database: AppDatabase by lazy { AppDatabase.get(appContext) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val okHttpClient: OkHttpClient by lazy {
        FeedFetcher.defaultClient(
            java.io.File(appContext.cacheDir, "http_cache").apply { mkdirs() }
        )
    }

    val feedFetcher: FeedFetcher by lazy { FeedFetcher(okHttpClient) }

    val feedSyncer: FeedSyncer by lazy { FeedSyncer(database, feedFetcher) }

    val sourceRepository: SourceRepository by lazy {
        SourceRepository(appContext, database, okHttpClient, feedSyncer)
    }

    val quoteRepository: QuoteRepository by lazy { QuoteRepository(database) }

    val readingTracker: com.intent.reader.data.repo.ReadingTracker by lazy {
        com.intent.reader.data.repo.ReadingTracker(database)
    }
}
