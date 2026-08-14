package com.alok.justrack.data.repository

import com.alok.justrack.data.api.TmdbApiService
import com.alok.justrack.data.db.*
import com.alok.justrack.data.model.*
import com.alok.justrack.data.mapper.TmdbMapper.toMediaItem
import com.alok.justrack.data.mapper.TmdbMapper.toMovieDetails
import com.alok.justrack.data.mapper.TmdbMapper.toSeason
import com.alok.justrack.data.mapper.TmdbMapper.toEpisode
import com.alok.justrack.data.mapper.TmdbMapper.toEntity
import com.alok.justrack.data.mapper.TmdbMapper.toPersonDetails
import com.alok.justrack.util.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbMediaRepository @Inject constructor(
    private val apiService: TmdbApiService,
    private val watchlistDao: WatchlistDao,
    private val favouriteDao: FavouriteDao,
    private val listDao: ListDao,
    private val customImageDao: CustomImageDao,
    private val episodeDao: EpisodeDao,
    private val watchedEpisodeDao: WatchedEpisodeDao
) : MediaRepository {

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
            val entities = watchlistDao.getAllOnce()
            entities
                .sortedByDescending { it.addedAt }
                .map { it.toMediaItem() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getMediaDetail(id: String, mediaType: MediaType): MovieDetails? {
        return try {
            val detail = when (mediaType) {
                MediaType.MOVIE -> apiService.getMovieDetails(id)
                MediaType.TV -> apiService.getTvDetails(id)
            }
            detail.toMovieDetails(mediaType)
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

    override fun getWatchlistFlow(): Flow<List<MediaItem>> = watchlistDao.getAllFlow()
        .map { entities -> 
            entities
                .sortedByDescending { it.addedAt }
                .map { it.toMediaItem() } 
        }

    override suspend fun addToWatchlist(item: MediaItem) {
        val entity = item.toWatchlistEntity(inWatchlist = true).copy(addedAt = System.currentTimeMillis())
        watchlistDao.insert(entity)
    }

    override suspend fun removeFromWatchlist(id: String) {
        val existing = watchlistDao.getEntityById(id)
        if (existing != null) {
            val hasProgress = if (existing.mediaType == MediaType.TV.name) {
                watchedEpisodeDao.getWatchedEpisodeCountForShow(id) > 0
            } else false

            if (existing.isWatched || hasProgress) {
                // Keep the record but remove from active watchlist if there is progress or it's fully watched
                watchlistDao.insert(existing.copy(inWatchlist = false))
            } else {
                // Delete entirely only if there's no progress
                watchlistDao.deleteById(id)
            }
        }
    }

    override suspend fun isInWatchlist(id: String): Boolean {
        return watchlistDao.getWatchlistStatus(id) ?: false
    }

    override suspend fun setWatched(item: MediaItem, watched: Boolean) {
        if (watched) {
            // Marking as watched: Ensure in DB with isWatched=true, inWatchlist=false
            val existing = watchlistDao.getEntityById(item.id)
            val addedAt = existing?.addedAt ?: System.currentTimeMillis()
            val entity = item.toWatchlistEntity(inWatchlist = false).copy(
                isWatched = true, 
                addedAt = addedAt
            )
            watchlistDao.insert(entity)

            // Special case for TV shows: If marking the whole show as watched, mark all episodes as watched too
            if (item.mediaType == MediaType.TV) {
                syncEpisodes(item.id) // Ensure we have the episodes
                val episodes = episodeDao.getEpisodesForShowOnce(item.id)
                if (episodes.isNotEmpty()) {
                    val entities = episodes.map {
                        WatchedEpisodeEntity(showId = item.id, seasonNumber = it.seasonNumber, episodeNumber = it.episodeNumber)
                    }
                    watchedEpisodeDao.insertAll(entities)
                }
            }
        } else {
            // Unmarking as watched: If also not in wishlist, delete entirely
            val existing = watchlistDao.getEntityById(item.id)
            if (existing != null) {
                if (existing.inWatchlist) {
                    watchlistDao.updateWatched(item.id, false)
                } else {
                    watchlistDao.deleteById(item.id)
                }

                // If unmarking a show, we don't necessarily unmark episodes 
                // as that might be destructive to specific progress. 
                // But checkShowCompletion will handle the show state if episodes change.
            }
        }
    }

    override suspend fun isWatched(id: String): Boolean {
        return watchlistDao.getWatchedStatus(id) ?: false
    }

    override suspend fun toggleWatchLater(id: String, isWatchLater: Boolean) {
        watchlistDao.updateWatchLater(id, isWatchLater)
    }

    override fun getFavouritesFlow(): Flow<List<MediaItem>> = favouriteDao.getAllFlow()
        .map { entities -> entities.map { it.toMediaItem() } }

    override fun getFavouritesByTypeFlow(mediaType: MediaType): Flow<List<MediaItem>> = 
        favouriteDao.getByTypeFlow(mediaType.name)
            .map { entities -> entities.map { it.toMediaItem() } }

    override suspend fun toggleFavourite(item: MediaItem): Boolean {
        val exists = favouriteDao.exists(item.id, item.mediaType.name)
        if (exists) {
            favouriteDao.delete(item.id, item.mediaType.name)
        } else {
            favouriteDao.insert(item.toFavouriteEntity())
        }
        return !exists
    }

    override suspend fun isFavourite(mediaId: String, mediaType: MediaType): Boolean {
        return favouriteDao.exists(mediaId, mediaType.name)
    }

    override fun getListsFlow(): Flow<List<Pair<String, String>>> = listDao.getAllListsFlow()
        .map { entities -> entities.map { it.id to it.name } }

    override suspend fun createList(name: String) {
        listDao.createList(ListEntity(id = UUID.randomUUID().toString(), name = name))
    }

    override suspend fun deleteList(listId: String) {
        listDao.deleteList(listId)
        listDao.deleteListItems(listId)
    }

    override suspend fun addToList(listId: String, item: MediaItem) {
        listDao.addItem(item.toListItemEntity(listId))
    }

    override suspend fun removeFromList(listId: String, mediaId: String, mediaType: MediaType) {
        listDao.removeItem(listId, mediaId, mediaType.name)
    }

    override suspend fun isInList(listId: String, mediaId: String, mediaType: MediaType): Boolean {
        return listDao.isInList(listId, mediaId, mediaType.name)
    }

    override suspend fun getListsForMedia(mediaId: String, mediaType: MediaType): List<String> {
        return listDao.getListsForMedia(mediaId, mediaType.name)
    }

    override fun getListItemsFlow(listId: String): Flow<List<MediaItem>> = listDao.getListItemsFlow(listId)
        .map { entities -> entities.map { it.toMediaItem() } }

    override suspend fun getMovieImages(id: String): Pair<List<String>, List<String>> {
        return try {
            val response = apiService.getMovieImages(id)
            val posters = response.posters.map { "${Constants.TMDB_IMAGE_BASE_URL_W500}${it.filePath}" }
            val backdrops = response.backdrops.map { "${Constants.TMDB_IMAGE_BASE_URL_W780}${it.filePath}" }
            posters to backdrops
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList<String>() to emptyList()
        }
    }

    override suspend fun getTvImages(id: String): Pair<List<String>, List<String>> {
        return try {
            val response = apiService.getTvImages(id)
            val posters = response.posters.map { "${Constants.TMDB_IMAGE_BASE_URL_W500}${it.filePath}" }
            val backdrops = response.backdrops.map { "${Constants.TMDB_IMAGE_BASE_URL_W780}${it.filePath}" }
            posters to backdrops
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList<String>() to emptyList()
        }
    }

    override suspend fun saveCustomPoster(id: String, url: String?) {
        customImageDao.updatePoster(id, url)
        watchlistDao.updateCustomPoster(id, url)
    }

    override suspend fun saveCustomBackdrop(id: String, url: String?) {
        customImageDao.updateBackdrop(id, url)
        watchlistDao.updateCustomBackdrop(id, url)
    }

    override suspend fun getCustomPoster(id: String): String? {
        return watchlistDao.getCustomPoster(id) ?: customImageDao.getPoster(id)
    }

    override suspend fun getCustomBackdrop(id: String): String? {
        return watchlistDao.getCustomBackdrop(id) ?: customImageDao.getBackdrop(id)
    }

    override suspend fun syncEpisodes(showId: String) {
        try {
            val tvDetails = apiService.getTvDetails(showId)
            val seasons = tvDetails.seasons ?: return
            
            val fallbackRuntime = when {
                tvDetails.episodeRunTime != null && tvDetails.episodeRunTime.isNotEmpty() -> tvDetails.episodeRunTime.first()
                tvDetails.originalLanguage == "ja" -> 24
                else -> 45
            }

            coroutineScope {
                seasons.chunked(5).forEach { chunk ->
                    chunk.map { seasonDto ->
                        async {
                            try {
                                val seasonDetails = apiService.getTvSeasonDetails(showId, seasonDto.seasonNumber)
                                val entities = seasonDetails.episodes?.map { episodeDto ->
                                    episodeDto.toEntity(showId, fallbackRuntime)
                                } ?: emptyList()
                                episodeDao.insertAll(entities)
                                _episodesUpdateEvents.emit(Unit)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }.awaitAll()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun getSeasonDetails(tvId: String, seasonNumber: Int): Season? {
        return try {
            val tvDetails = apiService.getTvDetails(tvId)
            val fallbackRuntime = when {
                tvDetails.episodeRunTime != null && tvDetails.episodeRunTime.isNotEmpty() -> tvDetails.episodeRunTime.first()
                tvDetails.originalLanguage == "ja" -> 24
                else -> 45
            }

            val response = apiService.getTvSeasonDetails(tvId, seasonNumber)
            val watchedEpisodes = watchedEpisodeDao.getWatchedEpisodesForShowOnce(tvId)
                .filter { it.seasonNumber == seasonNumber }
                .map { "S${it.seasonNumber}E${it.episodeNumber}" }
                .toSet()
            
            response.toSeason(watchedEpisodes, fallbackRuntime)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun markEpisodeWatched(showId: String, seasonNumber: Int, episodeNumber: Int, watched: Boolean) {
        if (watched) {
            watchedEpisodeDao.insert(WatchedEpisodeEntity(showId = showId, seasonNumber = seasonNumber, episodeNumber = episodeNumber))
            
            // Ensure the show exists in the watchlist table so it can be tracked/displayed
            val existing = watchlistDao.getEntityById(showId)
            if (existing == null) {
                // If it doesn't exist, we need to fetch details to create a placeholder
                try {
                    val details = apiService.getTvDetails(showId)
                    val mediaItem = details.toMediaItem(MediaType.TV)
                    val entity = mediaItem.toWatchlistEntity(inWatchlist = false).copy(
                        isWatched = false,
                        addedAt = System.currentTimeMillis()
                    )
                    watchlistDao.insert(entity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            checkShowCompletion(showId)
        } else {
            watchedEpisodeDao.delete(showId, seasonNumber, episodeNumber)
        }
    }

    override suspend fun markEpisodesWatched(showId: String, seasonNumber: Int, episodeNumbers: List<Int>, watched: Boolean) {
        if (watched) {
            val entities = episodeNumbers.map {
                WatchedEpisodeEntity(showId = showId, seasonNumber = seasonNumber, episodeNumber = it)
            }
            watchedEpisodeDao.insertAll(entities)
            checkShowCompletion(showId)
        } else {
            // Not typically used for multi-unmark but implemented for completeness
            episodeNumbers.forEach { 
                watchedEpisodeDao.delete(showId, seasonNumber, it)
            }
        }
    }

    override suspend fun markSeasonWatched(showId: String, seasonNumber: Int, watched: Boolean, episodes: List<Episode>) {
        if (watched) {
            val entities = episodes.map { 
                WatchedEpisodeEntity(showId = showId, seasonNumber = seasonNumber, episodeNumber = it.episodeNumber) 
            }
            watchedEpisodeDao.insertAll(entities)
            checkShowCompletion(showId)
        } else {
            watchedEpisodeDao.deleteSeason(showId, seasonNumber)
        }
    }

    override fun getWatchedEpisodesFlow(showId: String): Flow<List<String>> = 
        watchedEpisodeDao.getWatchedEpisodesForShow(showId)
            .map { list -> list.map { "S${it.seasonNumber}E${it.episodeNumber}" } }

    override fun getAllWatchedEpisodesFlow(): Flow<Map<String, Set<String>>> = 
        watchedEpisodeDao.getAllWatchedEpisodesFlow()
            .map { list ->
                list.groupBy({ it.showId }, { "S${it.seasonNumber}E${it.episodeNumber}" })
                    .mapValues { it.value.toSet() }
            }

    override suspend fun getNextEpisodeToWatch(showId: String, onlyReleased: Boolean): Episode? {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nextEntity = if (onlyReleased) {
            episodeDao.getNextReleasedUnwatchedEpisode(showId, today)
        } else {
            episodeDao.getNextUnwatchedEpisode(showId)
        }
        
        return nextEntity?.let { entity ->
            Episode(
                id = "${entity.showId}_${entity.seasonNumber}_${entity.episodeNumber}",
                name = entity.title,
                overview = entity.overview ?: "",
                stillPath = entity.stillPath,
                seasonNumber = entity.seasonNumber,
                episodeNumber = entity.episodeNumber,
                airDate = entity.airDate,
                voteAverage = entity.voteAverage ?: 0.0,
                isWatched = false
            )
        }
    }

    override suspend fun getUnwatchedEpisodeCount(showId: String): Int {
        val total = episodeDao.getTotalEpisodeCount(showId)
        val watched = episodeDao.getWatchedEpisodeCount(showId)
        return (total - watched).coerceAtLeast(0)
    }

    override suspend fun getWatchedEpisodeCount(showId: String): Int {
        return episodeDao.getWatchedEpisodeCount(showId)
    }

    override suspend fun getTotalEpisodeCount(showId: String): Int {
        return episodeDao.getTotalEpisodeCount(showId)
    }

    override suspend fun getReleasedEpisodeCount(showId: String): Int {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return episodeDao.getReleasedEpisodeCount(showId, today)
    }

    override fun getTotalAiredRuntimeFlow(showId: String): Flow<Int> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return episodeDao.getTotalAiredRuntimeFlow(showId, today)
            .map { it ?: 0 }
    }

    override fun getTotalWatchedTvRuntimeFlow(): Flow<Int> = 
        episodeDao.getTotalWatchedTvRuntimeFlow().map { it ?: 0 }

    override fun getTotalWatchedEpisodeCountFlow(): Flow<Int> = 
        episodeDao.getTotalWatchedEpisodeCountFlow()

    override fun getTotalWatchedMovieRuntimeFlow(): Flow<Int> = 
        watchlistDao.getTotalWatchedMovieRuntimeFlow().map { it ?: 0 }

    override fun getTotalWatchedMovieCountFlow(): Flow<Int> = 
        watchlistDao.getTotalWatchedMovieCountFlow()

    override suspend fun syncMissingRuntimes() {
        withContext(Dispatchers.IO) {
            // 1. Handle Movies: Fetch watched movies with 0 runtime
            // We'll process them in chunks to show progress gradually and respect rate limits
            var moviesToSync = watchlistDao.getWatchedMoviesMissingRuntime()
            
            while (moviesToSync.isNotEmpty()) {
                val batch = moviesToSync.take(20)
                batch.forEach { entity ->
                    try {
                        val details = apiService.getMovieDetails(entity.id)
                        if (details.runtime != null && details.runtime > 0) {
                            watchlistDao.updateRuntime(entity.id, details.runtime)
                        }
                        delay(150) // Respect TMDb rate limits (~40 requests per 10 seconds)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // Refresh list for next batch
                moviesToSync = watchlistDao.getWatchedMoviesMissingRuntime()
                // Safety break if needed, but the loop naturally terminates when all are updated
            }

            // 2. Handle TV Shows: Check for watched shows that might be missing episode data
            val watchedShows = watchlistDao.getAllOnce()
                .filter { it.mediaType == MediaType.TV.name && it.isWatched }
            
            watchedShows.forEach { show ->
                try {
                    // If we have no episodes stored for a watched show, sync them to get runtimes
                    if (episodeDao.getTotalEpisodeCount(show.id) == 0) {
                        syncEpisodes(show.id)
                        delay(500)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override suspend fun getMaxEpisodeNumberForSeason(showId: String, seasonNumber: Int): Int? {
        return episodeDao.getMaxEpisodeNumberForSeason(showId, seasonNumber)
    }

    override suspend fun getFutureEpisodes(showId: String): List<Episode> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return episodeDao.getFutureEpisodes(showId, today)
            .filter { it.seasonNumber > 0 }
            .map { entity ->
            Episode(
                id = "${entity.showId}_${entity.seasonNumber}_${entity.episodeNumber}",
                name = entity.title,
                overview = entity.overview ?: "",
                stillPath = entity.stillPath,
                seasonNumber = entity.seasonNumber,
                episodeNumber = entity.episodeNumber,
                airDate = entity.airDate,
                voteAverage = entity.voteAverage ?: 0.0,
                isWatched = false
            )
        }
    }

    override suspend fun getLatestWatchActivity(showId: String): Long? {
        return watchedEpisodeDao.getLatestWatchActivityForShow(showId)
    }

    private suspend fun checkShowCompletion(showId: String) {
        val total = episodeDao.getTotalEpisodeCount(showId)
        val watched = episodeDao.getWatchedEpisodeCount(showId)
        
        if (total > 0 && watched == total) {
            // Automatically mark as watched and remove from watchlist
            val existing = watchlistDao.getEntityById(showId)
            if (existing != null && !existing.isWatched) {
                val updated = existing.copy(
                    isWatched = true,
                    inWatchlist = false,
                    addedAt = System.currentTimeMillis() // Update timestamp for sorting in Profile
                )
                watchlistDao.insert(updated)
                _showCompletionEvents.emit(showId)
            }
        }
    }

    override suspend fun getPersonDetails(id: String): PersonDetails? {
        return try {
            apiService.getPersonDetails(id).toPersonDetails()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ---- Mappers ----

    private fun WatchlistEntity.toMediaItem(): MediaItem = MediaItem(
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
        runtime = runtime,
        addedAt = addedAt
    )

    private fun MediaItem.toWatchlistEntity(inWatchlist: Boolean): WatchlistEntity = WatchlistEntity(
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
        runtime = runtime,
        addedAt = addedAt
    )

    private fun FavouriteEntity.toMediaItem(): MediaItem = MediaItem(
        id = mediaId,
        title = title,
        overview = "",
        posterPath = posterPath,
        backdropPath = backdropPath,
        rating = 0.0,
        releaseDate = "",
        mediaType = MediaType.valueOf(mediaType)
    )

    private fun MediaItem.toFavouriteEntity(): FavouriteEntity = FavouriteEntity(
        id = UUID.randomUUID().toString(),
        mediaId = id,
        mediaType = mediaType.name,
        title = title,
        posterPath = posterPath,
        backdropPath = backdropPath
    )

    private fun ListItemEntity.toMediaItem(): MediaItem = MediaItem(
        id = mediaId,
        title = title,
        overview = "",
        posterPath = posterPath,
        backdropPath = backdropPath,
        rating = 0.0,
        releaseDate = "",
        mediaType = MediaType.valueOf(mediaType)
    )

    private fun MediaItem.toListItemEntity(listId: String): ListItemEntity = ListItemEntity(
        id = UUID.randomUUID().toString(),
        listId = listId,
        mediaId = id,
        mediaType = mediaType.name,
        title = title,
        posterPath = posterPath,
        backdropPath = backdropPath
    )
}
