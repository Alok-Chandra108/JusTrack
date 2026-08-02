package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.model.MovieDetails
import com.alok.justrack.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState

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

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)

    private var currentId: String = ""
    private var currentMediaType: MediaType = MediaType.MOVIE

    fun loadDetail(id: String, mediaType: String = "MOVIE") {
        currentId = id
        currentMediaType = try { MediaType.valueOf(mediaType) } catch (_: Exception) { MediaType.MOVIE }

        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val item = repository.getMediaDetail(id, currentMediaType)
                if (item != null) {
                    // Check for custom poster/backdrop saved in DB
                    val customPoster = repository.getCustomPoster(id)
                    val customBackdrop = repository.getCustomBackdrop(id)

                    val finalItem = item.copy(
                        posterPath = customPoster ?: item.posterPath,
                        backdropPath = customBackdrop ?: item.backdropPath
                    )

                    _uiState.value = DetailUiState.Success(finalItem)
                    _isInWatchlist.value = repository.isInWatchlist(id)
                    _isWatched.value = repository.isWatched(id)
                    _isFavourite.value = repository.isFavourite(id, currentMediaType)
                    _mediaLists.value = repository.getListsForMedia(id, currentMediaType)
                    _currentMediaItem.value = MediaItem(
                        id = finalItem.id,
                        title = finalItem.title,
                        overview = finalItem.overview,
                        posterPath = finalItem.posterPath,
                        backdropPath = finalItem.backdropPath,
                        rating = finalItem.rating,
                        releaseDate = finalItem.releaseDate,
                        mediaType = finalItem.mediaType
                    )
                } else {
                    _uiState.value = DetailUiState.Error("Media not found")
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "Unknown error")
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
                    releaseDate = movie.releaseDate,
                    mediaType = movie.mediaType
                )
                repository.addToWatchlist(item)
                _isInWatchlist.value = true
            }
        }
    }

    fun toggleWatched(id: String) {
        viewModelScope.launch {
            val currentWatched = _isWatched.value
            val isCurrentlyInWatchlist = _isInWatchlist.value

            if (!currentWatched && !isCurrentlyInWatchlist) {
                // If marking as watched and not in watchlist, add it to watchlist first
                _currentMediaItem.value?.let { item ->
                    repository.addToWatchlist(item.copy(isWatched = true))
                    _isInWatchlist.value = true
                }
            } else {
                repository.setWatched(id, !currentWatched)
            }
            _isWatched.value = !currentWatched
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
            val current = (_uiState.value as? DetailUiState.Success)?.item ?: return@launch
            val updated = current.copy(posterPath = url)
            _uiState.value = DetailUiState.Success(updated)
            _currentMediaItem.value = _currentMediaItem.value?.copy(posterPath = url)
        }
    }

    fun changeBackdrop(url: String) {
        viewModelScope.launch {
            repository.saveCustomBackdrop(currentId, url)
            val current = (_uiState.value as? DetailUiState.Success)?.item ?: return@launch
            val updated = current.copy(backdropPath = url)
            _uiState.value = DetailUiState.Success(updated)
            _currentMediaItem.value = _currentMediaItem.value?.copy(backdropPath = url)
        }
    }
}

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val item: MovieDetails) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}
