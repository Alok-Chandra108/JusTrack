package com.alok.justrack.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 8, // Incremented version to 8 to fix schema mismatch
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun listDao(): ListDao
    abstract fun customImageDao(): CustomImageDao
    abstract fun watchedEpisodeDao(): WatchedEpisodeDao
    abstract fun episodeDao(): EpisodeDao // Added for episode tracking

    companion object {
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE watchlist ADD COLUMN isWatchLater INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
