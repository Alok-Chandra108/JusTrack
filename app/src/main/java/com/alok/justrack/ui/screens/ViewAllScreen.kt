package com.alok.justrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.ui.components.CapsuleHeader
import com.alok.justrack.ui.components.PosterCard
import com.alok.justrack.ui.components.PosterOnlyCard
import com.alok.justrack.ui.navigation.Screen
import com.alok.justrack.ui.theme.Background
import com.alok.justrack.ui.theme.TextPrimary
import com.alok.justrack.ui.viewmodel.WatchlistViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ViewAllScreen(
    navController: NavController,
    title: String,
    type: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val watchedMovies by viewModel.watchedMovies.collectAsState()
    val watchedShows by viewModel.watchedShows.collectAsState()
    val favoriteMovies by viewModel.favoriteMovies.collectAsState()
    val favoriteShows by viewModel.favoriteShows.collectAsState()
    val explicitWatchlistItems by viewModel.explicitWatchlistItems.collectAsState()
    val listsWithPreviews by viewModel.listsWithPreviews.collectAsState()

    val filteredItems = remember(type, title, watchedMovies, watchedShows, favoriteMovies, favoriteShows, explicitWatchlistItems, listsWithPreviews) {
        when (type) {
            "movie" -> watchedMovies
            "tv" -> watchedShows
            "watchlist_movie" -> explicitWatchlistItems.filter { it.mediaType == MediaType.MOVIE }
            "watchlist_tv" -> explicitWatchlistItems.filter { it.mediaType == MediaType.TV }
            "favorite_movie" -> favoriteMovies
            "favorite_tv" -> favoriteShows
            "list" -> listsWithPreviews.find { it.first == title }?.second ?: emptyList()
            else -> emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Header ---
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            CapsuleHeader(
                title = title.uppercase()
            )
        }

        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No items found in $title", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredItems, key = { it.id + it.mediaType.name }) { item ->
                    PosterOnlyCard(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name)) }
                    )
                }
            }
        }
    }
}
