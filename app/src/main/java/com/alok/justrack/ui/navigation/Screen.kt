package com.alok.justrack.ui.navigation

sealed class Screen(val route: String, val label: String) {
    object Watchlist : Screen("watchlist", "Watchlist")
    object Lists : Screen("lists", "Lists")
    object Search : Screen("search", "Search")
    object Stats : Screen("stats", "Stats")
    object Profile : Screen("profile", "Profile")
    object Detail : Screen("detail/{id}", "Detail") {
        fun createRoute(id: String) = "detail/$id"
    }
}
