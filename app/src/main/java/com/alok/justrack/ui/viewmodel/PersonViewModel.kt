package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.model.PersonDetails
import com.alok.justrack.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType

sealed class PersonUiState {
    object Loading : PersonUiState()
    data class Success(val person: PersonDetails) : PersonUiState()
    data class Error(val message: String) : PersonUiState()
}

@HiltViewModel
class PersonViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _internalUiState = MutableStateFlow<PersonUiState>(PersonUiState.Loading)
    val uiState: StateFlow<PersonUiState> = combine(_internalUiState, repository.getWatchlistFlow()) { state, watchlist ->
        updateStateWithWatchlist(state, watchlist)
    }.stateIn(viewModelScope, SharingStarted.Lazily, PersonUiState.Loading)

    fun loadPersonDetails(id: String) {
        viewModelScope.launch {
            _internalUiState.value = PersonUiState.Loading
            try {
                val person = repository.getPersonDetails(id)
                if (person != null) {
                    _internalUiState.value = PersonUiState.Success(person)
                } else {
                    _internalUiState.value = PersonUiState.Error("Person not found")
                }
            } catch (e: Exception) {
                _internalUiState.value = PersonUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun updateStateWithWatchlist(state: PersonUiState, watchlist: List<MediaItem>): PersonUiState {
        return if (state is PersonUiState.Success) {
            val person = state.person
            val updatedMovieCredits = person.movieCredits.map { it.syncWithWatchlist(watchlist) }
            val updatedTvCredits = person.tvCredits.map { it.syncWithWatchlist(watchlist) }
            PersonUiState.Success(person.copy(movieCredits = updatedMovieCredits, tvCredits = updatedTvCredits))
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
}
