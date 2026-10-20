package com.intent.reader.data.db

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    indices = [Index("sourceId"), Index("publishedAt"), Index(value = ["guid"], unique = true)]
)
data class Article(
    @PrimaryKey val id: String,            // guid hash
    val sourceId: String,
    val guid: String,          // raw guid or link+title hash
    val title: String,
    val link: String,
    val author: String?,
    val summary: String?,      // plain-text summary
    val contentHtml: String?,
    val imageUrl: String?,
    val publishedAt: Long,
    val fetchedAt: Long,
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val cardStyle: String?     // A/B/C/D/E, persisted on first computation for stable UI
)

@Fts4(contentEntity = Article::class)
@Entity(tableName = "articles_fts")
data class ArticleFts(
    val title: String,
    val summary: String?,
    val contentHtml: String?,
)
