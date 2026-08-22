package com.alok.justrack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val overview: String,
    @SerialName("posterpath") val posterPath: String?,
    @SerialName("backdroppath") val backdropPath: String?,
    @SerialName("customposterpath") val customPosterPath: String? = null,
    @SerialName("custombackdroppath") val customBackdropPath: String? = null,
    val rating: Double,
    @SerialName("releasedate") val releaseDate: String,
    @SerialName("mediatype") val mediaType: String,
    val runtime: Int = 0,
    @SerialName("addedat") val addedAt: Long = System.currentTimeMillis(),
    @SerialName("iswatched") val isWatched: Boolean = false,
    @SerialName("inwatchlist") val inWatchlist: Boolean = false,
    @SerialName("iswatchlater") val isWatchLater: Boolean = false,
    @SerialName("userid") val userId: String? = null,
    @SerialName("lastsyncat") val lastSyncAt: Long? = null
)
