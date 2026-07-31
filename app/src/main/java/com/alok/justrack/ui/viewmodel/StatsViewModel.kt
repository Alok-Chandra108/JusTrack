package com.alok.justrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsData(
    val totalItems: Int,
    val movieCount: Int,
    val tvCount: Int,
    val averageRating: Double,
    val topRatedTitle: String
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    repository: MediaRepository
) : ViewModel() {

    val stats: StateFlow<StatsData?> = repository
        .getWatchlistFlow()
        .map { items ->
            if (items.isEmpty()) return@map null
            StatsData(
                totalItems = items.size,
                movieCount = items.count { it.mediaType == MediaType.MOVIE },
                tvCount = items.count { it.mediaType == MediaType.TV },
                averageRating = items.map { it.rating }.average().let {
                    Math.round(it * 10) / 10.0
                },
                topRatedTitle = items.maxByOrNull { it.rating }?.title ?: "-"
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
