package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState

    private val _isWatchlisted = MutableStateFlow(false)
    val isWatchlisted: StateFlow<Boolean> = _isWatchlisted

    private val _isWatched = MutableStateFlow(false)
    val isWatched: StateFlow<Boolean> = _isWatched

    fun loadDetail(id: String, mediaType: String = "MOVIE") {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val type = try { MediaType.valueOf(mediaType) } catch (_: Exception) { MediaType.MOVIE }
                val item = repository.getMediaDetail(id, type)
                if (item != null) {
                    _uiState.value = DetailUiState.Success(item)
                    _isWatchlisted.value = repository.isInWatchlist(id)
                    _isWatched.value = repository.isWatched(id)
                } else {
                    _uiState.value = DetailUiState.Error("Media not found")
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun toggleWatchlist(movie: com.alok.justrack.data.model.MovieDetails) {
        viewModelScope.launch {
            if (_isWatchlisted.value) {
                repository.removeFromWatchlist(movie.id)
                _isWatchlisted.value = false
            } else {
                val item = com.alok.justrack.data.model.MediaItem(
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
                _isWatchlisted.value = true
            }
        }
    }

    fun toggleWatched(id: String) {
        viewModelScope.launch {
            val currentWatched = _isWatched.value
            repository.setWatched(id, !currentWatched)
            _isWatched.value = !currentWatched
        }
    }
}

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val item: com.alok.justrack.data.model.MovieDetails) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}
