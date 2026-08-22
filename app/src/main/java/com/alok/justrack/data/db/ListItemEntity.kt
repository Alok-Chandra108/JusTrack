package com.alok.justrack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@Entity(tableName = "list_items")
data class ListItemEntity(
    @PrimaryKey val id: String,
    @SerialName("listid") val listId: String,
    @SerialName("mediaid") val mediaId: String,
    @SerialName("mediatype") val mediaType: String,
    val title: String,
    @SerialName("posterpath") val posterPath: String?,
    @SerialName("backdroppath") val backdropPath: String?,
    @SerialName("addedat") val addedAt: Long = System.currentTimeMillis(),
    @SerialName("userid") val userId: String? = null,
    @SerialName("lastsyncat") val lastSyncAt: Long? = null
)
