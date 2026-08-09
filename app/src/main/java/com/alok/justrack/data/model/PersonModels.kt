package com.alok.justrack.data.model

data class PersonDetails(
    val id: String,
    val name: String,
    val biography: String,
    val birthday: String?,
    val placeOfBirth: String?,
    val profilePath: String?,
    val knownForDepartment: String,
    val movieCredits: List<MediaItem>,
    val tvCredits: List<MediaItem>
)
