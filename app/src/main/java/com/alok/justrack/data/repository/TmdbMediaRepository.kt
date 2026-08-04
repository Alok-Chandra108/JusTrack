package com.alok.justrack.data.repository

import com.alok.justrack.data.api.TmdbApiService
import com.alok.justrack.data.api.TmdbMediaDto
import com.alok.justrack.data.api.TmdbEpisodeDto
import com.alok.justrack.data.api.TmdbSeasonDto
import com.alok.justrack.data.db.*
import com.alok.justrack.data.model.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
        watchlistDao.deleteById(id)
    }

    override suspend fun isInWatchlist(id: String): Boolean {
        return watchlistDao.getWatchlistStatus(id) ?: false
    }

    override suspend fun setWatched(item: MediaItem, watched: Boolean) {
        if (watched) {
            // Marking as watched: Ensure in DB with isWatched=true, inWatchlist=false
            val entity = item.toWatchlistEntity(inWatchlist = false).copy(isWatched = true, addedAt = System.currentTimeMillis())
            watchlistDao.insert(entity)
        } else {
            // Unmarking as watched: If also not in wishlist, delete entirely
            val existing = watchlistDao.getEntityById(item.id)
            if (existing != null) {
                if (existing.inWatchlist) {
                    watchlistDao.updateWatched(item.id, false)
                } else {
                    watchlistDao.deleteById(item.id)
                }
            }
        }
    }

    override suspend fun isWatched(id: String): Boolean {
        return watchlistDao.getWatchedStatus(id) ?: false
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
            tvDetails.seasons?.forEach { seasonDto ->
                val seasonDetails = apiService.getTvSeasonDetails(showId, seasonDto.seasonNumber)
                val entities = seasonDetails.episodes?.map { episodeDto ->
                    episodeDto.toEntity(showId)
                } ?: emptyList()
                episodeDao.insertAll(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun getSeasonDetails(tvId: String, seasonNumber: Int): Season? {
        return try {
            val response = apiService.getTvSeasonDetails(tvId, seasonNumber)
            val watchedEpisodes = watchedEpisodeDao.getWatchedEpisodesForShowOnce(tvId)
                .filter { it.seasonNumber == seasonNumber }
                .map { "S${it.seasonNumber}E${it.episodeNumber}" }
                .toSet()
            
            response.toSeason(watchedEpisodes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun markEpisodeWatched(showId: String, seasonNumber: Int, episodeNumber: Int, watched: Boolean) {
        if (watched) {
            watchedEpisodeDao.insert(WatchedEpisodeEntity(showId = showId, seasonNumber = seasonNumber, episodeNumber = episodeNumber))
        } else {
            watchedEpisodeDao.delete(showId, seasonNumber, episodeNumber)
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

    override suspend fun getNextEpisodeToWatch(showId: String): Episode? {
        val nextEntity = episodeDao.getNextUnwatchedEpisode(showId)
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

    // ---- Mappers ----

    private fun TmdbMediaDto.toMediaItem(fallbackType: MediaType? = null): MediaItem {
        val detectedType = when {
            mediaType == "tv" -> MediaType.TV
            mediaType == "movie" -> MediaType.MOVIE
            fallbackType != null -> fallbackType
            // TV shows use 'name', Movies use 'title'
            name != null && title == null -> MediaType.TV
            name != null && firstAirDate != null -> MediaType.TV
            else -> MediaType.MOVIE
        }
        val displayTitle = title ?: name ?: "Untitled"
        val rawDate = releaseDate ?: firstAirDate ?: ""
        val posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        val backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
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
        mediaType = MediaType.valueOf(mediaType),
        isWatched = isWatched,
        inWatchlist = inWatchlist,
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

    private fun TmdbMediaDto.toMovieDetails(type: MediaType): MovieDetails {
        val detectedType = when {
            mediaType == "tv" -> MediaType.TV
            mediaType == "movie" -> MediaType.MOVIE
            name != null && title == null -> MediaType.TV
            name != null && firstAirDate != null -> MediaType.TV
            else -> type
        }
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

        val directorNames = when (detectedType) {
            MediaType.MOVIE -> credits?.crew?.filter { it.job == "Director" }?.map { it.name }?.distinct() ?: emptyList()
            MediaType.TV -> createdBy?.map { it.name }?.distinct() ?: emptyList()
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
            mediaType = detectedType,
            cast = castMembers,
            ratings = listOf(
                RatingSource("TMDb", String.format("%.1f", voteAverage ?: 0.0))
            ),
            recommendations = recommendations?.results?.map { it.toMediaItem() } ?: emptyList(),
            seasons = seasons?.map { it.toSeason(emptySet()) } ?: emptyList()
        )
    }

    private fun TmdbSeasonDto.toSeason(watchedEpisodes: Set<String>): Season {
        return Season(
            id = id.toString(),
            name = name,
            overview = overview ?: "",
            posterPath = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            seasonNumber = seasonNumber,
            episodeCount = episodeCount ?: 0,
            airDate = airDate,
            episodes = episodes?.map { it.toEpisode(watchedEpisodes.contains("S${it.seasonNumber}E${it.episodeNumber}")) } ?: emptyList()
        )
    }

    private fun TmdbEpisodeDto.toEpisode(isWatched: Boolean): Episode {
        return Episode(
            id = id.toString(),
            name = name,
            overview = overview ?: "",
            stillPath = stillPath?.let { "https://image.tmdb.org/t/p/w300$it" },
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            airDate = airDate,
            voteAverage = voteAverage ?: 0.0,
            isWatched = isWatched
        )
    }

    private fun TmdbEpisodeDto.toEntity(showId: String): EpisodeEntity {
        return EpisodeEntity(
            showId = showId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            title = name,
            overview = overview,
            airDate = airDate,
            stillPath = stillPath,
            voteAverage = voteAverage
        )
    }
}
