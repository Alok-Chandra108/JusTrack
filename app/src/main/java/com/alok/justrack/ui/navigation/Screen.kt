package com.alok.justrack.ui.navigation

sealed class Screen(val route: String, val label: String) {
    object Shows : Screen("shows", "Shows")
    object Movies : Screen("movies", "Movies")
    object Explore : Screen("explore", "Explore")
    object Profile : Screen("profile", "Profile")
    object Detail : Screen("detail/{id}", "Detail") {
        fun createRoute(id: String) = "detail/$id"
    }
    object ViewAll : Screen("view_all/{title}/{type}", "View All") {
        fun createRoute(title: String, type: String) = "view_all/$title/$type"
    }
}
