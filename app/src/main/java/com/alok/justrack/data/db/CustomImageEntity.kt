package com.alok.justrack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@Entity(tableName = "custom_images")
data class CustomImageEntity(
    @PrimaryKey @SerialName("mediaid") val mediaId: String,
    @SerialName("mediatype") val mediaType: String,
    @SerialName("customposterpath") val customPosterPath: String? = null,
    @SerialName("custombackdroppath") val customBackdropPath: String? = null,
    @SerialName("userid") val userId: String? = null,
    @SerialName("lastsyncat") val lastSyncAt: Long? = null
)
