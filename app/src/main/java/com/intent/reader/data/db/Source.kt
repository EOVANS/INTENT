package com.intent.reader.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sources", indices = [Index(value = ["feedUrl"], unique = true)])
data class Source(
    @PrimaryKey val id: String,            // feedUrl hash
    val title: String,
    val feedUrl: String,
    val siteUrl: String?,
    val logoUrl: String?,
    val category: String?,
    val addedAt: Long,
    val sortOrder: Int,
    val notifyEnabled: Boolean = true,
    val fetchIntervalMin: Int = 360,
    val isBuiltIn: Boolean = false,
    // conditional GET caching
    val etag: String? = null,
    val lastModified: String? = null,
    val lastFetchedAt: Long = 0L,
    val lastError: String? = null,
)
