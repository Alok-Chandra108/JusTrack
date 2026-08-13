package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.model.MovieDetails
import com.alok.justrack.data.model.Season
import com.alok.justrack.data.model.Episode
import com.alok.justrack.data.model.ShowProgress
import com.alok.justrack.data.repository.MediaRepository
import com.alok.justrack.util.DateUtils
import com.alok.justrack.data.api.TmdbApiService
import com.alok.justrack.data.mapper.TmdbMapper.toMediaItem
import com.alok.justrack.ui.theme.GoldAccent
import com.alok.justrack.ui.theme.WatchedGreen
import com.alok.justrack.ui.theme.EndedPurple
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val apiService: TmdbApiService,
    private val repository: MediaRepository
) : ViewModel() {

    private val _rawDetails = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    private val _watchedEpisodes = MutableStateFlow<Set<String>>(emptySet())
    private var watchedEpisodesJob: Job? = null
    private var syncJob: Job? = null

    private val _releasedEpisodeCount = MutableStateFlow(0)
    private val _totalAiredRuntime = MutableStateFlow(0)
    
    val showCompletionEvents: Flow<String> = repository.showCompletionEvents

    private val _recommendationSeed = MutableStateFlow(System.currentTimeMillis().toInt())

    // Watchlist flow to track watched IDs for filtering recommendations
    private val _watchedIds = repository.getWatchlistFlow()
        .map { list -> list.filter { it.isWatched }.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _watchlistIds = repository.getWatchlistFlow()
        .map { list -> list.filter { it.inWatchlist }.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Reactive UI state that filters recommendations based on watched status
    val uiState: StateFlow<DetailUiState> = combine(
        _rawDetails, 
        _watchedIds, 
        _watchlistIds, 
        _watchedEpisodes, 
        _recommendationSeed, 
        _totalAiredRuntime
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val state = flows[0] as DetailUiState
        @Suppress("UNCHECKED_CAST")
        val watched = flows[1] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val inWatchlistIds = flows[2] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val watchedEps = flows[3] as Set<String>
        val seed = flows[4] as Int
        val totalRuntime = flows[5] as Int

        if (state is DetailUiState.Success) {
            // 1. Process Recommendations (Only depends on watched, inWatchlistIds, and seed)
            val filteredRecs = state.item.recommendations.filter { it.id !in watched && it.id !in inWatchlistIds }
            val processedRecs = if (filteredRecs.isNotEmpty()) {
                filteredRecs.shuffled(java.util.Random(seed.toLong())).map { 
                    it.copy(inWatchlist = it.id in inWatchlistIds) 
                }
            } else emptyList()

            // 2. Process TV Content (Only depends on watchedEps and totalRuntime)
            val finalItem = if (state.item.mediaType == MediaType.TV) {
                val formattedRuntime = if (totalRuntime > 0) {
                    com.alok.justrack.util.DateUtils.formatMinutes(totalRuntime)
                } else state.item.runtime

                state.item.copy(
                    runtime = formattedRuntime,
                    recommendations = processedRecs,
                    seasons = state.item.seasons.map { season ->
                        val episodesWithWatchedStatus = season.episodes.map { ep ->
                            ep.copy(isWatched = watchedEps.contains("S${ep.seasonNumber}E${ep.episodeNumber}"))
                        }
                        
                        val watchedCount = episodesWithWatchedStatus.count { it.isWatched }
                        
                        season.copy(
                            watchedCount = watchedCount,
                            episodes = if (episodesWithWatchedStatus.isNotEmpty()) episodesWithWatchedStatus else season.episodes,
                            episodeCount = if (season.episodeCount == 0 && episodesWithWatchedStatus.isNotEmpty()) episodesWithWatchedStatus.size else season.episodeCount
                        )
                    }
                )
            } else {
                state.item.copy(recommendations = processedRecs)
            }
            
            state.copy(item = finalItem)
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState.Loading)

    val showProgress: StateFlow<ShowProgress?> = combine(
        _rawDetails,
        _watchedEpisodes,
        _releasedEpisodeCount
    ) { details, watchedEps, releasedCount ->
        if (details is DetailUiState.Success && details.item.mediaType == MediaType.TV) {
            val total = if (releasedCount > 0) releasedCount else details.item.numberOfEpisodes
            
            // Filter out specials from watched count if they were synced
            val watched = watchedEps.filter { 
                !it.startsWith("S0E") 
            }.size

            val percentage = if (total > 0) (watched.toFloat() / total * 100).toInt().coerceIn(0, 100) else 0
            
            val status = details.item.status.lowercase()
            val isEnded = status == "ended" || status == "canceled"
            
            val color = when {
                total == 0 -> androidx.compose.ui.graphics.Color.Gray
                isEnded -> EndedPurple
                watched == total && total > 0 -> WatchedGreen
                else -> GoldAccent
            }
            
            val label = when {
                total == 0 -> "Progress unavailable"
                isEnded && watched == total && total > 0 -> "Ended"
                watched == total && total > 0 -> "Completed"
                else -> "$percentage%"
            }
            
            ShowProgress(
                percentage = percentage,
                color = color,
                label = label,
                totalReleased = total,
                watched = watched
            )
        } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isInWatchlist = MutableStateFlow(false)
    val isInWatchlist: StateFlow<Boolean> = _isInWatchlist

    private val _isWatched = MutableStateFlow(false)
    val isWatched: StateFlow<Boolean> = _isWatched

    private val _isFavourite = MutableStateFlow(false)
    val isFavourite: StateFlow<Boolean> = _isFavourite

    private val _lists = repository.getListsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val lists: StateFlow<List<Pair<String, String>>> = _lists

    private val _mediaLists = MutableStateFlow<List<String>>(emptyList())
    val mediaLists: StateFlow<List<String>> = _mediaLists

    private val _posterImages = MutableStateFlow<List<String>>(emptyList())
    val posterImages: StateFlow<List<String>> = _posterImages

    private val _backdropImages = MutableStateFlow<List<String>>(emptyList())
    val backdropImages: StateFlow<List<String>> = _backdropImages

    private val _episodeMarkConfirmation = MutableStateFlow<EpisodeMarkConfirmation?>(null)
    val episodeMarkConfirmation: StateFlow<EpisodeMarkConfirmation?> = _episodeMarkConfirmation

    private val _selectedSeason = MutableStateFlow<com.alok.justrack.data.model.Season?>(null)
    val selectedSeason: StateFlow<com.alok.justrack.data.model.Season?> = combine(_selectedSeason, _watchedEpisodes) { season, watchedEps ->
        season?.copy(
            episodes = season.episodes.map { ep ->
                ep.copy(isWatched = watchedEps.contains("S${ep.seasonNumber}E${ep.episodeNumber}"))
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)

    private var currentId: String = ""
    private var currentMediaType: MediaType = MediaType.MOVIE

    fun loadDetail(id: String, mediaType: String = "MOVIE") {
        currentId = id
        currentMediaType = try { 
            MediaType.valueOf(mediaType.uppercase()) 
        } catch (_: Exception) { 
            MediaType.MOVIE 
        }

        // Setup reactive episode tracking for TV shows
        watchedEpisodesJob?.cancel()
        _watchedEpisodes.value = emptySet()
        _releasedEpisodeCount.value = 0
        _totalAiredRuntime.value = 0
        if (currentMediaType == MediaType.TV) {
            watchedEpisodesJob = repository.getWatchedEpisodesFlow(id)
                .onEach { _watchedEpisodes.value = it.toSet() }
                .launchIn(viewModelScope)
            
            repository.getTotalAiredRuntimeFlow(id)
                .onEach { _totalAiredRuntime.value = it }
                .launchIn(viewModelScope)
                
            // Sync episodes to get accurate counts
            syncJob?.cancel()
            syncJob = viewModelScope.launch {
                repository.syncEpisodes(id)
                _releasedEpisodeCount.value = repository.getReleasedEpisodeCount(id)
            }
            
            // Also update released count whenever episodes are updated
            repository.episodesUpdateEvents
                .onEach { 
                    _releasedEpisodeCount.value = repository.getReleasedEpisodeCount(id)
                }
                .launchIn(viewModelScope)
        }

        viewModelScope.launch {
            _rawDetails.value = DetailUiState.Loading
            try {
                val item = repository.getMediaDetail(id, currentMediaType)
                if (item != null) {
                    val customPoster = repository.getCustomPoster(id)
                    val customBackdrop = repository.getCustomBackdrop(id)

                    // --- Smart Recommendations Blending ---
                    val smartRecommendations = fetchSmartRecommendations(item)
                    val blendedRecs = (item.recommendations + smartRecommendations)
                        .distinctBy { it.id }
                        .filter { it.id != item.id }

                    val finalItem = item.copy(
                        posterPath = customPoster ?: item.posterPath,
                        backdropPath = customBackdrop ?: item.backdropPath,
                        recommendations = blendedRecs
                    )

                    _rawDetails.value = DetailUiState.Success(finalItem)
                    _isInWatchlist.value = repository.isInWatchlist(id)
                    _isWatched.value = repository.isWatched(id)
                    _isFavourite.value = repository.isFavourite(id, currentMediaType)
                    _mediaLists.value = repository.getListsForMedia(id, currentMediaType)
                    
                    // Fetch existing entity to get correct addedAt if it exists
                    val existingItem = repository.getWatchlist().find { it.id == id }
                    
                    _currentMediaItem.value = MediaItem(
                        id = finalItem.id,
                        title = finalItem.title,
                        overview = finalItem.overview,
                        posterPath = finalItem.posterPath,
                        backdropPath = finalItem.backdropPath,
                        rating = finalItem.rating,
                        releaseDate = finalItem.rawReleaseDate.ifEmpty { finalItem.releaseDate },
                        mediaType = finalItem.mediaType,
                        isWatched = _isWatched.value,
                        inWatchlist = _isInWatchlist.value,
                        addedAt = existingItem?.addedAt ?: System.currentTimeMillis()
                    )
                } else {
                    _rawDetails.value = DetailUiState.Error("Media not found")
                }
            } catch (e: Exception) {
                _rawDetails.value = DetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun fetchSmartRecommendations(item: MovieDetails): List<MediaItem> = coroutineScope {
        val actorIds = item.cast.take(2).map { it.id }
        val lang = item.originalLanguage
        
        val actorRequests = actorIds.map { actorId ->
            async {
                try {
                    if (item.mediaType == MediaType.MOVIE) {
                        apiService.discoverMovies(withCast = actorId, includeAdult = false).results
                    } else {
                        apiService.discoverTv(withCast = actorId, includeAdult = false).results
                    }
                } catch (e: Exception) { emptyList() }
            }
        }

        val regionalRequest = async {
            try {
                if (item.mediaType == MediaType.MOVIE) {
                    apiService.discoverMovies(originalLanguage = lang, includeAdult = false, sortBy = "popularity.desc").results
                } else {
                    apiService.discoverTv(originalLanguage = lang, includeAdult = false, sortBy = "popularity.desc").results
                }
            } catch (e: Exception) { emptyList() }
        }

        val actorResults = actorRequests.flatMap { it.await() }
        val regionalResults = regionalRequest.await()

        (actorResults + regionalResults)
            .map { it.toMediaItem(item.mediaType) }
            .distinctBy { it.id }
    }

    fun toggleWatchlist(movie: MovieDetails) {
        viewModelScope.launch {
            if (_isInWatchlist.value) {
                repository.removeFromWatchlist(movie.id)
                _isInWatchlist.value = false
            } else {
                val item = MediaItem(
                    id = movie.id,
                    title = movie.title,
                    overview = movie.overview,
                    posterPath = movie.posterPath,
                    backdropPath = movie.backdropPath,
                    rating = movie.rating,
                    releaseDate = movie.rawReleaseDate.ifEmpty { movie.releaseDate },
                    mediaType = movie.mediaType,
                    isWatched = _isWatched.value,
                    inWatchlist = true
                )
                repository.addToWatchlist(item)
                _isInWatchlist.value = true
            }
        }
    }

    fun toggleWatched(id: String) {
        viewModelScope.launch {
            val currentWatched = _isWatched.value
            val markingAsWatched = !currentWatched

            _currentMediaItem.value?.let { item ->
                repository.setWatched(item, markingAsWatched)
                _isWatched.value = markingAsWatched
                if (markingAsWatched) {
                    _isInWatchlist.value = false
                }
            }
        }
    }

    fun toggleFavourite() {
        viewModelScope.launch {
            val item = _currentMediaItem.value ?: return@launch
            _isFavourite.value = repository.toggleFavourite(item)
        }
    }

    fun addToList(listId: String) {
        viewModelScope.launch {
            val item = _currentMediaItem.value ?: return@launch
            repository.addToList(listId, item)
            _mediaLists.value = repository.getListsForMedia(currentId, currentMediaType)
        }
    }

    fun removeFromList(listId: String) {
        viewModelScope.launch {
            repository.removeFromList(listId, currentId, currentMediaType)
            _mediaLists.value = repository.getListsForMedia(currentId, currentMediaType)
        }
    }

    fun toggleWatchlistForRecommendation(movie: MediaItem) {
        viewModelScope.launch {
            if (movie.id in _watchlistIds.value) {
                repository.removeFromWatchlist(movie.id)
            } else {
                val item = movie.copy(inWatchlist = true)
                repository.addToWatchlist(item)
            }
        }
    }

    fun refreshRecommendations() {
        _recommendationSeed.value = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    }

    fun createList(name: String) {
        viewModelScope.launch {
            repository.createList(name)
        }
    }

    fun loadImages() {
        viewModelScope.launch {
            val (posters, backdrops) = when (currentMediaType) {
                MediaType.MOVIE -> repository.getMovieImages(currentId)
                MediaType.TV -> repository.getTvImages(currentId)
            }
            _posterImages.value = posters
            _backdropImages.value = backdrops
        }
    }

    fun changePoster(url: String) {
        viewModelScope.launch {
            repository.saveCustomPoster(currentId, url)
            val current = (_rawDetails.value as? DetailUiState.Success)?.item ?: return@launch
            val updated = current.copy(posterPath = url)
            _rawDetails.value = DetailUiState.Success(updated)
            _currentMediaItem.value = _currentMediaItem.value?.copy(posterPath = url)
        }
    }

    fun changeBackdrop(url: String) {
        viewModelScope.launch {
            repository.saveCustomBackdrop(currentId, url)
            val current = (_rawDetails.value as? DetailUiState.Success)?.item ?: return@launch
            val updated = current.copy(backdropPath = url)
            _rawDetails.value = DetailUiState.Success(updated)
            _currentMediaItem.value = _currentMediaItem.value?.copy(backdropPath = url)
        }
    }

    // --- TV Show specific methods ---

    fun loadSeason(seasonNumber: Int) {
        viewModelScope.launch {
            val season = repository.getSeasonDetails(currentId, seasonNumber)
            if (season != null) {
                _selectedSeason.value = season
                
                // Merge into main details for inline display
                val currentState = _rawDetails.value
                if (currentState is DetailUiState.Success) {
                    val updatedSeasons = currentState.item.seasons.map {
                        if (it.seasonNumber == seasonNumber) season else it
                    }
                    _rawDetails.value = currentState.copy(
                        item = currentState.item.copy(seasons = updatedSeasons)
                    )
                }
            }
        }
    }

    fun markEpisodeWatched(seasonNumber: Int, episodeNumber: Int, watched: Boolean) {
        viewModelScope.launch {
            if (watched) {
                val season = _selectedSeason.value
                val watchedSet = _watchedEpisodes.value
                if (season != null && season.seasonNumber == seasonNumber) {
                    val previousUnwatched = season.episodes
                        .filter { ep -> 
                            ep.episodeNumber < episodeNumber && 
                            !watchedSet.contains("S${ep.seasonNumber}E${ep.episodeNumber}")
                        }
                        .map { it.episodeNumber }

                    if (previousUnwatched.isNotEmpty()) {
                        _episodeMarkConfirmation.value = EpisodeMarkConfirmation(
                            seasonNumber = seasonNumber,
                            episodeNumber = episodeNumber,
                            previousEpisodes = previousUnwatched
                        )
                        return@launch
                    }
                }
            }
            
            repository.markEpisodeWatched(currentId, seasonNumber, episodeNumber, watched)
        }
    }

    fun confirmMarkPreviousWatched() {
        val confirmation = _episodeMarkConfirmation.value ?: return
        viewModelScope.launch {
            val allEpisodes = confirmation.previousEpisodes + confirmation.episodeNumber
            repository.markEpisodesWatched(currentId, confirmation.seasonNumber, allEpisodes, true)
            _episodeMarkConfirmation.value = null
        }
    }

    fun dismissMarkPreviousConfirmation() {
        _episodeMarkConfirmation.value = null
    }

    fun toggleSeasonWatched(season: Season) {
        viewModelScope.launch {
            val episodes = if (season.episodes.isNotEmpty()) {
                season.episodes
            } else {
                repository.getSeasonDetails(currentId, season.seasonNumber)?.episodes ?: emptyList()
            }

            // Filter episodes that have already aired (daysUntil <= 0)
            val releasedEpisodes = episodes.filter { 
                val days = DateUtils.getDaysUntil(it.airDate)
                days == null || days <= 0 
            }

            if (releasedEpisodes.isEmpty()) return@launch

            // Check if all released episodes are currently watched
            val allReleasedWatched = releasedEpisodes.all { it.isWatched }
            
            if (allReleasedWatched) {
                // If all released are watched, unmark the entire season
                repository.markSeasonWatched(currentId, season.seasonNumber, false, episodes)
            } else {
                // Otherwise, mark only the released episodes as watched
                repository.markSeasonWatched(currentId, season.seasonNumber, true, releasedEpisodes)
            }
        }
    }
}

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val item: MovieDetails) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

data class EpisodeMarkConfirmation(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val previousEpisodes: List<Int>
)
