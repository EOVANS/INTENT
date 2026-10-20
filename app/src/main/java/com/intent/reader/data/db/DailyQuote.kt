package com.intent.reader.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_quotes")
data class DailyQuote(
    @PrimaryKey val date: String,            // yyyy-MM-dd
    val text: String,
    val sourceId: String?,
    val articleId: String?,
    val highlights: String?,     // JSON: [{"start":..,"end":..}] character offsets for red underline
    val sourceTitle: String?,    // source name for the meta line
    val articleSummary: String?, // source article summary for the meta line
)
