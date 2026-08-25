package com.alok.justrack.ui.viewmodel

enum class ExploreSection(val key: String) {
    POPULAR_MOVIES("popular_movies"),
    POPULAR_TV("popular_tv"),
    TOP_RATED_MOVIES("top_rated_movies"),
    TOP_RATED_TV("top_rated_tv"),
    UPCOMING_MOVIES("upcoming_movies"),
    ON_THE_AIR_TV("on_the_air_tv");

    companion object {
        fun fromKey(key: String): ExploreSection? = entries.find { it.key == key }
    }
}
