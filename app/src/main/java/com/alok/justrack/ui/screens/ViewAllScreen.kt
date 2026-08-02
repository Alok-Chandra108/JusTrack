package com.alok.justrack.ui.screens

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
import com.alok.justrack.ui.components.PosterCard
import com.alok.justrack.ui.navigation.Screen
import com.alok.justrack.ui.theme.Background
import com.alok.justrack.ui.theme.TextPrimary
import com.alok.justrack.ui.viewmodel.WatchlistViewModel

@Composable
fun ViewAllScreen(
    navController: NavController,
    title: String,
    type: String,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val watchlistItems by viewModel.watchlistItems.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val listsWithPreviews by viewModel.listsWithPreviews.collectAsState()

    val filteredItems = remember(type, title, watchlistItems, favorites, listsWithPreviews) {
        when (type) {
            "movie" -> watchlistItems.filter { it.mediaType == MediaType.MOVIE && it.isWatched }
            "tv" -> watchlistItems.filter { it.mediaType == MediaType.TV && it.isWatched }
            "watchlist_movie" -> watchlistItems.filter { it.mediaType == MediaType.MOVIE && it.inWatchlist }
            "watchlist_tv" -> watchlistItems.filter { it.mediaType == MediaType.TV && it.inWatchlist }
            "favorite_movie" -> favorites.filter { it.mediaType == MediaType.MOVIE }
            "favorite_tv" -> favorites.filter { it.mediaType == MediaType.TV }
            "list" -> listsWithPreviews.find { it.first == title }?.second ?: emptyList()
            else -> emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No items found in $title", color = TextPrimary.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredItems, key = { it.id + it.mediaType.name }) { item ->
                    PosterCard(
                        item = item,
                        onClick = { navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name)) }
                    )
                }
            }
        }
    }
}
