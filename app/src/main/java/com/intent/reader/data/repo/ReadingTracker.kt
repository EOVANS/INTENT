package com.intent.reader.data.repo

import com.intent.reader.data.db.AppDatabase
import com.intent.reader.data.db.ReadingLog
import com.intent.reader.notify.LiveUpdate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Tracks daily foreground reading minutes (persisted every minute) and
 * drives the Live Update reading-progress notification.
 */
class ReadingTracker(private val db: AppDatabase) {

    fun observeToday(): Flow<ReadingLog?> =
        db.readingLogDao().observeByDate(todayKey())

    /** Called by the reader screen once per foreground minute. Returns new total. */
    suspend fun addMinute(goalMinutes: Int): Int {
        val date = todayKey()
        val current = db.readingLogDao().getByDate(date)?.minutes ?: 0
        val updated = current + 1
        db.readingLogDao().upsert(ReadingLog(date, updated))
        db.readingLogDao().trimBefore(LocalDate.now().minusDays(60).toString())
        return updated
    }

    suspend fun todayMinutes(): Int =
        db.readingLogDao().getByDate(todayKey())?.minutes ?: 0

    companion object {
        fun todayKey(): String = LocalDate.now().toString()
    }
}
