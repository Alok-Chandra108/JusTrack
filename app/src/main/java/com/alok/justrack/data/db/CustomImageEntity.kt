package com.alok.justrack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "custom_images")
data class CustomImageEntity(
    @PrimaryKey val mediaId: String,
    val mediaType: String,
    val customPosterPath: String? = null,
    val customBackdropPath: String? = null,
    val userId: String? = null,
    val lastSyncAt: Long? = null
)
