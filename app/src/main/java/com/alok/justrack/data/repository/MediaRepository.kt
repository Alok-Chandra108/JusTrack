package com.alok.justrack.data.repository

import com.alok.justrack.data.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    suspend fun getTrending(): List<MediaItem>
    suspend fun getWatchlist(): List<MediaItem>
    suspend fun getMediaDetail(id: String): MediaItem?
    suspend fun searchMedia(query: String): List<MediaItem>

    // Room-backed watchlist operations
    fun getWatchlistFlow(): Flow<List<MediaItem>>
    suspend fun addToWatchlist(item: MediaItem)
    suspend fun removeFromWatchlist(id: String)
    suspend fun isInWatchlist(id: String): Boolean
}
