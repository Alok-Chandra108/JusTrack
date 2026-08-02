package com.alok.justrack.data.repository

import com.alok.justrack.data.api.TmdbApiService
import com.alok.justrack.data.api.TmdbMediaDto
import com.alok.justrack.data.db.*
import com.alok.justrack.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbMediaRepository @Inject constructor(
    private val apiService: TmdbApiService,
    private val watchlistDao: WatchlistDao,
    private val favouriteDao: FavouriteDao,
    private val listDao: ListDao,
    private val customImageDao: CustomImageDao
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

    override suspend fun setWatched(id: String, watched: Boolean) {
        watchlistDao.updateWatched(id, watched)
    }

    override suspend fun isWatched(id: String): Boolean {
        return watchlistDao.exists(id)
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

    // ---- Favourites ----

    override fun getFavouritesFlow(): Flow<List<MediaItem>> {
        return favouriteDao.getAllFlow().map { entities ->
            entities.map { it.toMediaItem() }
        }
    }

    override fun getFavouritesByTypeFlow(mediaType: MediaType): Flow<List<MediaItem>> {
        return favouriteDao.getByTypeFlow(mediaType.name).map { entities ->
            entities.map { it.toMediaItem() }
        }
    }

    override suspend fun toggleFavourite(item: MediaItem): Boolean {
        val exists = favouriteDao.exists(item.id, item.mediaType.name)
        if (exists) {
            favouriteDao.delete(item.id, item.mediaType.name)
            return false
        } else {
            favouriteDao.insert(item.toFavouriteEntity())
            return true
        }
    }

    override suspend fun isFavourite(mediaId: String, mediaType: MediaType): Boolean {
        return favouriteDao.exists(mediaId, mediaType.name)
    }

    // ---- Custom Lists ----

    override fun getListsFlow(): Flow<List<Pair<String, String>>> {
        return listDao.getAllListsFlow().map { lists ->
            lists.map { it.id to it.name }
        }
    }

    override suspend fun createList(name: String) {
        listDao.createList(ListEntity(id = UUID.randomUUID().toString(), name = name))
    }

    override suspend fun deleteList(listId: String) {
        listDao.deleteListItems(listId)
        listDao.deleteList(listId)
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

    override fun getListItemsFlow(listId: String): Flow<List<MediaItem>> {
        return listDao.getListItemsFlow(listId).map { entities ->
            entities.map { it.toMediaItem() }
        }
    }

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

        val cert = when (type) {
            MediaType.MOVIE -> {
                releaseDates?.results?.find { it.iso31661 == "US" }?.releaseDates?.firstOrNull { it.certification.isNotBlank() }?.certification
            }
            MediaType.TV -> {
                contentRatings?.results?.find { it.iso31661 == "US" }?.rating
            }
        } ?: "-"

        return MovieDetails(
            id = id.toString(),
            title = displayTitle,
            overview = overview ?: "",
            posterPath = posterUrl,
            backdropPath = backdropUrl,
            rating = voteAverage?.let { Math.round(it * 10) / 10.0 } ?: 0.0,
            releaseDate = rawDate,
            runtime = runtimeStr,
            certification = cert,
            director = directorNames,
            mediaType = type,
            cast = castMembers,
            ratings = listOf(
                RatingSource("TMDb", String.format("%.1f", voteAverage ?: 0.0))
            ),
            recommendations = recommendations?.results?.take(15)?.map { it.toMediaItem(type) } ?: emptyList()
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
        isWatched = isWatched
    )

    private fun MediaItem.toEntity(): WatchlistEntity = WatchlistEntity(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        rating = rating,
        releaseDate = releaseDate,
        mediaType = mediaType.name,
        isWatched = isWatched
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
        id = "${mediaType.name}_$id",
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
        id = "${listId}_${mediaType.name}_$id",
        listId = listId,
        mediaId = id,
        mediaType = mediaType.name,
        title = title,
        posterPath = posterPath,
        backdropPath = backdropPath
    )

    // ---- Custom poster/backdrop persistence ----

    override suspend fun saveCustomPoster(id: String, url: String?) {
        val existing = customImageDao.getBackdrop(id)
        if (customImageDao.exists(id)) {
            customImageDao.updatePoster(id, url)
        } else {
            customImageDao.upsert(CustomImageEntity(mediaId = id, mediaType = "", customPosterPath = url))
        }
    }

    override suspend fun saveCustomBackdrop(id: String, url: String?) {
        if (customImageDao.exists(id)) {
            customImageDao.updateBackdrop(id, url)
        } else {
            customImageDao.upsert(CustomImageEntity(mediaId = id, mediaType = "", customBackdropPath = url))
        }
    }

    override suspend fun getCustomPoster(id: String): String? {
        return customImageDao.getPoster(id)
    }

    override suspend fun getCustomBackdrop(id: String): String? {
        return customImageDao.getBackdrop(id)
    }
}
