package com.alok.justrack.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {

    @Query("SELECT * FROM favourites ORDER BY addedAt DESC")
    fun getAllFlow(): Flow<List<FavouriteEntity>>

    @Query("SELECT * FROM favourites WHERE mediaType = :mediaType ORDER BY addedAt DESC")
    fun getByTypeFlow(mediaType: String): Flow<List<FavouriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE mediaId = :mediaId AND mediaType = :mediaType")
    suspend fun delete(mediaId: String, mediaType: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE mediaId = :mediaId AND mediaType = :mediaType LIMIT 1)")
    suspend fun exists(mediaId: String, mediaType: String): Boolean

    @Query("SELECT * FROM favourites WHERE userId IS NULL OR lastSyncAt IS NULL")
    suspend fun getUnsynced(): List<FavouriteEntity>

    @Query("UPDATE favourites SET userId = :userId, lastSyncAt = :lastSyncAt WHERE id IN (:ids)")
    suspend fun updateSyncStatus(ids: List<String>, userId: String, lastSyncAt: Long)

    @Query("UPDATE favourites SET userId = :userId WHERE userId IS NULL")
    suspend fun claimLocalData(userId: String)
}
