package com.alok.justrack.data.model

data class CastMember(
    val id: String,
    val name: String,
    val character: String,
    val profilePath: String?
)

data class RatingSource(
    val label: String,
    val value: String
)

data class Person(
    val id: String,
    val name: String
)

data class WatchProvider(
    val id: Int,
    val name: String,
    val logoUrl: String
)

data class WatchProviders(
    val stream: List<WatchProvider> = emptyList(),
    val rent: List<WatchProvider> = emptyList(),
    val buy: List<WatchProvider> = emptyList()
)

data class MovieDetails(
    val id: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val rating: Double,
    val releaseDate: String,
    val rawReleaseDate: String = "",
    val runtime: String,
    val certification: String,
    val director: List<Person>,
    val originalLanguage: String = "",
    val status: String = "",
    val numberOfEpisodes: Int = 0,
    val mediaType: MediaType = MediaType.MOVIE,
    val cast: List<CastMember>,
    val ratings: List<RatingSource>,
    val recommendations: List<MediaItem>,
    val seasons: List<Season> = emptyList(),
    val watchProviders: WatchProviders? = null
)

data class Season(
    val id: String,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val seasonNumber: Int,
    val episodeCount: Int,
    val airDate: String?,
    val episodes: List<Episode> = emptyList(),
    val watchedCount: Int = 0
)

data class Episode(
    val id: String,
    val name: String,
    val overview: String,
    val stillPath: String?,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val airDate: String?,
    val voteAverage: Double,
    val runtime: Int = 0,
    val isWatched: Boolean = false
)

data class ShowProgress(
    val percentage: Int,
    val color: androidx.compose.ui.graphics.Color,
    val label: String,
    val totalReleased: Int,
    val watched: Int
)
