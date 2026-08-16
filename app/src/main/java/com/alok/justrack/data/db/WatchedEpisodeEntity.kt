package com.alok.justrack.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watched_episodes",
    indices = [
        Index(value = ["showId", "seasonNumber", "episodeNumber"], unique = true),
        Index(value = ["showId"])
    ]
)
data class WatchedEpisodeEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val showId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val watchedAt: Long = System.currentTimeMillis()
)
