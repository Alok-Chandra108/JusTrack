package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.domain.repository.ExploreRepository
import com.alok.justrack.data.repository.MediaRepository
import com.alok.justrack.domain.usecase.SearchMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    private val exploreRepository: ExploreRepository,
    private val repository: MediaRepository,
    private val searchMediaUseCase: SearchMediaUseCase
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

    fun loadSection(section: ExploreSection) {
        val currentState = _internalUiState.value as? ExploreUiState.Success ?: return
        if (isSectionLoaded(section, currentState)) return

        viewModelScope.launch {
            try {
                val items = loadWithCache(section.key) {
                    when (section) {
                        ExploreSection.POPULAR_MOVIES -> exploreRepository.getPopularMovies()
                        ExploreSection.POPULAR_TV -> exploreRepository.getPopularTv()
                        ExploreSection.TOP_RATED_MOVIES -> exploreRepository.getTopRatedMovies()
                        ExploreSection.TOP_RATED_TV -> exploreRepository.getTopRatedTv()
                        ExploreSection.UPCOMING_MOVIES -> exploreRepository.getUpcomingMovies()
                        ExploreSection.ON_THE_AIR_TV -> exploreRepository.getOnTheAirTv()
                    }
                }
                _internalUiState.value = when (section) {
                    ExploreSection.POPULAR_MOVIES -> currentState.copy(popularMovies = items)
                    ExploreSection.POPULAR_TV -> currentState.copy(popularTv = items)
                    ExploreSection.TOP_RATED_MOVIES -> currentState.copy(topRatedMovies = items)
                    ExploreSection.TOP_RATED_TV -> currentState.copy(topRatedTv = items)
                    ExploreSection.UPCOMING_MOVIES -> currentState.copy(upcomingMovies = items)
                    ExploreSection.ON_THE_AIR_TV -> currentState.copy(onTheAirTv = items)
                }
            } catch (e: Exception) {
                // Keep existing state on error for individual sections
            }
        }
    }

    private fun isSectionLoaded(section: ExploreSection, state: ExploreUiState.Success): Boolean {
        return when (section) {
            ExploreSection.POPULAR_MOVIES -> state.popularMovies.isNotEmpty()
            ExploreSection.POPULAR_TV -> state.popularTv.isNotEmpty()
            ExploreSection.TOP_RATED_MOVIES -> state.topRatedMovies.isNotEmpty()
            ExploreSection.TOP_RATED_TV -> state.topRatedTv.isNotEmpty()
            ExploreSection.UPCOMING_MOVIES -> state.upcomingMovies.isNotEmpty()
            ExploreSection.ON_THE_AIR_TV -> state.onTheAirTv.isNotEmpty()
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private suspend fun performSearch(query: String) = coroutineScope {
        _internalSearchState.value = ExploreSearchUiState.Searching
        
        try {
            val finalItems = searchMediaUseCase(query)
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
                    exploreRepository.discoverByGenre(genre.id)
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
        return exploreRepository.getGenres()
    }

    private suspend fun fetchTrending(): List<MediaItem> {
        return exploreRepository.getTrending()
    }

    private suspend fun fetchTrendingIndia(): List<MediaItem> {
        return exploreRepository.getTrendingIndia()
    }
}
