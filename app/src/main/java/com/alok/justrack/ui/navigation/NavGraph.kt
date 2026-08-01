package com.alok.justrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.alok.justrack.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Shows.route
    ) {
        composable(Screen.Shows.route)   { WatchlistShowsScreen(navController) }
        composable(Screen.Movies.route)  { MoviesScreen(navController) }
        composable(Screen.Explore.route) { ExploreScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }
        composable(Screen.Detail.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "MOVIE"
            DetailScreen(navController, id, mediaType)
        }
        composable(Screen.ViewAll.route) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val type = backStackEntry.arguments?.getString("type") ?: ""
            ViewAllScreen(navController, title, type)
        }
    }
}
