package com.alok.justrack.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WatchlistEntity::class, FavouriteEntity::class, ListEntity::class, ListItemEntity::class, CustomImageEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun listDao(): ListDao
    abstract fun customImageDao(): CustomImageDao
}
