package com.alok.justrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.alok.justrack.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Watchlist.route
    ) {
        composable(Screen.Watchlist.route) { WatchlistScreen(navController) }
        composable(Screen.Lists.route)    { ListsScreen(navController) }
        composable(Screen.Search.route)   { SearchScreen(navController) }
        composable(Screen.Stats.route)    { StatsScreen() }
        composable(Screen.Profile.route)  { ProfileScreen() }
        composable(Screen.Detail.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            DetailScreen(id)
        }
    }
}
