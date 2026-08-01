package com.alok.justrack.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WatchlistEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
}
