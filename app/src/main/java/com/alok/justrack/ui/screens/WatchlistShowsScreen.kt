package com.alok.justrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.ui.components.*
import com.alok.justrack.ui.navigation.Screen
import com.alok.justrack.ui.theme.*
import com.alok.justrack.ui.viewmodel.WatchlistUiState
import com.alok.justrack.ui.viewmodel.WatchlistViewModel
import java.util.Locale

// ── TABBED WATCHLIST (SHOWS) SCREEN ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistShowsScreen(
    navController: NavController,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabTitles = listOf("WATCHLIST", "UPCOMING")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // --- Premium Tab Row ---
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Black,
            contentColor = Color.White,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = Color.White
                    )
                }
            },
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (selectedTab == index) Color.White else TextSecondary
                        )
                    }
                )
            }
        }

        // --- Page content with smooth transitions ---
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = tween(500, easing = EaseOutExpo)) { it } +
                            fadeIn(animationSpec = tween(500))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(500, easing = EaseOutExpo)) { -it } +
                                    fadeOut(animationSpec = tween(500)))
                } else {
                    (slideInHorizontally(animationSpec = tween(500, easing = EaseOutExpo)) { -it } +
                            fadeIn(animationSpec = tween(500))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(500, easing = EaseOutExpo)) { it } +
                                    fadeOut(animationSpec = tween(500)))
                }
            },
            label = "tab_content"
        ) { tab ->
            when (tab) {
                0 -> WatchlistTabContent(uiState = uiState, navController = navController)
                1 -> UpcomingTabContent(uiState = uiState, navController = navController)
            }
        }
    }
}

@Composable
private fun WatchlistTabContent(
    uiState: WatchlistUiState,
    navController: NavController
) {
    when (uiState) {
        is WatchlistUiState.Loading -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(5) { SkeletonBox(modifier = Modifier.fillMaxWidth().height(120.dp), cornerRadius = 12) }
            }
        }
        is WatchlistUiState.Success -> {
            val tvShows = uiState.items.filter { it.mediaType == com.alok.justrack.data.model.MediaType.TV }
            if (tvShows.isEmpty()) {
                PremiumEmptyState(
                    title = "Your watchlist is empty!",
                    subtitle = "Add shows you want to watch.",
                    buttonLabel = "BROWSE ALL SHOWS",
                    onClick = { navController.navigate(Screen.Explore.route) },
                    icon = Icons.Rounded.Tv
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tvShows, key = { it.id }) { item ->
                        ListMediaCard(
                            item = item,
                            onClick = { navController.navigate(Screen.Detail.createRoute(item.id)) }
                        )
                    }
                }
            }
        }
        is WatchlistUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun UpcomingTabContent(
    uiState: WatchlistUiState,
    navController: NavController
) {
    when (uiState) {
        is WatchlistUiState.Loading -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(5) { SkeletonBox(modifier = Modifier.fillMaxWidth().height(120.dp), cornerRadius = 12) }
            }
        }
        is WatchlistUiState.Success -> {
            val upcomingItems = uiState.items.filter { it.releaseDate.isNotBlank() }
            if (upcomingItems.isEmpty()) {
                PremiumEmptyState(
                    title = "No Upcoming Titles",
                    subtitle = "We'll notify you when shows in your watchlist have new episodes.",
                    buttonLabel = "Explore Now",
                    onClick = { navController.navigate(Screen.Explore.route) },
                    icon = Icons.Rounded.CalendarMonth
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(upcomingItems, key = { it.id }) { item ->
                        ListMediaCard(
                            item = item,
                            onClick = { navController.navigate(Screen.Detail.createRoute(item.id)) }
                        )
                    }
                }
            }
        }
        is WatchlistUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ListMediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.posterPath,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp, 90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceColor)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.mediaType.name} • ${item.releaseDate.split("-").firstOrNull() ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format(Locale.getDefault(), "%.1f", item.rating),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
        
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f)
        )
    }
}
