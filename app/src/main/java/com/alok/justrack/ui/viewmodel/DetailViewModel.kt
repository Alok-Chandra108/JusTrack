package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.model.MediaItem
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

    fun loadDetail(id: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val item = repository.getMediaDetail(id)
                if (item != null) {
                    _uiState.value = DetailUiState.Success(item)
                    _isWatchlisted.value = repository.isInWatchlist(id)
                } else {
                    _uiState.value = DetailUiState.Error("Media not found")
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun toggleWatchlist(item: MediaItem) {
        viewModelScope.launch {
            if (_isWatchlisted.value) {
                repository.removeFromWatchlist(item.id)
                _isWatchlisted.value = false
            } else {
                repository.addToWatchlist(item)
                _isWatchlisted.value = true
            }
        }
    }
}

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val item: MediaItem) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}
