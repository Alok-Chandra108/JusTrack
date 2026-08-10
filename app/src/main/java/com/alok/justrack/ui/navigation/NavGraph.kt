package com.alok.justrack.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.alok.justrack.ui.screens.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(navController: NavHostController) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Screen.Shows.route
        ) {
            composable(Screen.Shows.route) {
                WatchlistShowsScreen(navController, this@SharedTransitionLayout, this)
            }
            composable(Screen.Movies.route) {
                MoviesScreen(navController, this@SharedTransitionLayout, this)
            }
            composable(Screen.Explore.route) {
                ExploreScreen(navController, this@SharedTransitionLayout, this)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(navController)
            }
            composable(Screen.Detail.route) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "MOVIE"
                DetailScreen(navController, id, mediaType, sharedTransitionScope = this@SharedTransitionLayout, animatedVisibilityScope = this)
            }
            composable(Screen.ViewAll.route) { backStackEntry ->
                val title = backStackEntry.arguments?.getString("title") ?: ""
                val type = backStackEntry.arguments?.getString("type") ?: ""
                ViewAllScreen(navController, title, type, sharedTransitionScope = this@SharedTransitionLayout, animatedVisibilityScope = this)
            }
            composable(Screen.Person.route) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                PersonDetailScreen(navController, id)
            }
        }
    }
}
