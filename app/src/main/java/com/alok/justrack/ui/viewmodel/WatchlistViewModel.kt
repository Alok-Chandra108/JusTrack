package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.model.Episode
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.model.StatsData
import com.alok.justrack.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

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
        started = SharingStarted.Lazily,
        initialValue = null
    )

    // Watched Content
    val watchedMovies: StateFlow<List<MediaItem>> = watchlistItems.map { items ->
        items.filter { it.mediaType == MediaType.MOVIE && it.isWatched }
            .sortedByDescending { it.addedAt }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val watchedShows: StateFlow<List<MediaItem>> = watchlistItems.map { items ->
        items.filter { it.mediaType == MediaType.TV && it.isWatched }
            .sortedByDescending { it.addedAt }
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
        val totalCount: Int
    )

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    data class UpcomingEpisodeItem(
        val showId: String,
        val showName: String,
        val showPosterPath: String?,
        val episode: Episode,
        val daysAway: Long? // Negative = aired, 0 = today, positive = future days
    )

    // Watchlist tab: shows next unwatched episode for each show in watchlist
    @OptIn(ExperimentalCoroutinesApi::class)
    val watchlistEpisodes: StateFlow<List<WatchlistEpisodeItem>> = explicitWatchlistItems
        .filter { items -> items.any { it.mediaType == MediaType.TV } }
        .flatMapLatest { items ->
            val tvShows = items.filter { it.mediaType == MediaType.TV }
            flow {
                val episodeItems = mutableListOf<WatchlistEpisodeItem>()
                for (show in tvShows) {
                    try {
                        // Try to get next episode from cache/DB
                        val nextEpisode = repository.getNextEpisodeToWatch(show.id)
                        
                        // Filter out season 0 if it somehow leaked through
                        if (nextEpisode != null && nextEpisode.seasonNumber == 0) {
                             // This case is handled by DAO, but being defensive
                             continue 
                        }
                        
                        val isPremiere = nextEpisode?.episodeNumber == 1
                        val maxEp = nextEpisode?.let { repository.getMaxEpisodeNumberForSeason(show.id, it.seasonNumber) }
                        val isFinale = nextEpisode?.episodeNumber != null && nextEpisode.episodeNumber == maxEp
                        
                        val daysAway = calculateDaysAway(nextEpisode?.airDate)
                        // Aired in the last 7 days
                        val isNew = daysAway != null && daysAway < 0 && daysAway >= -7
                        
                        val totalCount = repository.getTotalEpisodeCount(show.id)
                        val watchedCount = repository.getWatchedEpisodeCount(show.id)
                        val remainingCount = (totalCount - watchedCount - 1).coerceAtLeast(0)

                        if (nextEpisode != null && nextEpisode.seasonNumber > 0) {
                            episodeItems.add(
                                WatchlistEpisodeItem(
                                    showId = show.id,
                                    showName = show.title,
                                    showPosterPath = show.posterPath,
                                    episode = nextEpisode,
                                    isPremiere = isPremiere,
                                    isFinale = isFinale,
                                    isNew = isNew,
                                    remainingCount = remainingCount,
                                    watchedCount = watchedCount,
                                    totalCount = totalCount
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Log error for this specific show but continue processing others
                        e.printStackTrace()
                        // Add a placeholder episode for this show so UI doesn't break
                        episodeItems.add(
                            WatchlistEpisodeItem(
                                showId = show.id,
                                showName = show.title,
                                showPosterPath = show.posterPath,
                                episode = Episode(
                                    id = "-1",
                                    name = "Error loading episode",
                                    overview = "",
                                    stillPath = null,
                                    seasonNumber = 1,
                                    episodeNumber = 1,
                                    airDate = null,
                                    voteAverage = 0.0,
                                    isWatched = false
                                ),
                                isPremiere = false,
                                isFinale = false,
                                isNew = false,
                                remainingCount = 0,
                                watchedCount = 0,
                                totalCount = 0
                            )
                        )
                    }
                }
                emit(episodeItems.filter { it.episode.seasonNumber > 0 }.sortedByDescending { it.isNew })
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Grouped episodes for the UI (maintains "IN PROGRESS" first)
    val groupedWatchlistEpisodes = watchlistEpisodes.map { items ->
        val groups = items.groupBy { if (it.watchedCount == 0) "HAVEN'T STARTED" else "IN PROGRESS" }
        // Ensure "IN PROGRESS" is first if it exists
        val sortedGroups = mutableMapOf<String, List<WatchlistEpisodeItem>>()
        groups["IN PROGRESS"]?.let { sortedGroups["IN PROGRESS"] = it }
        groups["HAVEN'T STARTED"]?.let { sortedGroups["HAVEN'T STARTED"] = it }
        sortedGroups
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val upcomingEpisodes: StateFlow<List<UpcomingEpisodeItem>> = explicitWatchlistItems
        .filter { items -> items.any { it.mediaType == MediaType.TV } }
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
            val airDateStr = item.episode.airDate ?: return@groupBy "LATER"
            val airDate = try {
                LocalDate.parse(airDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                return@groupBy "LATER"
            }
            
            val daysAway = ChronoUnit.DAYS.between(today, airDate)
            when {
                daysAway in 0..6 -> "THIS WEEK"
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

    private fun calculateDaysAway(airDateString: String?): Long? {
        if (airDateString.isNullOrBlank()) return null
        try {
            val airDate = LocalDate.parse(airDateString, DateTimeFormatter.ISO_LOCAL_DATE)
            val today = LocalDate.now()
            return ChronoUnit.DAYS.between(today, airDate)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    sealed class WatchlistUiState {
        object Loading : WatchlistUiState()
        data class Success(val items: List<MediaItem>) : WatchlistUiState()
        data class Error(val message: String) : WatchlistUiState()
    }
}
