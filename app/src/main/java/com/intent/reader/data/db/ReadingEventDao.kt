package com.intent.reader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingEventDao {
    @Insert
    suspend fun insert(event: ReadingEvent)

    @Query("SELECT * FROM reading_events ORDER BY at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ReadingEvent>>

    @Query("SELECT COUNT(*) FROM reading_events")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM reading_events")
    suspend fun clear()

    @Query("DELETE FROM reading_events WHERE at < :cutoff")
    suspend fun trimBefore(cutoff: Long)
}
