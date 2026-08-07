package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.model.MovieDetails
import com.alok.justrack.data.model.Season
import com.alok.justrack.data.repository.MediaRepository
import com.alok.justrack.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _rawDetails = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    private val _watchedEpisodes = MutableStateFlow<Set<String>>(emptySet())
    private var watchedEpisodesJob: Job? = null
    
    // Watchlist flow to track watched IDs for filtering recommendations
    private val _watchedIds = repository.getWatchlistFlow()
        .map { list -> list.filter { it.isWatched }.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Reactive UI state that filters recommendations based on watched status
    val uiState: StateFlow<DetailUiState> = combine(_rawDetails, _watchedIds, _watchedEpisodes) { state, watched, watchedEps ->
        if (state is DetailUiState.Success) {
            val filteredRecs = state.item.recommendations.filter { it.id !in watched }
            
            // For TV shows, ensure episodes in seasons reflect latest watched status and update counts
            val finalItem = if (state.item.mediaType == MediaType.TV) {
                state.item.copy(
                    recommendations = filteredRecs,
                    seasons = state.item.seasons.map { season ->
                        val seasonWatchedEps = watchedEps.filter { it.startsWith("S${season.seasonNumber}E") }
                        val episodesWithWatchedStatus = season.episodes.map { ep ->
                            ep.copy(isWatched = watchedEps.contains("S${ep.seasonNumber}E${ep.episodeNumber}"))
                        }
                        
                        season.copy(
                            watchedCount = seasonWatchedEps.size,
                            // If we have detailed episodes, use them. Otherwise keep current.
                            episodes = if (episodesWithWatchedStatus.isNotEmpty()) episodesWithWatchedStatus else season.episodes,
                            // Ensure total count is updated if episodes are present but count is 0
                            episodeCount = if (season.episodeCount == 0 && episodesWithWatchedStatus.isNotEmpty()) episodesWithWatchedStatus.size else season.episodeCount
                        )
                    }
                )
            } else {
                state.item.copy(recommendations = filteredRecs)
            }
            
            state.copy(item = finalItem)
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState.Loading)

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
        if (currentMediaType == MediaType.TV) {
            watchedEpisodesJob = repository.getWatchedEpisodesFlow(id)
                .onEach { _watchedEpisodes.value = it.toSet() }
                .launchIn(viewModelScope)
        }

        viewModelScope.launch {
            _rawDetails.value = DetailUiState.Loading
            try {
                val item = repository.getMediaDetail(id, currentMediaType)
                if (item != null) {
                    val customPoster = repository.getCustomPoster(id)
                    val customBackdrop = repository.getCustomBackdrop(id)

                    val finalItem = item.copy(
                        posterPath = customPoster ?: item.posterPath,
                        backdropPath = customBackdrop ?: item.backdropPath
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
            repository.markEpisodeWatched(currentId, seasonNumber, episodeNumber, watched)
            // No manual reload needed as _watchedEpisodes flow is reactive
        }
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
