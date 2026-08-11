package com.alok.justrack.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing and manipulating episode data.
 */
@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episode_entity WHERE showId = :showId ORDER BY seasonNumber, episodeNumber")
    fun getEpisodesForShow(showId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episode_entity WHERE showId = :showId ORDER BY seasonNumber, episodeNumber")
    suspend fun getEpisodesForShowOnce(showId: String): List<EpisodeEntity>

    @Query("SELECT * FROM episode_entity WHERE showId = :showId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber LIMIT 1")
    suspend fun getEpisode(showId: String, seasonNumber: Int, episodeNumber: Int): EpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EpisodeEntity>)

    @Query("DELETE FROM episode_entity WHERE showId = :showId")
    suspend fun deleteAllForShow(showId: String)

    @Query("DELETE FROM episode_entity WHERE showId = :showId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber")
    suspend fun delete(showId: String, seasonNumber: Int, episodeNumber: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM episode_entity WHERE showId = :showId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber LIMIT 1)")
    suspend fun exists(showId: String, seasonNumber: Int, episodeNumber: Int): Boolean

    // For tracking progress: get the next unwatched episode for a show
    // We assume that an episode is considered watched if it exists in the watched_episodes table.
    @Query("""
        SELECT e.* FROM episode_entity e
        WHERE e.showId = :showId
          AND e.seasonNumber > 0
          AND NOT EXISTS (
            SELECT 1 FROM watched_episodes we
            WHERE we.showId = :showId AND we.seasonNumber = e.seasonNumber AND we.episodeNumber = e.episodeNumber
          )
        ORDER BY e.seasonNumber, e.episodeNumber
        LIMIT 1
    """)
    suspend fun getNextUnwatchedEpisode(showId: String): EpisodeEntity?

    // Get the count of watched episodes for a show (excluding specials)
    @Query("""
        SELECT COUNT(*) FROM watched_episodes we
        WHERE we.showId = :showId AND we.seasonNumber > 0
    """)
    suspend fun getWatchedEpisodeCount(showId: String): Int

    // Get the total number of episodes for a show (excluding specials)
    @Query("""
        SELECT COUNT(*) FROM episode_entity ee
        WHERE ee.showId = :showId AND ee.seasonNumber > 0
    """)
    suspend fun getTotalEpisodeCount(showId: String): Int

    @Query("""
        SELECT COUNT(*) FROM episode_entity ee
        WHERE ee.showId = :showId AND ee.seasonNumber > 0
          AND (ee.airDate IS NULL OR ee.airDate <= :today)
    """)
    suspend fun getReleasedEpisodeCount(showId: String, today: String): Int

    @Query("""
        SELECT e.* FROM episode_entity e
        WHERE e.showId = :showId
          AND e.seasonNumber > 0
          AND (e.airDate IS NULL OR e.airDate <= :today)
          AND NOT EXISTS (
            SELECT 1 FROM watched_episodes we
            WHERE we.showId = :showId AND we.seasonNumber = e.seasonNumber AND we.episodeNumber = e.episodeNumber
          )
        ORDER BY e.seasonNumber, e.episodeNumber
        LIMIT 1
    """)
    suspend fun getNextReleasedUnwatchedEpisode(showId: String, today: String): EpisodeEntity?

    @Query("SELECT MAX(episodeNumber) FROM episode_entity WHERE showId = :showId AND seasonNumber = :seasonNumber")
    suspend fun getMaxEpisodeNumberForSeason(showId: String, seasonNumber: Int): Int?

    @Query("SELECT * FROM episode_entity WHERE showId = :showId AND airDate >= :today ORDER BY airDate ASC")
    suspend fun getFutureEpisodes(showId: String, today: String): List<EpisodeEntity>
}
