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
    val inWatchlist: Boolean = false,
    val isWatchLater: Boolean = false,
    val runtime: Int = 0,
    val genreIds: List<Int> = emptyList(),
    val addedAt: Long = System.currentTimeMillis()
)

enum class MediaType {
    MOVIE, TV
}
