package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.api.TmdbApiService
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.mapper.TmdbMapper.toMediaItem
import com.alok.justrack.data.repository.MediaRepository
import com.alok.justrack.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

sealed class ExploreUiState {
    object Loading : ExploreUiState()
    data class Success(
        val bannerItems: List<MediaItem> = emptyList(),
        val trending: List<MediaItem> = emptyList(),
        val trendingIndia: List<MediaItem> = emptyList(),
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

    private val _internalUiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val uiState: StateFlow<ExploreUiState> = combine(_internalUiState, repository.getWatchlistFlow()) { state, watchlist ->
        updateStateWithWatchlist(state, watchlist)
    }.stateIn(viewModelScope, SharingStarted.Lazily, ExploreUiState.Loading)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _internalSearchState = MutableStateFlow<ExploreSearchUiState>(ExploreSearchUiState.Idle)
    val searchState: StateFlow<ExploreSearchUiState> = combine(_internalSearchState, repository.getWatchlistFlow()) { state, watchlist ->
        updateSearchStateWithWatchlist(state, watchlist)
    }.stateIn(viewModelScope, SharingStarted.Lazily, ExploreSearchUiState.Idle)

    private val bannerCache = mutableMapOf<String, List<MediaItem>>()
    private val sectionCache = mutableMapOf<String, List<MediaItem>>()
    private val genreCache = mutableMapOf<Int, List<MediaItem>>()

    init {
        loadInitialData()
        setupSearchDebounce()
    }

    fun retry() {
        loadInitialData()
    }

    private fun setupSearchDebounce() {
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= 2) {
                        performSearch(query)
                    } else if (query.isEmpty()) {
                        clearSearch()
                    }
                }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val trending = loadWithCache("trending") { fetchTrending() }
                val trendingIndia = loadWithCache("trending_india") { fetchTrendingIndia() }
                val genres = loadGenres()

                _internalUiState.value = ExploreUiState.Success(
                    bannerItems = trending.take(10),
                    trending = trending,
                    trendingIndia = trendingIndia,
                    genres = genres
                )
            } catch (e: Exception) {
                _internalUiState.value = ExploreUiState.Error(e.message ?: "Failed to load explore data")
            }
        }
    }

    private suspend fun fetchTrendingIndia(): List<MediaItem> {
        return try {
            val movies = apiService.discoverMovies(region = "IN", sortBy = "popularity.desc").results
            movies.map { it.toMediaItem(MediaType.MOVIE) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadSection(sectionName: String) {
        val currentState = _internalUiState.value as? ExploreUiState.Success ?: return
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
                _internalUiState.value = when (sectionName) {
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

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private suspend fun performSearch(query: String) = coroutineScope {
        _internalSearchState.value = ExploreSearchUiState.Searching
        
        // 1. Extract potential year (e.g., "Welcome 2007" -> "Welcome", 2007)
        val yearRegex = Regex("\\b(19|20)\\d{2}\\b")
        val yearMatch = yearRegex.find(query)
        val year = yearMatch?.value?.toIntOrNull()
        val cleanQuery = if (year != null) query.replace(yearMatch.value, "").trim() else query

        try {
            // 2. Parallel Targeted Searching
            val multiGlobal = async { 
                apiService.searchMulti(query = query, includeAdult = false).results 
            }
            
            val multiIndia = async { 
                apiService.searchMulti(query = query, includeAdult = false, region = "IN").results 
            }

            val movieYear = if (year != null && cleanQuery.isNotBlank()) async {
                apiService.searchMovie(query = cleanQuery, year = year, includeAdult = false).results
            } else null

            val tvYear = if (year != null && cleanQuery.isNotBlank()) async {
                apiService.searchTv(query = cleanQuery, year = year, includeAdult = false).results
            } else null

            // 3. Collect Results
            val results = mutableListOf<MediaItem>()
            
            // Prioritize year matches if searched with year
            movieYear?.await()?.let { results.addAll(it.map { dto -> dto.toMediaItem(MediaType.MOVIE) }) }
            tvYear?.await()?.let { results.addAll(it.map { dto -> dto.toMediaItem(MediaType.TV) }) }
            
            // Add India targeted and Global results
            results.addAll(multiIndia.await().map { it.toMediaItem() })
            results.addAll(multiGlobal.await().map { it.toMediaItem() })

            // 4. Smart Deduplication & Ranking
            val finalItems = results
                .distinctBy { it.id + it.mediaType.name }
                .take(40)

            _internalSearchState.value = ExploreSearchUiState.Results(finalItems)
        } catch (e: Exception) {
            _internalSearchState.value = ExploreSearchUiState.Error(e.message ?: "Search failed")
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _internalSearchState.value = ExploreSearchUiState.Idle
    }

    fun selectGenre(genre: Genre) {
        val currentState = _internalUiState.value as? ExploreUiState.Success ?: return
        _internalUiState.value = currentState.copy(selectedGenre = genre)

        viewModelScope.launch {
            try {
                val items = loadWithCache("genre_${genre.id}") {
                    val movieResults = apiService.discoverMoviesByGenre(genre.id).results
                    val tvResults = apiService.discoverTvByGenre(genre.id).results
                    (movieResults + tvResults)
                        .map { it.toMediaItem() }
                        .sortedByDescending { it.rating }
                }
                _internalUiState.value = currentState.copy(genreResults = items, selectedGenre = genre)
            } catch (e: Exception) {
                // Keep existing state on error
            }
        }
    }

    fun clearGenreSelection() {
        val currentState = _internalUiState.value as? ExploreUiState.Success ?: return
        _internalUiState.value = currentState.copy(selectedGenre = null, genreResults = emptyList())
    }

    fun addToWatchlist(item: MediaItem) {
        viewModelScope.launch { repository.addToWatchlist(item) }
    }

    fun toggleWatched(item: MediaItem) {
        viewModelScope.launch {
            val isCurrentlyWatched = repository.isWatched(item.id)
            val markingAsWatched = !isCurrentlyWatched
            
            if (markingAsWatched) {
                // Fetch full details to get runtime before marking as watched
                try {
                    val details = repository.getMediaDetail(item.id, item.mediaType)
                    if (details != null) {
                        repository.setWatched(item.copy(runtime = details.runtimeInt), true)
                    } else {
                        repository.setWatched(item, true)
                    }
                } catch (e: Exception) {
                    repository.setWatched(item, true)
                }
            } else {
                repository.setWatched(item, false)
            }
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

    private fun updateStateWithWatchlist(state: ExploreUiState, watchlist: List<MediaItem>): ExploreUiState {
        return if (state is ExploreUiState.Success) {
            state.copy(
                bannerItems = state.bannerItems.map { it.syncWithWatchlist(watchlist) },
                trending = state.trending.map { it.syncWithWatchlist(watchlist) },
                popularMovies = state.popularMovies.map { it.syncWithWatchlist(watchlist) },
                popularTv = state.popularTv.map { it.syncWithWatchlist(watchlist) },
                topRatedMovies = state.topRatedMovies.map { it.syncWithWatchlist(watchlist) },
                topRatedTv = state.topRatedTv.map { it.syncWithWatchlist(watchlist) },
                upcomingMovies = state.upcomingMovies.map { it.syncWithWatchlist(watchlist) },
                onTheAirTv = state.onTheAirTv.map { it.syncWithWatchlist(watchlist) },
                genreResults = state.genreResults.map { it.syncWithWatchlist(watchlist) }
            )
        } else state
    }

    private fun updateSearchStateWithWatchlist(state: ExploreSearchUiState, watchlist: List<MediaItem>): ExploreSearchUiState {
        return if (state is ExploreSearchUiState.Results) {
            state.copy(items = state.items.map { it.syncWithWatchlist(watchlist) })
        } else state
    }

    private fun MediaItem.syncWithWatchlist(watchlist: List<MediaItem>): MediaItem {
        val localItem = watchlist.find { it.id == this.id && it.mediaType == this.mediaType }
        return if (localItem != null) {
            this.copy(isWatched = localItem.isWatched, inWatchlist = localItem.inWatchlist)
        } else {
            this.copy(isWatched = false, inWatchlist = false)
        }
    }

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
        return response.results.map { it.toMediaItem() }
    }

    private suspend fun fetchPopularMovies(): List<MediaItem> {
        return apiService.getPopularMovies().results.map { it.toMediaItem(MediaType.MOVIE) }
    }

    private suspend fun fetchPopularTv(): List<MediaItem> {
        return apiService.getPopularTv().results.map { it.toMediaItem(MediaType.TV) }
    }

    private suspend fun fetchTopRatedMovies(): List<MediaItem> {
        return apiService.getTopRatedMovies().results.map { it.toMediaItem(MediaType.MOVIE) }
    }

    private suspend fun fetchTopRatedTv(): List<MediaItem> {
        return apiService.getTopRatedTv().results.map { it.toMediaItem(MediaType.TV) }
    }

    private suspend fun fetchUpcomingMovies(): List<MediaItem> = coroutineScope {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val today = LocalDate.now()

        try {
            // Fetch Global Hyped/Buzzing Upcoming
            val globalDeferred = async {
                apiService.discoverMovies(
                    sortBy = "popularity.desc",
                    releaseDateGte = tomorrow,
                    includeAdult = false
                ).results.map { it.toMediaItem(MediaType.MOVIE) }
            }

            // Fetch Indian Hyped/Buzzing Upcoming
            val indianLanguages = "hi|te|ta|ml|kn"
            val indianDeferred = async {
                apiService.discoverMovies(
                    sortBy = "popularity.desc",
                    releaseDateGte = tomorrow,
                    includeAdult = false,
                    region = "IN",
                    originalLanguage = indianLanguages
                ).results.map { it.toMediaItem(MediaType.MOVIE) }
            }

            val globalResults = try { globalDeferred.await() } catch (e: Exception) { emptyList() }
            val indianResults = try { indianDeferred.await() } catch (e: Exception) { emptyList() }

            (globalResults + indianResults)
                .distinctBy { it.id }
                .filter { item ->
                    val releaseDate = DateUtils.parseDate(item.releaseDate)
                    releaseDate != null && releaseDate.isAfter(today)
                }
                .sortedBy { DateUtils.parseDate(it.releaseDate) }
        } catch (e: Exception) {
            // Fallback to standard upcoming if discover fails
            apiService.getUpcomingMovies().results
                .map { it.toMediaItem(MediaType.MOVIE) }
                .filter { item ->
                    val releaseDate = DateUtils.parseDate(item.releaseDate)
                    releaseDate != null && releaseDate.isAfter(today)
                }
                .sortedBy { DateUtils.parseDate(it.releaseDate) }
        }
    }

    private suspend fun fetchOnTheAirTv(): List<MediaItem> {
        return apiService.getOnTheAirTv().results.map { it.toMediaItem(MediaType.TV) }
    }
}
