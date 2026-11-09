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
        ReadingEvent::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun articleDao(): ArticleDao
    abstract fun dailyQuoteDao(): DailyQuoteDao
    abstract fun readingLogDao(): ReadingLogDao
    abstract fun readingEventDao(): ReadingEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reading_logs ADD COLUMN elapsedMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE reading_logs SET elapsedMillis = minutes * 60000")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reading_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`articleId` TEXT NOT NULL, `articleTitle` TEXT NOT NULL, `sourceTitle` TEXT NOT NULL, " +
                        "`at` INTEGER NOT NULL, `minutes` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_events_at` ON `reading_events` (`at`)")
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
