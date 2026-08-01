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
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.ui.components.PosterCard
import com.alok.justrack.ui.navigation.Screen
import com.alok.justrack.ui.theme.Background
import com.alok.justrack.ui.theme.TextPrimary
import com.alok.justrack.ui.viewmodel.WatchlistUiState
import com.alok.justrack.ui.viewmodel.WatchlistViewModel

@Composable
fun ViewAllScreen(
    navController: NavController,
    title: String,
    type: String,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // --- Custom Immersive Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        when (val state = uiState) {
            is WatchlistUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is WatchlistUiState.Success -> {
                val filteredItems = when (type) {
                    "movies" -> state.items.filter { it.mediaType == MediaType.MOVIE }
                    "series" -> state.items.filter { it.mediaType == MediaType.TV }
                    "watched" -> state.items.filter { it.isWatched }
                    else -> state.items
                }

                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No items found", color = TextPrimary)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            PosterCard(
                                item = item,
                                onClick = { navController.navigate(Screen.Detail.createRoute(item.id)) }
                            )
                        }
                    }
                }
            }
            is WatchlistUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
