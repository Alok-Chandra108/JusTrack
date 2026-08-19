package com.alok.justrack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val customPosterPath: String? = null,
    val customBackdropPath: String? = null,
    val rating: Double,
    val releaseDate: String,
    val mediaType: String,
    val runtime: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val isWatched: Boolean = false,
    val inWatchlist: Boolean = false,
    val isWatchLater: Boolean = false,
    val userId: String? = null,
    val lastSyncAt: Long? = null
)
