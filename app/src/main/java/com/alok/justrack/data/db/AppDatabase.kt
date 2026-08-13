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
    version = 9, // Incremented version to 9 to handle episode runtime and fix schema mismatch
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
                // Add isWatchLater column to watchlist
                database.execSQL("ALTER TABLE watchlist ADD COLUMN isWatchLater INTEGER NOT NULL DEFAULT 0")
                
                // Create episode tracking tables (Original Version 8 state)
                database.execSQL("CREATE TABLE IF NOT EXISTS `watched_episodes` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `showId` TEXT NOT NULL, `seasonNumber` INTEGER NOT NULL, `episodeNumber` INTEGER NOT NULL, `watchedAt` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `episode_entity` (`showId` TEXT NOT NULL, `seasonNumber` INTEGER NOT NULL, `episodeNumber` INTEGER NOT NULL, `title` TEXT NOT NULL, `overview` TEXT, `airDate` TEXT, `stillPath` TEXT, `voteAverage` REAL, PRIMARY KEY(`showId`, `seasonNumber`, `episodeNumber`), FOREIGN KEY(`showId`) REFERENCES `watchlist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_episode_entity_showId` ON `episode_entity` (`showId`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Ensure tables from v8 exist (defensive against incomplete MIGRATION_7_8)
                database.execSQL("CREATE TABLE IF NOT EXISTS `watched_episodes` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `showId` TEXT NOT NULL, `seasonNumber` INTEGER NOT NULL, `episodeNumber` INTEGER NOT NULL, `watchedAt` INTEGER NOT NULL)")
                
                // 2. Handle episode_entity and its runtime column
                val cursor = database.query("PRAGMA table_info(episode_entity)")
                var tableExists = false
                var hasRuntime = false
                while (cursor.moveToNext()) {
                    tableExists = true
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (name == "runtime") {
                        hasRuntime = true
                    }
                }
                cursor.close()

                if (!tableExists) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `episode_entity` (`showId` TEXT NOT NULL, `seasonNumber` INTEGER NOT NULL, `episodeNumber` INTEGER NOT NULL, `title` TEXT NOT NULL, `overview` TEXT, `airDate` TEXT, `stillPath` TEXT, `voteAverage` REAL, `runtime` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`showId`, `seasonNumber`, `episodeNumber`), FOREIGN KEY(`showId`) REFERENCES `watchlist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                    database.execSQL("CREATE INDEX IF NOT EXISTS `index_episode_entity_showId` ON `episode_entity` (`showId`)")
                } else if (!hasRuntime) {
                    database.execSQL("ALTER TABLE episode_entity ADD COLUMN runtime INTEGER NOT NULL DEFAULT 0")
                }
            }
        }
    }
}
