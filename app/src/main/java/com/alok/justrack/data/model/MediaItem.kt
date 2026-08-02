package com.alok.justrack.data.model

data class MediaItem(
    val id: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val rating: Double,
    val releaseDate: String,
    val mediaType: MediaType = MediaType.MOVIE,
    val isWatched: Boolean = false,
    val inWatchlist: Boolean = false
)

enum class MediaType {
    MOVIE, TV
}
