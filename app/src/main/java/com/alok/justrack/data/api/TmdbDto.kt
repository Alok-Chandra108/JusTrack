package com.alok.justrack.data.api

import com.google.gson.annotations.SerializedName

data class TmdbPaginatedResponse<T>(
    @SerializedName("page") val page: Int,
    @SerializedName("results") val results: List<T>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class TmdbMediaDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?,
    @SerializedName("credits") val credits: TmdbCreditsDto?,
    @SerializedName("release_dates") val releaseDates: TmdbReleaseDatesResponse?,
    @SerializedName("content_ratings") val contentRatings: TmdbContentRatingsResponse?,
    @SerializedName("created_by") val createdBy: List<TmdbCreatedByDto>?,
    @SerializedName("recommendations") val recommendations: TmdbPaginatedResponse<TmdbMediaDto>?
)

data class TmdbReleaseDatesResponse(
    @SerializedName("results") val results: List<TmdbReleaseDateResult>?
)

data class TmdbReleaseDateResult(
    @SerializedName("iso_3166_1") val iso31661: String,
    @SerializedName("release_dates") val releaseDates: List<TmdbReleaseDateItem>
)

data class TmdbReleaseDateItem(
    @SerializedName("certification") val certification: String
)

data class TmdbContentRatingsResponse(
    @SerializedName("results") val results: List<TmdbContentRatingResult>?
)

data class TmdbContentRatingResult(
    @SerializedName("iso_3166_1") val iso31661: String,
    @SerializedName("rating") val rating: String
)

data class TmdbCreatedByDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)

data class TmdbCreditsDto(
    @SerializedName("cast") val cast: List<TmdbCastDto>?,
    @SerializedName("crew") val crew: List<TmdbCrewDto>?
)

data class TmdbCastDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("character") val character: String,
    @SerializedName("profile_path") val profilePath: String?
)

data class TmdbCrewDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("job") val job: String
)
