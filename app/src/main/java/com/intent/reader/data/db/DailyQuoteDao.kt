package com.intent.reader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyQuoteDao {

    @Query("SELECT * FROM daily_quotes WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<DailyQuote?>

    @Query("SELECT * FROM daily_quotes WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyQuote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(quote: DailyQuote)

    @Query("DELETE FROM daily_quotes WHERE date < :cutoffDate")
    suspend fun trimBefore(cutoffDate: String)
}
