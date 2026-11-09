package com.intent.reader.data.db

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

/** Daily reading minutes for Live Update progress and daily summary. */
@Entity(tableName = "reading_logs")
data class ReadingLog(
    @PrimaryKey val date: String,        // yyyy-MM-dd
    val minutes: Int,
    @ColumnInfo(defaultValue = "0") val elapsedMillis: Long = minutes * 60_000L,
)
