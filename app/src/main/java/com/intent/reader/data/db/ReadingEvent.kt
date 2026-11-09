package com.intent.reader.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One minute of foreground reading attributed to a specific article. */
@Entity(tableName = "reading_events", indices = [Index("at")])
data class ReadingEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val articleId: String,
    val articleTitle: String,
    val sourceTitle: String,
    val at: Long,
    val minutes: Int = 1,
)
