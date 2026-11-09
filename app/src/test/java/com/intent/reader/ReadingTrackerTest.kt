package com.intent.reader

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.intent.reader.data.db.AppDatabase
import com.intent.reader.data.repo.ReadingTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ReadingTrackerTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val date = LocalDate.of(2026, 10, 26)
    private val noon = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After fun cleanup() {
        scope.cancel()
        db.close()
        context.deleteDatabase("reading-migration-test.db")
    }

    @Test fun shortReadsSurviveTrackerRecreation() = runBlocking {
        ReadingTracker(db, scope).addElapsed(25_500, noon)
        ReadingTracker(db, scope).addElapsed(35_500, noon + 60_000)
        val log = db.readingLogDao().getByDate(date.toString())!!
        assertEquals(1, log.minutes)
        assertEquals(61_000L, log.elapsedMillis)
    }

    @Test fun immediateExitFlushesOnceAndExcludesBackgroundTime() = runBlocking {
        var clock = 0L
        val tracker = ReadingTracker(db, scope, { clock }, { noon + clock })
        tracker.setReadingForeground(true)
        clock = 750
        tracker.setReadingForeground(false)
        tracker.setReadingForeground(false)
        clock = 30_000
        tracker.setReadingForeground(true)
        clock = 30_500
        tracker.setReadingForeground(false)
        val log = withTimeout(5_000) {
            db.readingLogDao().observeByDate(date.toString()).filterNotNull()
                .first { it.elapsedMillis == 1_250L }
        }
        assertEquals(0, log.minutes)
        assertEquals(1_250L, log.elapsedMillis)
    }

    @Test fun midnightSplitsTimeBetweenDays() = runBlocking {
        val midnight = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        ReadingTracker(db, scope).addElapsed(2_000, midnight + 500)
        assertEquals(1_500L, db.readingLogDao().getByDate(date.minusDays(1).toString())!!.elapsedMillis)
        assertEquals(500L, db.readingLogDao().getByDate(date.toString())!!.elapsedMillis)
    }

    @Test fun foregroundTickPublishesSecondsBeforeFirstMinute() = runBlocking {
        var clock = 0L
        val tracker = ReadingTracker(db, scope, { clock }, { noon + clock })
        tracker.setReadingForeground(true)
        clock = 1_000L
        val log = withTimeout(5_000) {
            tracker.observeToday().filterNotNull().first { it.elapsedMillis >= 1_000L }
        }
        tracker.setReadingForeground(false)
        assertEquals(0, log.minutes)
        assertEquals(1_000L, log.elapsedMillis)
    }

    @Test fun negativeIntervalsDoNotSubtractReadingTime() = runBlocking {
        val tracker = ReadingTracker(db, scope)
        tracker.addElapsed(5_000, noon)
        tracker.addElapsed(-3_000, noon)
        assertEquals(5_000L, db.readingLogDao().getByDate(date.toString())!!.elapsedMillis)
    }

    @Test fun completingGoalUpdatesTodayWithoutReducingExistingProgress() = runBlocking {
        val tracker = ReadingTracker(db, scope, wallTimeMillis = { noon })
        tracker.addElapsed(65_500, noon)
        tracker.completeTodayGoal(10)
        assertEquals(600_000L, db.readingLogDao().getByDate(date.toString())!!.elapsedMillis)
        tracker.addElapsed(2_000, noon)
        tracker.completeTodayGoal(5)
        val log = db.readingLogDao().getByDate(date.toString())!!
        assertEquals(602_000L, log.elapsedMillis)
        assertEquals(10, log.minutes)
    }

    @Test fun migrationPreservesExistingMinutesAndValidatesRoomSchema() = runBlocking {
        val name = "reading-migration-test.db"
        val seed = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        seed.openHelper.writableDatabase
        seed.close()
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("DROP TABLE reading_logs")
            it.execSQL("CREATE TABLE reading_logs (date TEXT NOT NULL PRIMARY KEY, minutes INTEGER NOT NULL)")
            it.execSQL("INSERT INTO reading_logs VALUES ('2026-10-26', 7)")
            it.version = 1
        }
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            val log = migrated.readingLogDao().getByDate(date.toString())!!
            assertEquals(7, log.minutes)
            assertEquals(420_000L, log.elapsedMillis)
        } finally {
            migrated.close()
        }
    }
}
