package com.intent.reader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<Article>): List<Long>

    @Query("SELECT * FROM articles ORDER BY publishedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 800): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getById(id: String): Article?

    @Query("SELECT * FROM articles WHERE id = :id")
    fun observeById(id: String): Flow<Article?>

    @Query("SELECT * FROM articles WHERE sourceId = :sourceId ORDER BY publishedAt DESC")
    fun observeBySource(sourceId: String): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE sourceId = :sourceId AND publishedAt >= :since ORDER BY publishedAt DESC")
    suspend fun getBySourceSince(sourceId: String, since: Long): List<Article>

    @Query("SELECT * FROM articles WHERE publishedAt >= :since OR fetchedAt >= :since ORDER BY publishedAt DESC")
    suspend fun getSince(since: Long): List<Article>

    @Query(
        """
        SELECT a.* FROM articles a
        JOIN articles_fts f ON a.rowid = f.rowid
        WHERE articles_fts MATCH :query
        ORDER BY a.publishedAt DESC LIMIT 200
        """
    )
    suspend fun searchFts(query: String): List<Article>

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :q || '%' OR summary LIKE '%' || :q || '%' ORDER BY publishedAt DESC LIMIT 200")
    suspend fun searchLike(q: String): List<Article>

    @Query("UPDATE articles SET isRead = :isRead WHERE id = :id")
    suspend fun setRead(id: String, isRead: Boolean)

    @Query("UPDATE articles SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: String, fav: Boolean)

    @Query("SELECT COUNT(*) FROM articles WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT sourceId, COUNT(*) as cnt FROM articles WHERE isRead = 0 GROUP BY sourceId")
    suspend fun unreadBySource(): List<UnreadCount>

    @Query("SELECT sourceId, COUNT(*) as cnt FROM articles WHERE publishedAt >= :dayStart GROUP BY sourceId")
    suspend fun countBySourceSince(dayStart: Long): List<SourceDayCount>

    @Query("UPDATE articles SET cardStyle = :style WHERE id = :id")
    suspend fun setCardStyle(id: String, style: String)

    @Query("SELECT DISTINCT sourceId FROM articles")
    suspend fun distinctSourceIds(): List<String>

    @Query(
        """
        DELETE FROM articles WHERE id IN (
            SELECT id FROM articles
            WHERE sourceId = :sourceId AND id NOT IN (
                SELECT id FROM articles WHERE sourceId = :sourceId
                ORDER BY publishedAt DESC LIMIT :keep
            )
        )
        """
    )
    suspend fun trimSourceKeep(sourceId: String, keep: Int)

    @Query("DELETE FROM articles WHERE publishedAt < :cutoff AND isFavorite = 0")
    suspend fun trimOlderThan(cutoff: Long)

    @Query("DELETE FROM articles WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun count(): Int
}

data class UnreadCount(val sourceId: String, val cnt: Int)
data class SourceDayCount(val sourceId: String, val cnt: Int)
