package com.alok.justrack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "list_items")
data class ListItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val mediaId: String,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val addedAt: Long = System.currentTimeMillis()
)
