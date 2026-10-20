package com.intent.reader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {

    @Query("SELECT * FROM sources ORDER BY sortOrder ASC, addedAt ASC")
    fun observeAll(): Flow<List<Source>>

    @Query("SELECT * FROM sources ORDER BY sortOrder ASC, addedAt ASC")
    suspend fun getAll(): List<Source>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getById(id: String): Source?

    @Query("SELECT * FROM sources WHERE feedUrl = :feedUrl LIMIT 1")
    suspend fun getByFeedUrl(feedUrl: String): Source?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: Source): Long

    @Update
    suspend fun update(source: Source)

    @Query("UPDATE sources SET title = :title WHERE id = :id")
    suspend fun rename(id: String, title: String)

    @Query("UPDATE sources SET notifyEnabled = :enabled WHERE id = :id")
    suspend fun setNotifyEnabled(id: String, enabled: Boolean)

    @Query("UPDATE sources SET fetchIntervalMin = :minutes WHERE id = :id")
    suspend fun setFetchInterval(id: String, minutes: Int)

    @Query("UPDATE sources SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: String, order: Int)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM sources")
    suspend fun count(): Int
}
