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
}
