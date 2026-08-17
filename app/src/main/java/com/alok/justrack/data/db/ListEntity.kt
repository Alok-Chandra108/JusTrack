package com.alok.justrack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_lists")
data class ListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
