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
    version = 12, // Incremented to 12 to add position to custom_lists
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
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add sync fields to all user-data tables
                val tables = listOf("watchlist", "favourites", "custom_lists", "list_items", "custom_images", "watched_episodes")
                for (table in tables) {
                    database.execSQL("ALTER TABLE `$table` ADD COLUMN `userId` TEXT")
                    database.execSQL("ALTER TABLE `$table` ADD COLUMN `lastSyncAt` INTEGER")
                }
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE custom_lists ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove duplicates before creating unique index
                database.execSQL("DELETE FROM watched_episodes WHERE localId NOT IN (SELECT MIN(localId) FROM watched_episodes GROUP BY showId, seasonNumber, episodeNumber)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_watched_episodes_showId_seasonNumber_episodeNumber` ON `watched_episodes` (`showId`, `seasonNumber`, `episodeNumber`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_watched_episodes_showId` ON `watched_episodes` (`showId`)")
            }
        }

        val MIGRATION_14_10 = object : Migration(14, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Recreate watchlist table (remove genreIds, totalEpisodes, lastSyncAt)
                database.execSQL("CREATE TABLE IF NOT EXISTS `watchlist_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `overview` TEXT NOT NULL, `posterPath` TEXT, `backdropPath` TEXT, `customPosterPath` TEXT, `customBackdropPath` TEXT, `rating` REAL NOT NULL, `releaseDate` TEXT NOT NULL, `mediaType` TEXT NOT NULL, `runtime` INTEGER NOT NULL DEFAULT 0, `addedAt` INTEGER NOT NULL, `isWatched` INTEGER NOT NULL, `inWatchlist` INTEGER NOT NULL, `isWatchLater` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
                database.execSQL("INSERT INTO watchlist_new (id, title, overview, posterPath, backdropPath, customPosterPath, customBackdropPath, rating, releaseDate, mediaType, runtime, addedAt, isWatched, inWatchlist, isWatchLater) SELECT id, title, overview, posterPath, backdropPath, customPosterPath, customBackdropPath, rating, releaseDate, mediaType, runtime, addedAt, isWatched, inWatchlist, isWatchLater FROM watchlist")
                database.execSQL("DROP TABLE watchlist")
                database.execSQL("ALTER TABLE watchlist_new RENAME TO watchlist")

                // 2. Recreate watched_episodes table (remove foreign key to watchlist)
                database.execSQL("CREATE TABLE IF NOT EXISTS `watched_episodes_new` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `showId` TEXT NOT NULL, `seasonNumber` INTEGER NOT NULL, `episodeNumber` INTEGER NOT NULL, `watchedAt` INTEGER NOT NULL)")
                database.execSQL("INSERT INTO watched_episodes_new (localId, showId, seasonNumber, episodeNumber, watchedAt) SELECT localId, showId, seasonNumber, episodeNumber, watchedAt FROM watched_episodes")
                database.execSQL("DROP TABLE watched_episodes")
                database.execSQL("ALTER TABLE watched_episodes_new RENAME TO watched_episodes")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE watchlist ADD COLUMN runtime INTEGER NOT NULL DEFAULT 0")
            }
        }

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
