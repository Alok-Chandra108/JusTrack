package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.model.StatsData
import com.alok.justrack.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    // Existing UI state for backward compatibility (used by MoviesScreen etc.)
    val uiState: StateFlow<WatchlistUiState> = repository
        .getWatchlistFlow()
        .map { WatchlistUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WatchlistUiState.Loading
        )

    // Expose raw list of watchlist items (starts empty, updates when data arrives)
    val watchlistItems: StateFlow<List<MediaItem>> = uiState
        .map { if (it is WatchlistUiState.Success) it.items else emptyList() }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Favorite items
    val favorites: StateFlow<List<MediaItem>> = repository
        .getFavouritesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Stats derived from watchlist
    val stats: StateFlow<StatsData?> = watchlistItems.map { items ->
        val total = items.size
        val watchedMovies = items.count { it.mediaType == MediaType.MOVIE && it.isWatched }
        val watchedShows = items.count { it.mediaType == MediaType.TV && it.isWatched }
        val avgRating = if (total > 0) {
            Math.round(items.map { it.rating * 10 }.average()) / 10.0
        } else 0.0
        val topTitle = items.maxByOrNull { it.rating }?.title ?: "-"
        StatsData(total, watchedMovies, watchedShows, avgRating, topTitle)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Watched Content
    val watchedMovies: StateFlow<List<MediaItem>> = watchlistItems.map { items ->
        items.filter { it.mediaType == MediaType.MOVIE && it.isWatched }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchedShows: StateFlow<List<MediaItem>> = watchlistItems.map { items ->
        items.filter { it.mediaType == MediaType.TV && it.isWatched }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorites by type
    val favoriteMovies: StateFlow<List<MediaItem>> = favorites.map { items ->
        items.filter { it.mediaType == MediaType.MOVIE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteShows: StateFlow<List<MediaItem>> = favorites.map { items ->
        items.filter { it.mediaType == MediaType.TV }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User‑created lists
    val lists: StateFlow<List<Pair<String, String>>> = repository
        .getListsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Lists with previews
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val listsWithPreviews: StateFlow<List<Pair<String, List<MediaItem>>>> = lists.flatMapLatest { listPairs ->
        if (listPairs.isEmpty()) flowOf(emptyList())
        else {
            val flows = listPairs.map { (id, name) ->
                repository.getListItemsFlow(id).map { items -> name to items }
            }
            combine(flows) { it.toList() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recently watched (watched items, most recent first – using list order as proxy)
    val recentlyWatched: StateFlow<List<MediaItem>> = watchlistItems.map { items ->
        items.filter { it.isWatched }
            // Reverse list to show newest first (assuming added order is preserved)
            .reversed()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Watchlist items (explicitly filtered by inWatchlist flag)
    val explicitWatchlistItems: StateFlow<List<MediaItem>> = watchlistItems.map { items ->
        items.filter { it.inWatchlist }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

}

sealed class WatchlistUiState {
    object Loading : WatchlistUiState()
    data class Success(val items: List<MediaItem>) : WatchlistUiState()
    data class Error(val message: String) : WatchlistUiState()
}
