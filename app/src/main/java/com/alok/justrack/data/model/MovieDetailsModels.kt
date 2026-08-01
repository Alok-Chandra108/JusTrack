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

data class MovieDetails(
    val id: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val rating: Double,
    val releaseDate: String,
    val runtime: String,
    val certification: String,
    val director: String,
    val cast: List<CastMember>,
    val ratings: List<RatingSource>,
    val recommendations: List<MediaItem>
)
