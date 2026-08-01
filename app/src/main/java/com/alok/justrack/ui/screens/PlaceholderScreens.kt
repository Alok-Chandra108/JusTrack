package com.alok.justrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.ui.components.*
import com.alok.justrack.ui.navigation.Screen
import com.alok.justrack.ui.theme.*
import com.alok.justrack.ui.viewmodel.*
import java.util.Locale

// ─────────────────────────────────────────────
// MOVIES SCREEN
// ─────────────────────────────────────────────
@Composable
fun MoviesScreen(
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
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Black,
            contentColor = Color.White,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
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

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(tween(500)) togetherWith fadeOut(tween(500))
            },
            label = "movie_tabs"
        ) { tab ->
            when (tab) {
                0 -> MovieWatchlistContent(uiState, navController)
                1 -> MovieUpcomingContent(navController)
            }
        }
    }
}

@Composable
private fun MovieWatchlistContent(uiState: WatchlistUiState, navController: NavController) {
    when (uiState) {
        is WatchlistUiState.Success -> {
            val movies = uiState.items.filter { it.mediaType == MediaType.MOVIE }
            if (movies.isEmpty()) {
                PremiumEmptyState(
                    title = "Your watchlist is empty!",
                    subtitle = "Add movies you want to watch.",
                    buttonLabel = "BROWSE ALL MOVIES",
                    onClick = { navController.navigate(Screen.Explore.route) },
                    icon = Icons.Rounded.Movie
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    lazyGridItems(movies, key = { it.id }) { item ->
                        PosterCard(item, { navController.navigate(Screen.Detail.createRoute(item.id)) })
                    }
                }
            }
        }
        else -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color.White) }
    }
}

@Composable
private fun MovieUpcomingContent(navController: NavController) {
    PremiumEmptyState(
        title = "No upcoming movies!",
        subtitle = "We'll let you know when they're out.",
        buttonLabel = "BROWSE ALL MOVIES",
        onClick = { navController.navigate(Screen.Explore.route) },
        icon = Icons.Rounded.CalendarMonth
    )
}

// ─────────────────────────────────────────────
// EXPLORE SCREEN
// ─────────────────────────────────────────────
@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Search bar
        TextField(
            value = query,
            onValueChange = { viewModel.onQueryChange(it) },
            placeholder = { Text("Search movies, shows...", color = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceColor,
                unfocusedContainerColor = SurfaceColor,
                disabledContainerColor = SurfaceColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = AccentPrimary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(28.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = TextSecondary)
                    }
                }
            }
        )

        when (val state = uiState) {
            is SearchUiState.Idle -> {
                PremiumEmptyState(
                    title = "Discover Something New",
                    subtitle = "Search for millions of movies and TV shows from around the world.",
                    buttonLabel = "Popular",
                    onClick = { /* Could trigger popular search */ },
                    icon = Icons.Rounded.Explore
                )
            }
            is SearchUiState.Loading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(5) { SkeletonBox(modifier = Modifier.fillMaxWidth().height(100.dp), cornerRadius = 12) }
                }
            }
            is SearchUiState.Success -> {
                val items = state.items
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No results found", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        lazyItems(items, key = { it.id }) { item ->
                            ListMediaCard(
                                item = item,
                                onClick = { navController.navigate(Screen.Detail.createRoute(item.id)) }
                            )
                        }
                    }
                }
            }
            is SearchUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// PROFILE SCREEN
// ─────────────────────────────────────────────
@Composable
fun ProfileScreen(
    navController: NavController,
    watchlistViewModel: WatchlistViewModel = hiltViewModel()
) {
    val watchlistState by watchlistViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                tint = GoldAccent,
                modifier = Modifier.size(28.dp)
            )
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            // --- Social Stats Row ---
            SocialStatsRow(following = 0, followers = 0, comments = 0)

            Spacer(modifier = Modifier.height(8.dp))

            when (val state = watchlistState) {
                is WatchlistUiState.Loading -> {
                    repeat(3) { SkeletonSection() }
                }
                is WatchlistUiState.Success -> {
                    val items = state.items
                    
                    // --- Stats Section ---
                    HorizontalSection(
                        title = "Stats",
                        items = emptyList(),
                        onItemClick = {},
                        onViewAllClick = {}
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Lists Section ---
                    HorizontalSection(
                        title = "Lists",
                        items = emptyList(),
                        onItemClick = {},
                        onViewAllClick = {}
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Shows Section ---
                    HorizontalSection(
                        title = "Shows",
                        items = items.filter { it.mediaType == MediaType.TV },
                        onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                        onViewAllClick = { navController.navigate(Screen.ViewAll.createRoute("Shows", "series")) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Favorite Shows Section ---
                    HorizontalSection(
                        title = "Favorite shows",
                        items = emptyList(),
                        onItemClick = {},
                        onViewAllClick = {},
                        icon = Icons.Rounded.Favorite,
                        iconTint = HeartRed
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Movies Section ---
                    HorizontalSection(
                        title = "Movies",
                        items = items.filter { it.mediaType == MediaType.MOVIE },
                        onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id)) },
                        onViewAllClick = { navController.navigate(Screen.ViewAll.createRoute("Movies", "movies")) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Favorite Movies Section ---
                    HorizontalSection(
                        title = "Favorite movies",
                        items = emptyList(),
                        onItemClick = {},
                        onViewAllClick = {},
                        icon = Icons.Rounded.Favorite,
                        iconTint = HeartRed
                    )
                }
                is WatchlistUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error loading profile", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// DETAIL SCREEN REMOVED - MOVED TO MovieDetailsScreen.kt
