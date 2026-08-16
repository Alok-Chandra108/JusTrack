package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.model.Episode
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.model.StatsData
import com.alok.justrack.data.repository.MediaRepository
import com.alok.justrack.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    init {
        syncMissingRuntimes()
    }

    val showCompletionEvents: Flow<String> = repository.showCompletionEvents

    // Existing UI state for backward compatibility (used by MoviesScreen etc.)
    val uiState: StateFlow<WatchlistUiState> = repository
        .getWatchlistFlow()
        .map { WatchlistUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = WatchlistUiState.Loading
        )

    // Expose raw list of watchlist items (starts empty, updates when data arrives)
    val watchlistItems: StateFlow<List<MediaItem>> = uiState
        .map { if (it is WatchlistUiState.Success) it.items else emptyList() }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // Favorite items
    val favorites: StateFlow<List<MediaItem>> = repository
        .getFavouritesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // Stats derived from watchlist
    val stats: StateFlow<StatsData?> = combine(
        watchlistItems,
        repository.getTotalWatchedTvRuntimeFlow(),
        repository.getTotalWatchedEpisodeCountFlow(),
        repository.getTotalWatchedMovieRuntimeFlow(),
        repository.getTotalWatchedMovieCountFlow()
    ) { items, tvMinutes, epCount, movieMinutes, movieCount ->
        val total = items.size
        val avgRating = if (total > 0) {
            Math.round(items.map { it.rating * 10 }.average()) / 10.0
        } else 0.0
        val topTitle = items.maxByOrNull { it.rating }?.title ?: "-"
        
        StatsData(
            totalItems = total,
            movieCount = items.count { it.mediaType == MediaType.MOVIE && it.isWatched },
            tvCount = items.count { it.mediaType == MediaType.TV && it.isWatched },
            averageRating = avgRating,
            topRatedTitle = topTitle,
            showWatchTime = DateUtils.toWatchTime(tvMinutes),
            episodesWatched = epCount,
            movieWatchTime = DateUtils.toWatchTime(movieMinutes),
            moviesWatched = movieCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = null
    )

    // Watched Content
    val watchedMovies: StateFlow<List<MediaItem>> = watchlistItems.map { items ->
        items.filter { it.mediaType == MediaType.MOVIE && it.isWatched }
            .sortedByDescending { it.addedAt }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val watchedShows: StateFlow<List<MediaItem>> = combine(
        watchlistItems,
        repository.episodesUpdateEvents.onStart { emit(Unit) }
    ) { items, _ -> items }
        .flatMapLatest { items ->
            val tvShows = items.filter { it.mediaType == MediaType.TV }
            flow {
                val result = tvShows.mapNotNull { show ->
                    val watchedCount = repository.getWatchedEpisodeCount(show.id)
                    if (show.isWatched || watchedCount > 0) {
                        val latestActivity = repository.getLatestWatchActivity(show.id) ?: show.addedAt
                        show to latestActivity
                    } else null
                }.sortedByDescending { it.second }
                .map { it.first }
                emit(result)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Favorites by type
    val favoriteMovies: StateFlow<List<MediaItem>> = favorites.map { items ->
        items.filter { it.mediaType == MediaType.MOVIE }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteShows: StateFlow<List<MediaItem>> = favorites.map { items ->
        items.filter { it.mediaType == MediaType.TV }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // User‑created lists
    val lists: StateFlow<List<Pair<String, String>>> = repository
        .getListsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // Lists with previews
    @OptIn(ExperimentalCoroutinesApi::class)
    val listsWithPreviews: StateFlow<List<Pair<String, List<MediaItem>>>> = lists.flatMapLatest { listPairs ->
        if (listPairs.isEmpty()) flowOf(emptyList())
        else {
            val flows = listPairs.map { (id, name) ->
                repository.getListItemsFlow(id).map { items -> name to items }
            }
            combine(flows) { it.toList() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Recently watched (watched items, most recent first)
    val recentlyWatched: StateFlow<List<MediaItem>> = watchlistItems.map { items ->
        items.filter { it.isWatched }
            .sortedByDescending { it.addedAt }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Watchlist items (explicitly filtered by inWatchlist flag and null-safe)
    val explicitWatchlistItems: StateFlow<List<MediaItem>> = watchlistItems.map { items -> 
        items.filterNotNull()
            .filter { it.inWatchlist }
    }.onEach { items ->
        // Trigger sync for TV shows that have no episodes in DB yet
        items.filter { it.mediaType == MediaType.TV }.forEach { show ->
            viewModelScope.launch {
                if (repository.getTotalEpisodeCount(show.id) == 0) {
                    repository.syncEpisodes(show.id)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // --- Episode Tracking for Shows ---

    // Data classes for episode items
    data class WatchlistEpisodeItem(
        val showId: String,
        val showName: String,
        val showPosterPath: String?,
        val episode: Episode,
        val isPremiere: Boolean,
        val isFinale: Boolean,
        val isNew: Boolean,
        val remainingCount: Int,
        val watchedCount: Int,
        val totalCount: Int,
        val isSyncing: Boolean = false,
        val isWatchLater: Boolean = false
    )

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _isMovieGridView = MutableStateFlow(true)
    val isMovieGridView: StateFlow<Boolean> = _isMovieGridView.asStateFlow()

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun toggleMovieGridView() {
        _isMovieGridView.value = !_isMovieGridView.value
    }

    // Grouped upcoming movies for the UI
    val groupedUpcomingMovies = explicitWatchlistItems.map { items ->
        val today = LocalDate.now()
        val movies = items.filter { it.mediaType == MediaType.MOVIE }
        
        movies.mapNotNull { movie ->
            val releaseDate = DateUtils.parseDate(movie.releaseDate)
            if (releaseDate != null && !releaseDate.isBefore(today)) {
                movie to releaseDate
            } else null
        }.sortedBy { it.second }
        .groupBy { (movie, releaseDate) ->
            val daysAway = ChronoUnit.DAYS.between(today, releaseDate)
            when {
                daysAway == 0L -> "TODAY"
                daysAway in 1..6 -> "THIS WEEK"
                daysAway in 7..13 -> "NEXT WEEK"
                releaseDate.month == today.month && releaseDate.year == today.year -> "THIS MONTH"
                releaseDate.month == today.plusMonths(1).month && releaseDate.year == today.plusMonths(1).year -> "NEXT MONTH"
                else -> "LATER"
            }
        }.mapValues { it.value.map { pair -> pair.first } }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    data class UpcomingEpisodeItem(
        val showId: String,
        val showName: String,
        val showPosterPath: String?,
        val episode: Episode,
        val daysAway: Long? // Negative = aired, 0 = today, positive = future days
    )

    // Watchlist tab: shows next unwatched episode for each show in watchlist
    @OptIn(ExperimentalCoroutinesApi::class)
    val watchlistEpisodes: StateFlow<List<WatchlistEpisodeItem>> = combine(
        explicitWatchlistItems,
        repository.getAllWatchedEpisodesFlow()
    ) { items, _ -> items }
        .flatMapLatest { items ->
            val tvShows = items.filter { it.mediaType == MediaType.TV }
            flow {
                if (tvShows.isEmpty()) {
                    emit(emptyList())
                    return@flow
                }

                val showIds = tvShows.map { it.id }
                val batchData = repository.getWatchlistEpisodesData(showIds)
                
                val episodeItems = tvShows.mapNotNull { show ->
                    val data = batchData[show.id]
                    val nextEpisode = data?.nextEpisode
                    
                    if (nextEpisode != null) {
                        val isPremiere = nextEpisode.episodeNumber == 1
                        val maxEp = repository.getMaxEpisodeNumberForSeason(show.id, nextEpisode.seasonNumber)
                        val isFinale = nextEpisode.episodeNumber == maxEp
                        
                        val daysAway = calculateDaysAway(nextEpisode.airDate)
                        val isNew = daysAway != null && daysAway < 0 && daysAway >= -7
                        
                        val remainingCount = (data.totalCount - data.watchedCount - 1).coerceAtLeast(0)

                        WatchlistEpisodeItem(
                            showId = show.id,
                            showName = show.title,
                            showPosterPath = show.posterPath,
                            episode = nextEpisode,
                            isPremiere = isPremiere,
                            isFinale = isFinale,
                            isNew = isNew,
                            remainingCount = remainingCount,
                            watchedCount = data.watchedCount,
                            totalCount = data.totalCount,
                            isSyncing = false,
                            isWatchLater = show.isWatchLater
                        )
                    } else if (data != null && data.totalCount == 0) {
                        // Show is in watchlist but has no episodes in DB yet -> Syncing
                        WatchlistEpisodeItem(
                            showId = show.id,
                            showName = show.title,
                            showPosterPath = show.posterPath,
                            episode = Episode("-1", "Syncing episodes...", "", null, 1, 1, null, 0.0, 0, false),
                            isPremiere = false,
                            isFinale = false,
                            isNew = false,
                            remainingCount = 0,
                            watchedCount = 0,
                            totalCount = 0,
                            isSyncing = true,
                            isWatchLater = show.isWatchLater
                        )
                    } else {
                        null
                    }
                }
                emit(episodeItems.sortedByDescending { it.isNew })
            }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Grouped episodes for the UI (maintains "IN PROGRESS" first)
    val groupedWatchlistEpisodes = watchlistEpisodes.map { items ->
        val groups = items.groupBy { 
            when {
                it.isWatchLater -> "WATCH LATER"
                it.watchedCount == 0 -> "HAVEN'T STARTED"
                else -> "IN PROGRESS"
            }
        }
        val sortedGroups = mutableMapOf<String, List<WatchlistEpisodeItem>>()
        groups["IN PROGRESS"]?.let { sortedGroups["IN PROGRESS"] = it }
        groups["HAVEN'T STARTED"]?.let { sortedGroups["HAVEN'T STARTED"] = it }
        groups["WATCH LATER"]?.let { sortedGroups["WATCH LATER"] = it }
        sortedGroups
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val upcomingEpisodes: StateFlow<List<UpcomingEpisodeItem>> = combine(
        explicitWatchlistItems,
        repository.getAllWatchedEpisodesFlow()
    ) { items, _ -> items }
        .flatMapLatest { items ->
            val tvShows = items.filter { it.mediaType == MediaType.TV }
            flow {
                val upcomingList = mutableListOf<UpcomingEpisodeItem>()
                for (show in tvShows) {
                    try {
                        // For upcoming, we want episodes that haven't aired yet or air today
                        val futureEpisodes = repository.getFutureEpisodes(show.id)
                        
                        futureEpisodes.forEach { episode ->
                            val daysAway = calculateDaysAway(episode.airDate)
                            if (daysAway != null && daysAway >= 0 && episode.seasonNumber > 0) {
                                upcomingList.add(
                                    UpcomingEpisodeItem(
                                        showId = show.id,
                                        showName = show.title,
                                        showPosterPath = show.posterPath,
                                        episode = episode,
                                        daysAway = daysAway
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                emit(upcomingList.sortedBy { it.daysAway })
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Grouped upcoming episodes for the UI
    val groupedUpcomingEpisodes = upcomingEpisodes.map { items ->
        val today = LocalDate.now()
        items.groupBy { item ->
            val airDate = DateUtils.parseDate(item.episode.airDate) ?: return@groupBy "LATER"
            
            val daysAway = ChronoUnit.DAYS.between(today, airDate)
            when {
                daysAway == 0L -> "TODAY"
                daysAway in 1..6 -> "THIS WEEK"
                daysAway in 7..13 -> "NEXT WEEK"
                airDate.month == today.month && airDate.year == today.year -> "THIS MONTH"
                airDate.month == today.plusMonths(1).month && airDate.year == today.plusMonths(1).year -> "NEXT MONTH"
                else -> "LATER"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun markEpisodeWatched(showId: String, seasonNumber: Int, episodeNumber: Int) {
        viewModelScope.launch {
            repository.markEpisodeWatched(showId, seasonNumber, episodeNumber, true)
        }
    }

    fun syncMissingRuntimes() {
        viewModelScope.launch {
            repository.syncMissingRuntimes()
        }
    }

    fun toggleWatchLater(showId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.toggleWatchLater(showId, !currentStatus)
        }
    }

    fun removeFromWatchlist(showId: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(showId)
        }
    }

    private fun calculateDaysAway(airDateString: String?): Long? {
        val airDate = DateUtils.parseDate(airDateString) ?: return null
        val today = LocalDate.now()
        return ChronoUnit.DAYS.between(today, airDate)
    }

    sealed class WatchlistUiState {
        object Loading : WatchlistUiState()
        data class Success(val items: List<MediaItem>) : WatchlistUiState()
        data class Error(val message: String) : WatchlistUiState()
    }
}
