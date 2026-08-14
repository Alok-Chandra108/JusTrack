package com.alok.justrack.data.model

data class StatsData(
    val totalItems: Int,
    val movieCount: Int,
    val tvCount: Int,
    val averageRating: Double,
    val topRatedTitle: String,
    val showWatchTime: WatchTime = WatchTime(),
    val episodesWatched: Int = 0,
    val movieWatchTime: WatchTime = WatchTime(),
    val moviesWatched: Int = 0
)

data class WatchTime(
    val months: Int = 0,
    val days: Int = 0,
    val hours: Int = 0
)