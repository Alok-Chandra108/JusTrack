package com.alok.justrack.data.repository

import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.model.MovieDetails
import com.alok.justrack.data.model.Season
import com.alok.justrack.data.model.Episode
import com.alok.justrack.data.model.PersonDetails
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
    suspend fun setWatched(item: MediaItem, watched: Boolean)
    suspend fun isWatched(id: String): Boolean

    // Favourites
    fun getFavouritesFlow(): Flow<List<MediaItem>>
    fun getFavouritesByTypeFlow(mediaType: MediaType): Flow<List<MediaItem>>
    suspend fun toggleFavourite(item: MediaItem): Boolean
    suspend fun isFavourite(mediaId: String, mediaType: MediaType): Boolean

    // Custom lists
    fun getListsFlow(): Flow<List<Pair<String, String>>>
    suspend fun createList(name: String)
    suspend fun deleteList(listId: String)
    suspend fun addToList(listId: String, item: MediaItem)
    suspend fun removeFromList(listId: String, mediaId: String, mediaType: MediaType)
    suspend fun isInList(listId: String, mediaId: String, mediaType: MediaType): Boolean
    suspend fun getListsForMedia(mediaId: String, mediaType: MediaType): List<String>
    fun getListItemsFlow(listId: String): Flow<List<MediaItem>>

    // TMDb images
    suspend fun getMovieImages(id: String): Pair<List<String>, List<String>>
    suspend fun getTvImages(id: String): Pair<List<String>, List<String>>

    // Custom poster/backdrop persistence
    suspend fun saveCustomPoster(id: String, url: String?)
    suspend fun saveCustomBackdrop(id: String, url: String?)
    suspend fun getCustomPoster(id: String): String?
    suspend fun getCustomBackdrop(id: String): String?

    // Episode Tracking
    val episodesUpdateEvents: Flow<Unit>
    val showCompletionEvents: Flow<String> // Emits showId when a show is automatically marked as watched
    suspend fun syncEpisodes(showId: String)
    suspend fun getSeasonDetails(tvId: String, seasonNumber: Int): Season?
    suspend fun markEpisodeWatched(showId: String, seasonNumber: Int, episodeNumber: Int, watched: Boolean)
    suspend fun markEpisodesWatched(showId: String, seasonNumber: Int, episodeNumbers: List<Int>, watched: Boolean)
    suspend fun markSeasonWatched(showId: String, seasonNumber: Int, watched: Boolean, episodes: List<Episode>)
    fun getWatchedEpisodesFlow(showId: String): Flow<List<String>> // Returns "S{season}E{episode}" strings
    fun getAllWatchedEpisodesFlow(): Flow<Map<String, Set<String>>> // Map<showId, Set<"S{season}E{episode}">>
    suspend fun getNextEpisodeToWatch(showId: String, onlyReleased: Boolean = false): Episode?
    suspend fun getUnwatchedEpisodeCount(showId: String): Int
    suspend fun getWatchedEpisodeCount(showId: String): Int
    suspend fun getTotalEpisodeCount(showId: String): Int
    suspend fun getMaxEpisodeNumberForSeason(showId: String, seasonNumber: Int): Int?
    suspend fun getFutureEpisodes(showId: String): List<Episode>
    suspend fun getLatestWatchActivity(showId: String): Long?

    // Person
    suspend fun getPersonDetails(id: String): PersonDetails?
}
