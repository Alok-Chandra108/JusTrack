package com.alok.justrack.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("3/trending/all/day")
    suspend fun getTrending(): TmdbPaginatedResponse<TmdbMediaDto>

    @GET("3/search/multi")
    suspend fun searchMulti(
        @Query("query") query: String
    ): TmdbPaginatedResponse<TmdbMediaDto>

    @GET("3/movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") id: String,
        @Query("append_to_response") appendToResponse: String = "credits,release_dates,recommendations"
    ): TmdbMediaDto

    @GET("3/tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") id: String,
        @Query("append_to_response") appendToResponse: String = "credits,content_ratings,recommendations"
    ): TmdbMediaDto

    @GET("3/tv/{tv_id}/season/{season_number}")
    suspend fun getTvSeasonDetails(
        @Path("tv_id") tvId: String,
        @Path("season_number") seasonNumber: Int
    ): TmdbSeasonDto

    @GET("3/movie/{movie_id}/images")
    suspend fun getMovieImages(@Path("movie_id") id: String): TmdbImagesResponse

    @GET("3/tv/{tv_id}/images")
    suspend fun getTvImages(@Path("tv_id") id: String): TmdbImagesResponse

    @GET("3/genre/movie/list")
    suspend fun getMovieGenres(): TmdbGenreResponse

    @GET("3/genre/tv/list")
    suspend fun getTvGenres(): TmdbGenreResponse

    @GET("3/movie/popular")
    suspend fun getPopularMovies(): TmdbPaginatedResponse<TmdbMediaDto>

    @GET("3/tv/popular")
    suspend fun getPopularTv(): TmdbPaginatedResponse<TmdbMediaDto>

    @GET("3/movie/top_rated")
    suspend fun getTopRatedMovies(): TmdbPaginatedResponse<TmdbMediaDto>

    @GET("3/tv/top_rated")
    suspend fun getTopRatedTv(): TmdbPaginatedResponse<TmdbMediaDto>

    @GET("3/movie/upcoming")
    suspend fun getUpcomingMovies(): TmdbPaginatedResponse<TmdbMediaDto>

    @GET("3/tv/on_the_air")
    suspend fun getOnTheAirTv(): TmdbPaginatedResponse<TmdbMediaDto>

    @GET("3/discover/movie")
    suspend fun discoverMovies(
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("primary_release_date.gte") releaseDateGte: String? = null,
        @Query("primary_release_date.lte") releaseDateLte: String? = null,
        @Query("with_release_type") releaseType: String? = null,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("region") region: String? = null,
        @Query("with_original_language") originalLanguage: String? = null
    ): TmdbPaginatedResponse<TmdbMediaDto>

    @GET("3/discover/movie")
    suspend fun discoverMoviesByGenre(
        @Query("with_genres") genreId: Int
    ): TmdbPaginatedResponse<TmdbMediaDto>

    @GET("3/discover/tv")
    suspend fun discoverTvByGenre(
        @Query("with_genres") genreId: Int
    ): TmdbPaginatedResponse<TmdbMediaDto>
}
