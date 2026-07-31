package com.alok.justrack.data.repository

import com.alok.justrack.data.api.TmdbApiService
import com.alok.justrack.data.api.TmdbMediaDto
import com.alok.justrack.data.db.WatchlistDao
import com.alok.justrack.data.db.WatchlistEntity
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbMediaRepository @Inject constructor(
    private val apiService: TmdbApiService,
    private val watchlistDao: WatchlistDao
) : MediaRepository {

    override suspend fun getTrending(): List<MediaItem> {
        return try {
            val response = apiService.getTrending()
            response.results.map { it.toMediaItem() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getWatchlist(): List<MediaItem> {
        return getTrending().take(5)
    }

    override fun getWatchlistFlow(): Flow<List<MediaItem>> {
        return watchlistDao.getAllFlow().map { entities ->
            entities.map { it.toMediaItem() }
        }
    }

    override suspend fun addToWatchlist(item: MediaItem) {
        watchlistDao.insert(item.toEntity())
    }

    override suspend fun removeFromWatchlist(id: String) {
        watchlistDao.deleteById(id)
    }

    override suspend fun isInWatchlist(id: String): Boolean {
        return watchlistDao.exists(id)
    }

    override suspend fun getMediaDetail(id: String): MediaItem? {
        return try {
            val movieDto = try {
                apiService.getMovieDetails(id)
            } catch (e: Exception) {
                null
            }
            if (movieDto != null && movieDto.title != null) {
                return movieDto.toMediaItem(MediaType.MOVIE)
            }
            val tvDto = apiService.getTvDetails(id)
            tvDto.toMediaItem(MediaType.TV)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun searchMedia(query: String): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        return try {
            val response = apiService.searchMulti(query)
            response.results.map { it.toMediaItem() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ---- Mappers ----

    private fun TmdbMediaDto.toMediaItem(fallbackType: MediaType? = null): MediaItem {
        val detectedType = when {
            mediaType == "tv" -> MediaType.TV
            mediaType == "movie" -> MediaType.MOVIE
            fallbackType != null -> fallbackType
            title == null && name != null -> MediaType.TV
            else -> MediaType.MOVIE
        }
        val displayTitle = title ?: name ?: "Untitled"
        val rawDate = releaseDate ?: firstAirDate ?: ""
        val posterUrl = posterPath?.let {
            if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w500$it"
        }
        val backdropUrl = backdropPath?.let {
            if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w780$it"
        }
        return MediaItem(
            id = id.toString(),
            title = displayTitle,
            overview = overview ?: "",
            posterPath = posterUrl,
            backdropPath = backdropUrl,
            rating = voteAverage?.let { Math.round(it * 10) / 10.0 } ?: 0.0,
            releaseDate = rawDate,
            mediaType = detectedType
        )
    }

    private fun WatchlistEntity.toMediaItem(): MediaItem = MediaItem(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        rating = rating,
        releaseDate = releaseDate,
        mediaType = MediaType.valueOf(mediaType)
    )

    private fun MediaItem.toEntity(): WatchlistEntity = WatchlistEntity(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        rating = rating,
        releaseDate = releaseDate,
        mediaType = mediaType.name
    )
}
