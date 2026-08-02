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
    val addedAt: Long = System.currentTimeMillis(),
    val isWatched: Boolean = false,
    val inWatchlist: Boolean = false
)
