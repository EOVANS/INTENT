package com.intent.reader.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Daily reading minutes for Live Update progress and daily summary. */
@Entity(tableName = "reading_logs")
data class ReadingLog(
    @PrimaryKey val date: String,        // yyyy-MM-dd
    val minutes: Int,
)
