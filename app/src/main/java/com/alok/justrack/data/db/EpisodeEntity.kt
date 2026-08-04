package com.alok.justrack.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a TV show episode.
 * We store essential episode metadata locally for quick access.
 */
@Entity(
    tableName = "episode_entity",
    primaryKeys = ["showId", "seasonNumber", "episodeNumber"],
    indices = [Index(value = ["showId"])],
    foreignKeys = [
        ForeignKey(
            entity = WatchlistEntity::class,
            parentColumns = ["id"],
            childColumns = ["showId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EpisodeEntity(
    val showId: String, // References WatchlistEntity.id (TMDB ID as String)
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String, // Episode name/title
    val overview: String?,
    val airDate: String?, // Release/air date in string format (YYYY-MM-DD)
    val stillPath: String?, // Path to episode still image
    val voteAverage: Double? = null
)
