package com.alok.justrack.data.repository

import com.alok.justrack.data.api.TmdbApiService
import com.alok.justrack.data.mapper.TmdbMapper.toMediaItem
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.domain.repository.ExploreRepository
import com.alok.justrack.ui.viewmodel.Genre
import com.alok.justrack.util.DateUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbExploreRepository @Inject constructor(
    private val apiService: TmdbApiService
) : ExploreRepository {

    override suspend fun getTrending(): List<MediaItem> {
        return try {
            val response = apiService.getTrending()
            response.results.map { it.toMediaItem() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getTrendingIndia(): List<MediaItem> {
        return try {
            val movies = apiService.discoverMovies(region = "IN", sortBy = "popularity.desc").results
            movies.map { it.toMediaItem(MediaType.MOVIE) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getGenres(): List<Genre> {
        return try {
            val movieGenres = apiService.getMovieGenres().genres
            val tvGenres = apiService.getTvGenres().genres
            val allGenres = (movieGenres + tvGenres).distinctBy { it.id }.sortedBy { it.name }
            allGenres.map { Genre(it.id, it.name) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getPopularMovies(): List<MediaItem> {
        return try {
            apiService.getPopularMovies().results.map { it.toMediaItem(MediaType.MOVIE) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPopularTv(): List<MediaItem> {
        return try {
            apiService.getPopularTv().results.map { it.toMediaItem(MediaType.TV) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTopRatedMovies(): List<MediaItem> {
        return try {
            apiService.getTopRatedMovies().results.map { it.toMediaItem(MediaType.MOVIE) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTopRatedTv(): List<MediaItem> {
        return try {
            apiService.getTopRatedTv().results.map { it.toMediaItem(MediaType.TV) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getUpcomingMovies(): List<MediaItem> = coroutineScope {
        val today = DateUtils.getTodayIST()
        val tomorrow = today.plusDays(1).toString()

        try {
            // Fetch Global Hyped/Buzzing Upcoming
            val globalDeferred = async {
                apiService.discoverMovies(
                    sortBy = "popularity.desc",
                    releaseDateGte = tomorrow,
                    includeAdult = false
                ).results.map { it.toMediaItem(MediaType.MOVIE) }
            }

            // Fetch Indian Hyped/Buzzing Upcoming
            val indianLanguages = "hi|te|ta|ml|kn"
            val indianDeferred = async {
                apiService.discoverMovies(
                    sortBy = "popularity.desc",
                    releaseDateGte = tomorrow,
                    includeAdult = false,
                    region = "IN",
                    originalLanguage = indianLanguages
                ).results.map { it.toMediaItem(MediaType.MOVIE) }
            }

            val globalResults = try { globalDeferred.await() } catch (e: Exception) { emptyList() }
            val indianResults = try { indianDeferred.await() } catch (e: Exception) { emptyList() }

            (globalResults + indianResults)
                .distinctBy { it.id }
                .filter { item ->
                    val releaseDate = DateUtils.parseDate(item.releaseDate)
                    releaseDate != null && releaseDate.isAfter(today)
                }
                .sortedBy { DateUtils.parseDate(it.releaseDate) }
        } catch (e: Exception) {
            try {
                // Fallback to standard upcoming if discover fails
                apiService.getUpcomingMovies().results
                    .map { it.toMediaItem(MediaType.MOVIE) }
                    .filter { item ->
                        val releaseDate = DateUtils.parseDate(item.releaseDate)
                        releaseDate != null && releaseDate.isAfter(today)
                    }
                    .sortedBy { DateUtils.parseDate(it.releaseDate) }
            } catch (ex: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getOnTheAirTv(): List<MediaItem> {
        return try {
            apiService.getOnTheAirTv().results.map { it.toMediaItem(MediaType.TV) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun discoverByGenre(genreId: Int): List<MediaItem> {
        return try {
            val movieResults = apiService.discoverMoviesByGenre(genreId).results
            val tvResults = apiService.discoverTvByGenre(genreId).results
            (movieResults + tvResults)
                .map { it.toMediaItem() }
                .sortedByDescending { it.rating }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun searchMulti(query: String, region: String?): List<MediaItem> {
        return try {
            apiService.searchMulti(query = query, includeAdult = false, region = region)
                .results.map { it.toMediaItem() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun searchMovie(query: String, year: Int?): List<MediaItem> {
        return try {
            apiService.searchMovie(query = query, year = year, includeAdult = false)
                .results.map { it.toMediaItem(MediaType.MOVIE) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun searchTv(query: String, year: Int?): List<MediaItem> {
        return try {
            apiService.searchTv(query = query, year = year, includeAdult = false)
                .results.map { it.toMediaItem(MediaType.TV) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
