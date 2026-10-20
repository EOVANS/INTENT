package com.intent.reader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Source::class,
        Article::class,
        ArticleFts::class,
        DailyQuote::class,
        ReadingLog::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun articleDao(): ArticleDao
    abstract fun dailyQuoteDao(): DailyQuoteDao
    abstract fun readingLogDao(): ReadingLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "intent.db"
                ).build().also { instance = it }
            }
    }
}
