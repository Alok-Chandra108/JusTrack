package com.alok.justrack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val id: String,
    val mediaId: String,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val addedAt: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val lastSyncAt: Long? = null
)
