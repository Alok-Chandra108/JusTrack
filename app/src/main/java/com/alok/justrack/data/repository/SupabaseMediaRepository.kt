package com.alok.justrack.data.repository

import com.alok.justrack.data.api.*
import com.alok.justrack.data.model.*
import com.alok.justrack.data.model.PersonDetails
import com.alok.justrack.data.mapper.TmdbMapper.toMediaItem
import com.alok.justrack.data.mapper.TmdbMapper.toMovieDetails
import com.alok.justrack.data.supabase.SupabaseClientProvider
import com.alok.justrack.data.supabase.SupabaseWatchlistItem
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseMediaRepository @Inject constructor(
    private val apiService: TmdbApiService
) : MediaRepository {

    private val postgrest = SupabaseClientProvider.client.postgrest["watchlist"]

    private val _episodesUpdateEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val episodesUpdateEvents: Flow<Unit> = _episodesUpdateEvents.asSharedFlow()

    private val _showCompletionEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val showCompletionEvents: Flow<String> = _showCompletionEvents.asSharedFlow()

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
        return try {
            val results = postgrest.select().decodeList<SupabaseWatchlistItem>()
            results.map { it.toMediaItem() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getWatchlistFlow(): Flow<List<MediaItem>> = flow {
        emit(getWatchlist())
    }

    override suspend fun addToWatchlist(item: MediaItem) {
        try {
            // Upsert with inWatchlist=true
            val supabaseItem = item.toSupabaseItem().copy(inWatchlist = true, addedAt = System.currentTimeMillis())
            postgrest.upsert(supabaseItem)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun removeFromWatchlist(id: String) {
        try {
            postgrest.delete {
                filter {
                    eq("id", id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun isInWatchlist(id: String): Boolean {
        return try {
            val result = postgrest.select {
                filter {
                    eq("id", id)
                }
            }.decodeList<SupabaseWatchlistItem>()
            result.firstOrNull()?.inWatchlist ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun setWatched(item: MediaItem, watched: Boolean) {
        try {
            if (watched) {
                // Upsert with isWatched=true, inWatchlist=false
                val supabaseItem = item.toSupabaseItem().copy(isWatched = true, inWatchlist = false, addedAt = System.currentTimeMillis())
                postgrest.upsert(supabaseItem)
            } else {
                // If unmarking, we need to know if it's in watchlist
                val result = postgrest.select { filter { eq("id", item.id) } }.decodeSingleOrNull<SupabaseWatchlistItem>()
                if (result != null) {
                    if (result.inWatchlist) {
                        postgrest.update({
                            set("is_watched", false)
                        }) {
                            filter { eq("id", item.id) }
                        }
                    } else {
                        postgrest.delete {
                            filter { eq("id", item.id) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun isWatched(id: String): Boolean {
        return try {
            val result = postgrest.select(columns = Columns.list("is_watched")) {
                filter { eq("id", id) }
            }.decodeList<SupabaseWatchlistItem>()
            result.firstOrNull()?.isWatched ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun toggleWatchLater(id: String, isWatchLater: Boolean) {
        try {
            postgrest.update({
                set("is_watch_later", isWatchLater)
            }) {
                filter { eq("id", id) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun getMediaDetail(id: String, mediaType: MediaType): MovieDetails? {
        return try {
            when (mediaType) {
                MediaType.MOVIE -> {
                    val movieDto = apiService.getMovieDetails(id)
                    movieDto.toMovieDetails(MediaType.MOVIE)
                }
                MediaType.TV -> {
                    val tvDto = apiService.getTvDetails(id)
                    tvDto.toMovieDetails(MediaType.TV)
                }
            }
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

    private fun SupabaseWatchlistItem.toMediaItem(): MediaItem = MediaItem(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        rating = rating,
        releaseDate = releaseDate,
        mediaType = MediaType.valueOf(mediaType),
        isWatched = isWatched,
        inWatchlist = inWatchlist,
        isWatchLater = isWatchLater,
        addedAt = addedAt
    )

    private fun MediaItem.toSupabaseItem(): SupabaseWatchlistItem = SupabaseWatchlistItem(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        rating = rating,
        releaseDate = releaseDate,
        mediaType = mediaType.name,
        isWatched = isWatched,
        inWatchlist = inWatchlist,
        isWatchLater = isWatchLater,
        addedAt = addedAt
    )

    // ---- Favourites (stub - not implemented for Supabase yet) ----

    override fun getFavouritesFlow(): Flow<List<MediaItem>> = flow { emit(emptyList()) }
    override fun getFavouritesByTypeFlow(mediaType: MediaType): Flow<List<MediaItem>> = flow { emit(emptyList()) }
    override suspend fun toggleFavourite(item: MediaItem): Boolean = false
    override suspend fun isFavourite(mediaId: String, mediaType: MediaType): Boolean = false

    // ---- Custom Lists (stub - not implemented for Supabase yet) ----

    override fun getListsFlow(): Flow<List<Pair<String, String>>> = flow { emit(emptyList()) }
    override suspend fun createList(name: String) {}
    override suspend fun deleteList(listId: String) {}
    override suspend fun addToList(listId: String, item: MediaItem) {}
    override suspend fun removeFromList(listId: String, mediaId: String, mediaType: MediaType) {}
    override suspend fun isInList(listId: String, mediaId: String, mediaType: MediaType): Boolean = false
    override suspend fun getListsForMedia(mediaId: String, mediaType: MediaType): List<String> = emptyList()
    override fun getListItemsFlow(listId: String): Flow<List<MediaItem>> = flow { emit(emptyList()) }

    // ---- TMDb Images ----

    override suspend fun getMovieImages(id: String): Pair<List<String>, List<String>> {
        return try {
            val response = apiService.getMovieImages(id)
            val posters = response.posters.map { "https://image.tmdb.org/t/p/w500${it.filePath}" }
            val backdrops = response.backdrops.map { "https://image.tmdb.org/t/p/w780${it.filePath}" }
            posters to backdrops
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList<String>() to emptyList()
        }
    }

    override suspend fun getTvImages(id: String): Pair<List<String>, List<String>> {
        return try {
            val response = apiService.getTvImages(id)
            val posters = response.posters.map { "https://image.tmdb.org/t/p/w500${it.filePath}" }
            val backdrops = response.backdrops.map { "https://image.tmdb.org/t/p/w780${it.filePath}" }
            posters to backdrops
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList<String>() to emptyList()
        }
    }

    // ---- Custom poster/backdrop persistence (stub) ----

    override suspend fun saveCustomPoster(id: String, url: String?) {}
    override suspend fun saveCustomBackdrop(id: String, url: String?) {}
    override suspend fun getCustomPoster(id: String): String? = null
    override suspend fun getCustomBackdrop(id: String): String? = null

    // Episode Tracking (stub for Supabase)
    override suspend fun syncEpisodes(showId: String) {}
    override suspend fun getSeasonDetails(tvId: String, seasonNumber: Int): Season? = null
    override suspend fun markEpisodeWatched(showId: String, seasonNumber: Int, episodeNumber: Int, watched: Boolean) {}
    override suspend fun markEpisodesWatched(showId: String, seasonNumber: Int, episodeNumbers: List<Int>, watched: Boolean) {}
    override suspend fun markSeasonWatched(showId: String, seasonNumber: Int, watched: Boolean, episodes: List<Episode>) {}
    override fun getWatchedEpisodesFlow(showId: String): Flow<List<String>> = flow { emit(emptyList()) }
    override fun getAllWatchedEpisodesFlow(): Flow<Map<String, Set<String>>> = flow { emit(emptyMap()) }
    override suspend fun getNextEpisodeToWatch(showId: String, onlyReleased: Boolean): Episode? = null
    override suspend fun getUnwatchedEpisodeCount(showId: String): Int = 0
    override suspend fun getWatchedEpisodeCount(showId: String): Int = 0
    override suspend fun getTotalEpisodeCount(showId: String): Int = 0
    override suspend fun getReleasedEpisodeCount(showId: String): Int = 0
    override fun getTotalAiredRuntimeFlow(showId: String): Flow<Int> = flow { emit(0) }
    override suspend fun getMaxEpisodeNumberForSeason(showId: String, seasonNumber: Int): Int? = null
    override suspend fun getFutureEpisodes(showId: String): List<Episode> = emptyList()
    override suspend fun getLatestWatchActivity(showId: String): Long? = null

    // Stats (stubs)
    override fun getTotalWatchedTvRuntimeFlow(): Flow<Int> = flow { emit(0) }
    override fun getTotalWatchedEpisodeCountFlow(): Flow<Int> = flow { emit(0) }
    override fun getTotalWatchedMovieRuntimeFlow(): Flow<Int> = flow { emit(0) }
    override fun getTotalWatchedMovieCountFlow(): Flow<Int> = flow { emit(0) }
    override suspend fun syncMissingRuntimes() {}

    override suspend fun getPersonDetails(id: String): PersonDetails? = null
}
