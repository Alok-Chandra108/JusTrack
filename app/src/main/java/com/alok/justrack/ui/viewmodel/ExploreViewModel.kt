package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.api.TmdbApiService
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.alok.justrack.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ExploreUiState {
    object Loading : ExploreUiState()
    data class Success(
        val bannerItems: List<MediaItem> = emptyList(),
        val trending: List<MediaItem> = emptyList(),
        val popularMovies: List<MediaItem> = emptyList(),
        val popularTv: List<MediaItem> = emptyList(),
        val topRatedMovies: List<MediaItem> = emptyList(),
        val topRatedTv: List<MediaItem> = emptyList(),
        val upcomingMovies: List<MediaItem> = emptyList(),
        val onTheAirTv: List<MediaItem> = emptyList(),
        val genres: List<Genre> = emptyList(),
        val genreResults: List<MediaItem> = emptyList(),
        val selectedGenre: Genre? = null
    ) : ExploreUiState()
    data class Error(val message: String) : ExploreUiState()
}

sealed class ExploreSearchUiState {
    object Idle : ExploreSearchUiState()
    object Searching : ExploreSearchUiState()
    data class Results(val items: List<MediaItem>) : ExploreSearchUiState()
    data class Error(val message: String) : ExploreSearchUiState()
}

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val apiService: TmdbApiService,
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow<ExploreSearchUiState>(ExploreSearchUiState.Idle)
    val searchState: StateFlow<ExploreSearchUiState> = _searchState.asStateFlow()

    private val bannerCache = mutableMapOf<String, List<MediaItem>>()
    private val sectionCache = mutableMapOf<String, List<MediaItem>>()
    private val genreCache = mutableMapOf<Int, List<MediaItem>>()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val trending = loadWithCache("trending") { fetchTrending() }
                val genres = loadGenres()

                _uiState.value = ExploreUiState.Success(
                    bannerItems = trending.take(10),
                    trending = trending,
                    genres = genres
                )
            } catch (e: Exception) {
                _uiState.value = ExploreUiState.Error(e.message ?: "Failed to load explore data")
            }
        }
    }

    fun loadSection(sectionName: String) {
        val currentState = _uiState.value as? ExploreUiState.Success ?: return
        if (isSectionLoaded(sectionName, currentState)) return

        viewModelScope.launch {
            try {
                val items = loadWithCache(sectionName) {
                    when (sectionName) {
                        "popular_movies" -> fetchPopularMovies()
                        "popular_tv" -> fetchPopularTv()
                        "top_rated_movies" -> fetchTopRatedMovies()
                        "top_rated_tv" -> fetchTopRatedTv()
                        "upcoming_movies" -> fetchUpcomingMovies()
                        "on_the_air_tv" -> fetchOnTheAirTv()
                        else -> emptyList()
                    }
                }
                _uiState.value = when (sectionName) {
                    "popular_movies" -> currentState.copy(popularMovies = items)
                    "popular_tv" -> currentState.copy(popularTv = items)
                    "top_rated_movies" -> currentState.copy(topRatedMovies = items)
                    "top_rated_tv" -> currentState.copy(topRatedTv = items)
                    "upcoming_movies" -> currentState.copy(upcomingMovies = items)
                    "on_the_air_tv" -> currentState.copy(onTheAirTv = items)
                    else -> currentState
                }
            } catch (e: Exception) {
                // Keep existing state on error for individual sections
            }
        }
    }

    private fun isSectionLoaded(sectionName: String, state: ExploreUiState.Success): Boolean {
        return when (sectionName) {
            "popular_movies" -> state.popularMovies.isNotEmpty()
            "popular_tv" -> state.popularTv.isNotEmpty()
            "top_rated_movies" -> state.topRatedMovies.isNotEmpty()
            "top_rated_tv" -> state.topRatedTv.isNotEmpty()
            "upcoming_movies" -> state.upcomingMovies.isNotEmpty()
            "on_the_air_tv" -> state.onTheAirTv.isNotEmpty()
            else -> false
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchState.value = ExploreSearchUiState.Idle
            return
        }
        viewModelScope.launch {
            _searchState.value = ExploreSearchUiState.Searching
            try {
                val response = apiService.searchMulti(query)
                val items = response.results.mapNotNull { dto ->
                    val mediaType = when {
                        dto.mediaType == "movie" -> MediaType.MOVIE
                        dto.mediaType == "tv" -> MediaType.TV
                        dto.name != null && dto.title == null -> MediaType.TV
                        dto.name != null && dto.firstAirDate != null -> MediaType.TV
                        dto.title != null -> MediaType.MOVIE
                        else -> return@mapNotNull null
                    }
                    MediaItem(
                        id = dto.id.toString(),
                        title = dto.title ?: dto.name ?: "",
                        overview = dto.overview ?: "",
                        posterPath = dto.posterPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" },
                        backdropPath = dto.backdropPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W780}$it" },
                        rating = dto.voteAverage ?: 0.0,
                        releaseDate = dto.releaseDate ?: dto.firstAirDate ?: "",
                        mediaType = mediaType
                    )
                }
                _searchState.value = ExploreSearchUiState.Results(items)
            } catch (e: Exception) {
                _searchState.value = ExploreSearchUiState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun clearSearch() {
        _searchState.value = ExploreSearchUiState.Idle
    }

    fun selectGenre(genre: Genre) {
        val currentState = _uiState.value as? ExploreUiState.Success ?: return
        _uiState.value = currentState.copy(selectedGenre = genre)

        viewModelScope.launch {
            try {
                val items = loadWithCache("genre_${genre.id}") {
                    val movieResults = apiService.discoverMoviesByGenre(genre.id).results
                    val tvResults = apiService.discoverTvByGenre(genre.id).results
                    (movieResults + tvResults).mapNotNull { dto ->
                        val mediaType = when (dto.mediaType) {
                            "movie" -> MediaType.MOVIE
                            "tv" -> MediaType.TV
                            else -> return@mapNotNull null
                        }
                        MediaItem(
                            id = dto.id.toString(),
                            title = dto.title ?: dto.name ?: "",
                            overview = dto.overview ?: "",
                            posterPath = dto.posterPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" },
                            backdropPath = dto.backdropPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W780}$it" },
                            rating = dto.voteAverage ?: 0.0,
                            releaseDate = dto.releaseDate ?: dto.firstAirDate ?: "",
                            mediaType = mediaType
                        )
                    }.sortedByDescending { it.rating }
                }
                _uiState.value = currentState.copy(genreResults = items, selectedGenre = genre)
            } catch (e: Exception) {
                // Keep existing state on error
            }
        }
    }

    fun clearGenreSelection() {
        val currentState = _uiState.value as? ExploreUiState.Success ?: return
        _uiState.value = currentState.copy(selectedGenre = null, genreResults = emptyList())
    }

    fun addToWatchlist(item: MediaItem) {
        viewModelScope.launch { repository.addToWatchlist(item) }
    }

    fun toggleWatched(item: MediaItem) {
        viewModelScope.launch {
            val isCurrentlyWatched = repository.isWatched(item.id)
            repository.setWatched(item, !isCurrentlyWatched)
        }
    }

    fun removeFromWatchlist(id: String) {
        viewModelScope.launch { repository.removeFromWatchlist(id) }
    }

    suspend fun isInWatchlist(id: String): Boolean = repository.isInWatchlist(id)

    suspend fun isWatched(id: String): Boolean = repository.isWatched(id)

    fun toggleFavourite(item: MediaItem) {
        viewModelScope.launch { repository.toggleFavourite(item) }
    }

    suspend fun isFavourite(mediaId: String, mediaType: MediaType): Boolean =
        repository.isFavourite(mediaId, mediaType)

    fun createList(name: String) {
        viewModelScope.launch { repository.createList(name) }
    }

    fun addToList(listId: String, item: MediaItem) {
        viewModelScope.launch { repository.addToList(listId, item) }
    }

    fun getLists() = repository.getListsFlow()

    private suspend fun loadWithCache(key: String, fetcher: suspend () -> List<MediaItem>): List<MediaItem> {
        val cached = sectionCache[key]
        if (cached != null) return cached
        val result = fetcher()
        sectionCache[key] = result
        return result
    }

    private suspend fun loadGenres(): List<Genre> {
        return try {
            val movieGenres = apiService.getMovieGenres().genres
            val tvGenres = apiService.getTvGenres().genres
            val allGenres = (movieGenres + tvGenres).distinctBy { it.id }.sortedBy { it.name }
            allGenres.map { Genre(it.id, it.name) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchTrending(): List<MediaItem> {
        val response = apiService.getTrending()
        return response.results.map { dto ->
            val mediaType = when {
                dto.mediaType == "movie" -> MediaType.MOVIE
                dto.mediaType == "tv" -> MediaType.TV
                dto.name != null && dto.title == null -> MediaType.TV
                dto.name != null && dto.firstAirDate != null -> MediaType.TV
                else -> MediaType.MOVIE
            }
            MediaItem(
                id = dto.id.toString(),
                title = dto.title ?: dto.name ?: "",
                overview = dto.overview ?: "",
                posterPath = dto.posterPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" },
                backdropPath = dto.backdropPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W780}$it" },
                rating = dto.voteAverage ?: 0.0,
                releaseDate = dto.releaseDate ?: dto.firstAirDate ?: "",
                mediaType = mediaType
            )
        }
    }

    private suspend fun fetchPopularMovies(): List<MediaItem> {
        return apiService.getPopularMovies().results.map { dto ->
            MediaItem(
                id = dto.id.toString(),
                title = dto.title ?: dto.name ?: "",
                overview = dto.overview ?: "",
                posterPath = dto.posterPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" },
                backdropPath = dto.backdropPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W780}$it" },
                rating = dto.voteAverage ?: 0.0,
                releaseDate = dto.releaseDate ?: "",
                mediaType = MediaType.MOVIE
            )
        }
    }

    private suspend fun fetchPopularTv(): List<MediaItem> {
        return apiService.getPopularTv().results.map { dto ->
            MediaItem(
                id = dto.id.toString(),
                title = dto.name ?: dto.title ?: "",
                overview = dto.overview ?: "",
                posterPath = dto.posterPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" },
                backdropPath = dto.backdropPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W780}$it" },
                rating = dto.voteAverage ?: 0.0,
                releaseDate = dto.firstAirDate ?: "",
                mediaType = MediaType.TV
            )
        }
    }

    private suspend fun fetchTopRatedMovies(): List<MediaItem> {
        return apiService.getTopRatedMovies().results.map { dto ->
            MediaItem(
                id = dto.id.toString(),
                title = dto.title ?: dto.name ?: "",
                overview = dto.overview ?: "",
                posterPath = dto.posterPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" },
                backdropPath = dto.backdropPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W780}$it" },
                rating = dto.voteAverage ?: 0.0,
                releaseDate = dto.releaseDate ?: "",
                mediaType = MediaType.MOVIE
            )
        }
    }

    private suspend fun fetchTopRatedTv(): List<MediaItem> {
        return apiService.getTopRatedTv().results.map { dto ->
            MediaItem(
                id = dto.id.toString(),
                title = dto.name ?: dto.title ?: "",
                overview = dto.overview ?: "",
                posterPath = dto.posterPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" },
                backdropPath = dto.backdropPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W780}$it" },
                rating = dto.voteAverage ?: 0.0,
                releaseDate = dto.firstAirDate ?: "",
                mediaType = MediaType.TV
            )
        }
    }

    private suspend fun fetchUpcomingMovies(): List<MediaItem> {
        return apiService.getUpcomingMovies().results.map { dto ->
            MediaItem(
                id = dto.id.toString(),
                title = dto.title ?: dto.name ?: "",
                overview = dto.overview ?: "",
                posterPath = dto.posterPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" },
                backdropPath = dto.backdropPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W780}$it" },
                rating = dto.voteAverage ?: 0.0,
                releaseDate = dto.releaseDate ?: "",
                mediaType = MediaType.MOVIE
            )
        }
    }

    private suspend fun fetchOnTheAirTv(): List<MediaItem> {
        return apiService.getOnTheAirTv().results.map { dto ->
            MediaItem(
                id = dto.id.toString(),
                title = dto.name ?: dto.title ?: "",
                overview = dto.overview ?: "",
                posterPath = dto.posterPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W500}$it" },
                backdropPath = dto.backdropPath?.let { "${Constants.TMDB_IMAGE_BASE_URL_W780}$it" },
                rating = dto.voteAverage ?: 0.0,
                releaseDate = dto.firstAirDate ?: "",
                mediaType = MediaType.TV
            )
        }
    }
}
