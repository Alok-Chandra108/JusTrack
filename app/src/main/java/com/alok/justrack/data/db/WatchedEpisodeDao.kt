package com.alok.justrack.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedEpisodeDao {

    @Query("SELECT * FROM watched_episodes WHERE showId = :showId")
    fun getWatchedEpisodesForShow(showId: String): Flow<List<WatchedEpisodeEntity>>

    @Query("SELECT * FROM watched_episodes WHERE showId = :showId")
    suspend fun getWatchedEpisodesForShowOnce(showId: String): List<WatchedEpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WatchedEpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<WatchedEpisodeEntity>)

    @Query("DELETE FROM watched_episodes WHERE showId = :showId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber")
    suspend fun delete(showId: String, seasonNumber: Int, episodeNumber: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM watched_episodes WHERE showId = :showId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber LIMIT 1)")
    suspend fun isWatched(showId: String, seasonNumber: Int, episodeNumber: Int): Boolean

    @Query("SELECT COUNT(*) FROM watched_episodes WHERE showId = :showId AND seasonNumber = :seasonNumber")
    suspend fun getWatchedCountForSeason(showId: String, seasonNumber: Int): Int
    
    @Query("SELECT * FROM watched_episodes")
    fun getAllWatchedEpisodesFlow(): Flow<List<WatchedEpisodeEntity>>
}
