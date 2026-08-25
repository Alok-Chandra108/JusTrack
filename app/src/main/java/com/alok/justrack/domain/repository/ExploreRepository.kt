package com.alok.justrack.domain.repository

import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.ui.viewmodel.Genre

interface ExploreRepository {
    suspend fun getTrending(): List<MediaItem>
    suspend fun getTrendingIndia(): List<MediaItem>
    suspend fun getGenres(): List<Genre>
    suspend fun getPopularMovies(): List<MediaItem>
    suspend fun getPopularTv(): List<MediaItem>
    suspend fun getTopRatedMovies(): List<MediaItem>
    suspend fun getTopRatedTv(): List<MediaItem>
    suspend fun getUpcomingMovies(): List<MediaItem>
    suspend fun getOnTheAirTv(): List<MediaItem>
    suspend fun discoverByGenre(genreId: Int): List<MediaItem>
    
    // Search specific methods used by the UseCase
    suspend fun searchMulti(query: String, region: String? = null): List<MediaItem>
    suspend fun searchMovie(query: String, year: Int?): List<MediaItem>
    suspend fun searchTv(query: String, year: Int?): List<MediaItem>
}
