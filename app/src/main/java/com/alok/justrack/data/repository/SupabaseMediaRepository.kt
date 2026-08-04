package com.alok.justrack.data.repository

import com.alok.justrack.data.api.*
import com.alok.justrack.data.model.*
import com.alok.justrack.data.supabase.SupabaseClientProvider
import com.alok.justrack.data.supabase.SupabaseWatchlistItem
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseMediaRepository @Inject constructor(
    private val apiService: TmdbApiService
) : MediaRepository {

    private val postgrest = SupabaseClientProvider.client.postgrest["watchlist"]

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
            val result = postgrest.select(columns = Columns.list("id")) {
                filter {
                    eq("id", id)
                }
            }.decodeList<SupabaseWatchlistItem>()
            result.isNotEmpty()
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

    private fun TmdbMediaDto.toMediaItem(fallbackType: MediaType? = null): MediaItem {
        val detectedType = when {
            mediaType == "tv" -> MediaType.TV
            mediaType == "movie" -> MediaType.MOVIE
            fallbackType != null -> fallbackType
            name != null && title == null -> MediaType.TV
            name != null && firstAirDate != null -> MediaType.TV
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

    private fun TmdbMediaDto.toMovieDetails(type: MediaType): MovieDetails {
        val displayTitle = title ?: name ?: "Untitled"
        val rawDate = releaseDate ?: firstAirDate ?: ""
        val posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        val backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }

        val runtimeStr = when {
            runtime != null -> "${runtime / 60}h ${runtime % 60}m"
            episodeRunTime != null && episodeRunTime.isNotEmpty() -> "${episodeRunTime.first()}m"
            else -> "-"
        }

        val castMembers = credits?.cast?.map {
            CastMember(
                id = it.id.toString(),
                name = it.name,
                character = it.character,
                profilePath = it.profilePath?.let { path -> "https://image.tmdb.org/t/p/w185$path" }
            )
        } ?: emptyList()

        val directorNames = when (type) {
            MediaType.MOVIE -> credits?.crew?.filter { it.job == "Director" }?.map { it.name }?.distinct() ?: emptyList()
            MediaType.TV -> {
                val creators = createdBy?.map { it.name }?.distinct()
                if (!creators.isNullOrEmpty()) creators
                else credits?.crew?.filter { it.job == "Executive Producer" }?.map { it.name }?.distinct() ?: emptyList()
            }
        }.ifEmpty { listOf("-") }

        return MovieDetails(
            id = id.toString(),
            title = displayTitle,
            overview = overview ?: "",
            posterPath = posterUrl,
            backdropPath = backdropUrl,
            rating = voteAverage?.let { Math.round(it * 10) / 10.0 } ?: 0.0,
            releaseDate = rawDate,
            runtime = runtimeStr,
            certification = "-",
            director = directorNames,
            cast = castMembers,
            ratings = listOf(
                RatingSource("TMDb", String.format("%.1f", voteAverage ?: 0.0))
            ),
            recommendations = emptyList()
        )
    }

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
    override fun getWatchedEpisodesFlow(showId: String): Flow<List<String>> = flow { emit(emptyList()) }
    override fun getAllWatchedEpisodesFlow(): Flow<Map<String, Set<String>>> = flow { emit(emptyMap()) }
    override suspend fun getNextEpisodeToWatch(showId: String): Episode? = null
}
