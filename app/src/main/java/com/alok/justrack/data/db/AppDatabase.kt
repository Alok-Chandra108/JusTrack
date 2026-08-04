package com.alok.justrack.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for the application.
 */
@Database(
    entities = [
        WatchlistEntity::class,
        FavouriteEntity::class,
        ListEntity::class,
        ListItemEntity::class,
        CustomImageEntity::class,
        WatchedEpisodeEntity::class,
        EpisodeEntity::class // Added for episode tracking
    ],
    version = 7, // Incremented version
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun listDao(): ListDao
    abstract fun customImageDao(): CustomImageDao
    abstract fun watchedEpisodeDao(): WatchedEpisodeDao
    abstract fun episodeDao(): EpisodeDao // Added for episode tracking
}
