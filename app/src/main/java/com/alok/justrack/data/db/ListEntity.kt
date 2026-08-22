package com.alok.justrack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@Entity(tableName = "custom_lists")
data class ListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int = 0,
    @SerialName("createdat") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("userid") val userId: String? = null,
    @SerialName("lastsyncat") val lastSyncAt: Long? = null
)
