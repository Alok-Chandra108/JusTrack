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
    @SerializedName("original_language") val originalLanguage: String?,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?,
    @SerializedName("credits") val credits: TmdbCreditsDto?,
    @SerializedName("release_dates") val releaseDates: TmdbReleaseDatesResponse?,
    @SerializedName("content_ratings") val contentRatings: TmdbContentRatingsResponse?,
    @SerializedName("created_by") val createdBy: List<TmdbCreatedByDto>?,
    @SerializedName("status") val status: String?,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int?,
    @SerializedName("seasons") val seasons: List<TmdbSeasonDto>?,
    @SerializedName("genres") val genres: List<TmdbGenreDto>?,
    @SerializedName("genre_ids") val genreIds: List<Int>?,
    @SerializedName("recommendations") val recommendations: TmdbPaginatedResponse<TmdbMediaDto>?,
    @SerializedName("watch/providers") val watchProviders: TmdbWatchProvidersResponse?
)

data class TmdbWatchProvidersResponse(
    @SerializedName("results") val results: Map<String, TmdbWatchProviderResult>?
)

data class TmdbWatchProviderResult(
    @SerializedName("link") val link: String?,
    @SerializedName("flatrate") val flatrate: List<TmdbWatchProviderItem>?,
    @SerializedName("rent") val rent: List<TmdbWatchProviderItem>?,
    @SerializedName("buy") val buy: List<TmdbWatchProviderItem>?
)

data class TmdbWatchProviderItem(
    @SerializedName("provider_id") val providerId: Int,
    @SerializedName("provider_name") val providerName: String,
    @SerializedName("logo_path") val logoPath: String?
)

data class TmdbSeasonDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("episodes") val episodes: List<TmdbEpisodeDto>?
)

data class TmdbEpisodeDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("overview") val overview: String?,
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("runtime") val runtime: Int?
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

data class TmdbImagesResponse(
    @SerializedName("posters") val posters: List<TmdbImageDto>,
    @SerializedName("backdrops") val backdrops: List<TmdbImageDto>
)

data class TmdbImageDto(
    @SerializedName("file_path") val filePath: String,
    @SerializedName("aspect_ratio") val aspectRatio: Double?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?
)

data class TmdbGenreResponse(
    @SerializedName("genres") val genres: List<TmdbGenreDto>
)

data class TmdbGenreDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class TmdbPersonDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("biography") val biography: String?,
    @SerializedName("birthday") val birthday: String?,
    @SerializedName("place_of_birth") val placeOfBirth: String?,
    @SerializedName("profile_path") val profilePath: String?,
    @SerializedName("known_for_department") val knownForDepartment: String?,
    @SerializedName("movie_credits") val movieCredits: TmdbPersonCreditsDto?,
    @SerializedName("tv_credits") val tvCredits: TmdbPersonCreditsDto?
)

data class TmdbPersonCreditsDto(
    @SerializedName("cast") val cast: List<TmdbMediaDto>?,
    @SerializedName("crew") val crew: List<TmdbMediaDto>?
)
