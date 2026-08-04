package com.alok.justrack.ui.viewmodel

import com.alok.justrack.data.model.Episode
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType

/**
 * Represents an episode to be displayed in the watchlist (next unwatched episode for a show)
 */
data class WatchlistEpisodeItem(
    val showId: String,
    val showName: String,
    val showPosterPath: String?,
    val episode: Episode,
    val isPremiere: Boolean // True if this is S01E01
)

/**
 * Represents an episode to be displayed in the upcoming tab
 */
data class UpcomingEpisodeItem(
    val showId: String,
    val showName: String,
    val showPosterPath: String?,
    val episode: Episode,
    val daysAway: Long? // null if aired today, negative if aired in past, positive if future
)
