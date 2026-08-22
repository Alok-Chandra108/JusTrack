package com.alok.justrack.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@Entity(
    tableName = "watched_episodes",
    indices = [
        Index(value = ["showId", "seasonNumber", "episodeNumber"], unique = true),
        Index(value = ["showId"])
    ]
)
data class WatchedEpisodeEntity(
    @PrimaryKey(autoGenerate = true) @SerialName("localid") val localId: Long = 0,
    @SerialName("showid") val showId: String,
    @SerialName("seasonnumber") val seasonNumber: Int,
    @SerialName("episodenumber") val episodeNumber: Int,
    @SerialName("watchedat") val watchedAt: Long = System.currentTimeMillis(),
    @SerialName("userid") val userId: String? = null,
    @SerialName("lastsyncat") val lastSyncAt: Long? = null
)
