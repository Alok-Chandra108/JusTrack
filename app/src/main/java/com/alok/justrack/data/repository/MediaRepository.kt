package com.alok.justrack.data.repository

import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.model.MovieDetails
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    suspend fun getTrending(): List<MediaItem>
    suspend fun getWatchlist(): List<MediaItem>
    suspend fun getMediaDetail(id: String, mediaType: MediaType = MediaType.MOVIE): MovieDetails?
    suspend fun searchMedia(query: String): List<MediaItem>

    // Room-backed watchlist operations
    fun getWatchlistFlow(): Flow<List<MediaItem>>
    suspend fun addToWatchlist(item: MediaItem)
    suspend fun removeFromWatchlist(id: String)
    suspend fun isInWatchlist(id: String): Boolean
    suspend fun setWatched(id: String, watched: Boolean)
    suspend fun isWatched(id: String): Boolean
}
