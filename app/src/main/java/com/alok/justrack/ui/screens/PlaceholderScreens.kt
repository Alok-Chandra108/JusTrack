package com.alok.justrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.ui.components.*
import com.alok.justrack.ui.navigation.Screen
import com.alok.justrack.ui.theme.*
import com.alok.justrack.ui.viewmodel.*
import com.alok.justrack.ui.viewmodel.WatchlistViewModel.WatchlistUiState

// ─────────────────────────────────────────────
// MOVIES SCREEN
// ─────────────────────────────────────────────
@Composable
fun MoviesScreen(
    navController: NavController,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabTitles = listOf("WATCHLIST", "UPCOMING")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Background,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = AccentPrimary
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
                            color = if (selectedTab == index) AccentPrimary else TextSecondary
                        )
                    }
                )
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(tween(500)) togetherWith fadeOut(tween(500))
            },
            label = "movie_tabs"
        ) { tab ->
            when (tab) {
                0 -> MovieWatchlistContent(uiState, navController)
                1 -> MovieUpcomingContent(uiState, navController)
            }
        }
    }
}

@Composable
private fun MovieWatchlistContent(uiState: WatchlistUiState, navController: NavController) {
    when (uiState) {
        is WatchlistUiState.Success -> {
            val movies = uiState.items.filter { it.mediaType == MediaType.MOVIE && it.inWatchlist }
            if (movies.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PremiumEmptyState(
                        title = "Your watchlist is empty!",
                        subtitle = "Add movies you want to watch.",
                        buttonLabel = "BROWSE",
                        onClick = { navController.navigate(Screen.Explore.route) },
                        illustration = { MovieIllustration() }
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    lazyGridItems(movies) { item ->
                        PosterOnlyCard(item, { navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name)) })
                    }
                }
            }
        }
        else -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = AccentPrimary) }
    }
}

@Composable
private fun MovieUpcomingContent(uiState: WatchlistUiState, navController: NavController) {
    when (uiState) {
        is WatchlistUiState.Success -> {
            val today = java.time.LocalDate.now()
            val upcomingMovies = uiState.items.filter {
                it.mediaType == MediaType.MOVIE && it.inWatchlist && it.releaseDate.isNotBlank() && try {
                    java.time.LocalDate.parse(it.releaseDate).isAfter(today)
                } catch (_: Exception) { false }
            }
            if (upcomingMovies.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PremiumEmptyState(
                        title = "No upcoming movies!",
                        subtitle = "We'll let you know when they're out.",
                        buttonLabel = "BROWSE",
                        onClick = { navController.navigate(Screen.Explore.route) },
                        illustration = { MovieIllustration() }
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    lazyGridItems(upcomingMovies) { item ->
                        PosterCard(item, { navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name)) })
                    }
                }
            }
        }
        else -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = AccentPrimary) }
    }
}

// ─────────────────────────────────────────────
// PROFILE SCREEN
// ─────────────────────────────────────────────
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val listsWithPreviews by viewModel.listsWithPreviews.collectAsState()
    val watchedMovies by viewModel.watchedMovies.collectAsState()
    val watchedShows by viewModel.watchedShows.collectAsState()
    val favoriteMovies by viewModel.favoriteMovies.collectAsState()
    val favoriteShows by viewModel.favoriteShows.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(28.dp)
            )
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        // Main content
        when (uiState) {
            is WatchlistUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    repeat(6) { SkeletonSection() }
                }
            }
            is WatchlistUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error loading profile", color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    // 1. Avatar and User Name
                    ProfileHeader(
                        userName = "Alok Chandra",
                        avatarUrl = null
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Stats Section (Dual Stats)
                    DualStatsRow(
                        movieCount = stats?.movieCount ?: 0,
                        showCount = stats?.tvCount ?: 0
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. Lists Section (Horizontal Previews)
                    listsWithPreviews.forEach { (listName, items) ->
                        HorizontalSection(
                            title = listName,
                            items = items,
                            onItemClick = { item ->
                                navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name))
                            },
                            onViewAllClick = {
                                navController.navigate(Screen.ViewAll.createRoute(listName, "list"))
                            },
                            emptyMessage = "This list is empty"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 4. Shows (Watched only)
                    HorizontalSection(
                        title = "Shows",
                        items = watchedShows,
                        onItemClick = { item ->
                            navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name))
                        },
                        onViewAllClick = {
                            navController.navigate(Screen.ViewAll.createRoute("Watched Shows", "tv"))
                        },
                        emptyMessage = "No watched shows yet"
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Favorite shows (Hearted only)
                    HorizontalSection(
                        title = "Favorite shows",
                        items = favoriteShows,
                        onItemClick = { item ->
                            navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name))
                        },
                        onViewAllClick = {
                            navController.navigate(Screen.ViewAll.createRoute("Favorite Shows", "favorite_tv"))
                        },
                        icon = Icons.Rounded.Favorite,
                        iconTint = HeartRed,
                        emptyMessage = "No favorite shows yet"
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. Movies (Watched only)
                    HorizontalSection(
                        title = "Movies",
                        items = watchedMovies,
                        onItemClick = { item ->
                            navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name))
                        },
                        onViewAllClick = {
                            navController.navigate(Screen.ViewAll.createRoute("Watched Movies", "movie"))
                        },
                        emptyMessage = "No watched movies yet"
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 7. Favorite movies (Hearted only)
                    HorizontalSection(
                        title = "Favorite movies",
                        items = favoriteMovies,
                        onItemClick = { item ->
                            navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name))
                        },
                        onViewAllClick = {
                            navController.navigate(Screen.ViewAll.createRoute("Favorite Movies", "favorite_movie"))
                        },
                        icon = Icons.Rounded.Favorite,
                        iconTint = HeartRed,
                        emptyMessage = "No favorite movies yet"
                    )
                }
            }
        }
    }
}
