package com.alok.justrack.data.db

import androidx.room.*

@Dao
interface CustomImageDao {

    @Query("SELECT customPosterPath FROM custom_images WHERE mediaId = :mediaId")
    suspend fun getPoster(mediaId: String): String?

    @Query("SELECT customBackdropPath FROM custom_images WHERE mediaId = :mediaId")
    suspend fun getBackdrop(mediaId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomImageEntity)

    @Query("UPDATE custom_images SET customPosterPath = :url WHERE mediaId = :mediaId")
    suspend fun updatePoster(mediaId: String, url: String?)

    @Query("UPDATE custom_images SET customBackdropPath = :url WHERE mediaId = :mediaId")
    suspend fun updateBackdrop(mediaId: String, url: String?)

    @Query("SELECT EXISTS(SELECT 1 FROM custom_images WHERE mediaId = :mediaId LIMIT 1)")
    suspend fun exists(mediaId: String): Boolean
}
