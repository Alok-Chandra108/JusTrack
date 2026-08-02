package com.alok.justrack.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAllFlow(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    suspend fun getAllOnce(): List<WatchlistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE id = :id LIMIT 1)")
    suspend fun exists(id: String): Boolean

    @Query("SELECT * FROM watchlist WHERE id = :id LIMIT 1")
    suspend fun getEntityById(id: String): WatchlistEntity?

    @Query("SELECT isWatched FROM watchlist WHERE id = :id")
    suspend fun getWatchedStatus(id: String): Boolean?

    @Query("UPDATE watchlist SET isWatched = :watched WHERE id = :id")
    suspend fun updateWatched(id: String, watched: Boolean)

    @Query("UPDATE watchlist SET customPosterPath = :posterPath WHERE id = :id")
    suspend fun updateCustomPoster(id: String, posterPath: String?)

    @Query("UPDATE watchlist SET customBackdropPath = :backdropPath WHERE id = :id")
    suspend fun updateCustomBackdrop(id: String, backdropPath: String?)

    @Query("SELECT customPosterPath FROM watchlist WHERE id = :id")
    suspend fun getCustomPoster(id: String): String?

    @Query("SELECT customBackdropPath FROM watchlist WHERE id = :id")
    suspend fun getCustomBackdrop(id: String): String?
}
