package com.intent.reader.data.repo

import android.os.SystemClock
import com.intent.reader.data.db.AppDatabase
import com.intent.reader.data.db.ReadingEvent
import com.intent.reader.data.db.ReadingLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Article context used to attribute accumulated reading time in the privacy history. */
data class ReadingArticle(val id: String, val title: String, val sourceTitle: String)

/** Application-owned timer: navigation cannot cancel the final database write. */
class ReadingTracker(
    private val db: AppDatabase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
    private val wallTimeMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val mutex = Mutex()
    private var sessionStart: Long? = null
    private var timerJob: Job? = null
    private var activeArticle: ReadingArticle? = null
    private var pendingArticleMillis = 0L

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeToday(): Flow<ReadingLog?> = flow {
        while (true) {
            emit(Instant.ofEpochMilli(wallTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDate().toString())
            delay(1_000)
        }
    }.distinctUntilChanged().flatMapLatest { db.readingLogDao().observeByDate(it) }

    /** Call on the main thread with the reader destination's resumed state. */
    fun setReadingForeground(active: Boolean) {
        if (active == (sessionStart != null)) return
        if (active) {
            sessionStart = elapsedRealtime()
            timerJob = scope.launch {
                while (true) {
                    delay(1_000)
                    checkpoint()
                }
            }
        } else {
            timerJob?.cancel()
            timerJob = null
            checkpoint()
            sessionStart = null
        }
    }

    /** Registers the article on screen so reading minutes can be attributed to it. */
    fun setReadingArticle(article: ReadingArticle?) {
        if (activeArticle?.id != article?.id) pendingArticleMillis = 0L
        activeArticle = article
    }

    private fun checkpoint() {
        val start = sessionStart ?: return
        val now = elapsedRealtime()
        sessionStart = now
        val elapsed = (now - start).coerceAtLeast(0L)
        val endedAt = wallTimeMillis()
        if (elapsed > 0) {
            scope.launch { addElapsed(elapsed, endedAt) }
            recordArticleReading(elapsed, endedAt)
        }
    }

    /** Writes one history row per full minute of foreground reading. */
    private fun recordArticleReading(elapsed: Long, endedAt: Long) {
        val article = activeArticle ?: return
        pendingArticleMillis += elapsed
        val minutes = (pendingArticleMillis / 60_000L).toInt()
        if (minutes < 1) return
        pendingArticleMillis -= minutes * 60_000L
        scope.launch {
            val dao = db.readingEventDao()
            dao.insert(ReadingEvent(
                articleId = article.id,
                articleTitle = article.title,
                sourceTitle = article.sourceTitle,
                at = endedAt,
                minutes = minutes,
            ))
            dao.trimBefore(endedAt - 60L * 24 * 60 * 60 * 1000)
        }
    }

    /** Raise today's recorded time to the goal without reducing existing progress. */
    suspend fun completeTodayGoal(goalMinutes: Int) = mutex.withLock {
        val date = Instant.ofEpochMilli(wallTimeMillis()).atZone(ZoneId.systemDefault())
            .toLocalDate().toString()
        val dao = db.readingLogDao()
        val total = maxOf(dao.getByDate(date)?.elapsedMillis ?: 0L, goalMinutes.coerceAtLeast(0) * 60_000L)
        dao.upsert(ReadingLog(date, (total / 60_000).toInt(), total))
    }

    /** Persist sub-minute reads too, splitting a midnight crossing by local date. */
    suspend fun addElapsed(elapsedMillis: Long, endedAtMillis: Long = wallTimeMillis()): Int = mutex.withLock {
        val dao = db.readingLogDao()
        val zone = ZoneId.systemDefault()
        var cursor = endedAtMillis - elapsedMillis.coerceAtLeast(0L)
        while (cursor < endedAtMillis) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val boundary = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = minOf(endedAtMillis, boundary)
            val key = date.toString()
            val total = (dao.getByDate(key)?.elapsedMillis ?: 0L) + end - cursor
            dao.upsert(ReadingLog(key, (total / 60_000).toInt(), total))
            cursor = end
        }
        val today = Instant.ofEpochMilli(endedAtMillis).atZone(zone).toLocalDate()
        dao.trimBefore(today.minusDays(60).toString())
        dao.getByDate(today.toString())?.minutes ?: 0
    }

    companion object {
        fun todayKey(): String = LocalDate.now().toString()
    }
}
