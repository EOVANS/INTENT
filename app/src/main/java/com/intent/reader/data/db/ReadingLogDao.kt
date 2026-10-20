package com.intent.reader.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingLogDao {

    @Query("SELECT * FROM reading_logs WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<ReadingLog?>

    @Query("SELECT * FROM reading_logs WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): ReadingLog?

    @Upsert
    suspend fun upsert(log: ReadingLog)

    @Query("DELETE FROM reading_logs WHERE date < :cutoffDate")
    suspend fun trimBefore(cutoffDate: String)
}
