package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.model.PersonDetails
import com.alok.justrack.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PersonUiState {
    object Loading : PersonUiState()
    data class Success(val person: PersonDetails) : PersonUiState()
    data class Error(val message: String) : PersonUiState()
}

@HiltViewModel
class PersonViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PersonUiState>(PersonUiState.Loading)
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    fun loadPersonDetails(id: String) {
        viewModelScope.launch {
            _uiState.value = PersonUiState.Loading
            try {
                val person = repository.getPersonDetails(id)
                if (person != null) {
                    _uiState.value = PersonUiState.Success(person)
                } else {
                    _uiState.value = PersonUiState.Error("Person not found")
                }
            } catch (e: Exception) {
                _uiState.value = PersonUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
