package com.intent.reader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Source::class,
        Article::class,
        ArticleFts::class,
        DailyQuote::class,
        ReadingLog::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun articleDao(): ArticleDao
    abstract fun dailyQuoteDao(): DailyQuoteDao
    abstract fun readingLogDao(): ReadingLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reading_logs ADD COLUMN elapsedMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE reading_logs SET elapsedMillis = minutes * 60000")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "intent.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
