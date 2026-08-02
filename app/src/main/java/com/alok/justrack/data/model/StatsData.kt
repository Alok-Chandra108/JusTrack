package com.alok.justrack.data.model

data class StatsData(
    val totalItems: Int,
    val movieCount: Int,
    val tvCount: Int,
    val averageRating: Double,
    val topRatedTitle: String
)