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
            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("mediaType") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("key") { 
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "MOVIE"
                val sharedKey = backStackEntry.arguments?.getString("key")
                DetailScreen(
                    navController = navController,
                    id = id,
                    mediaType = mediaType,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                    sharedElementKey = sharedKey
                )
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
