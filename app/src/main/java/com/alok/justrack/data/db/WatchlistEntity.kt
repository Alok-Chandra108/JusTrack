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
    val rating: Double,
    val releaseDate: String,
    val mediaType: String, // Store enum name ("MOVIE" or "TV")
    val addedAt: Long = System.currentTimeMillis()
)
