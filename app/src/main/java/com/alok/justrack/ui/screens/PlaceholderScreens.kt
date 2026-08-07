package com.alok.justrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.ui.components.*
import com.alok.justrack.ui.navigation.Screen
import com.alok.justrack.ui.theme.*
import com.alok.justrack.ui.viewmodel.*
import com.alok.justrack.ui.viewmodel.WatchlistViewModel.WatchlistUiState
import java.util.Locale

// ─────────────────────────────────────────────
// MOVIES SCREEN
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
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
        // --- Premium Tab Row ---
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
            label = "movie_tab_content"
        ) { tab ->
            when (tab) {
                0 -> MovieWatchlistTabContent(uiState = uiState, viewModel = viewModel, navController = navController)
                1 -> MovieUpcomingTabContent(uiState = uiState, viewModel = viewModel, navController = navController)
            }
        }
    }
}

@Composable
private fun MovieWatchlistTabContent(
    uiState: WatchlistUiState,
    viewModel: WatchlistViewModel,
    navController: NavController
) {
    val isGridView by viewModel.isMovieGridView.collectAsState()
    val movies = if (uiState is WatchlistUiState.Success) {
        uiState.items.filter { it.mediaType == MediaType.MOVIE && it.inWatchlist }
    } else emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar with Grid/List toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Centered Header
            SectionHeader(
                title = "MY MOVIES",
                modifier = Modifier.align(Alignment.Center)
            )

            // Grid/List toggle on the right
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariant)
                    .clickable { viewModel.toggleMovieGridView() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Rounded.List else Icons.Rounded.GridView,
                    contentDescription = "Toggle View",
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        when {
            uiState is WatchlistUiState.Loading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(5) { SkeletonBox(modifier = Modifier.fillMaxWidth().height(120.dp), cornerRadius = 12) }
                }
            }
            uiState is WatchlistUiState.Success && movies.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PremiumEmptyState(
                        title = "Your watchlist is empty!",
                        subtitle = "Add movies you want to watch.",
                        buttonLabel = "BROWSE",
                        onClick = { navController.navigate(Screen.Explore.route) },
                        illustration = { MovieIllustration() }
                    )
                }
            }
            else -> {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(movies, key = { it.id }) { movie ->
                            PosterOnlyCard(
                                item = movie,
                                onClick = { navController.navigate(Screen.Detail.createRoute(movie.id, movie.mediaType.name)) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(movies, key = { it.id }) { movie ->
                            MovieWatchlistCard(
                                movie = movie,
                                onClick = { navController.navigate(Screen.Detail.createRoute(movie.id, movie.mediaType.name)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieUpcomingTabContent(
    uiState: WatchlistUiState,
    viewModel: WatchlistViewModel,
    navController: NavController
) {
    val groupedUpcoming by viewModel.groupedUpcomingMovies.collectAsState()

    when {
        uiState is WatchlistUiState.Loading -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(5) { SkeletonBox(modifier = Modifier.fillMaxWidth().height(120.dp), cornerRadius = 12) }
            }
        }
        uiState is WatchlistUiState.Success && groupedUpcoming.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PremiumEmptyState(
                    title = "No Upcoming Movies",
                    subtitle = "Add more movies to your watchlist to track their release dates.",
                    buttonLabel = "BROWSE",
                    onClick = { navController.navigate(Screen.Explore.route) },
                    illustration = { MovieIllustration() }
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val order = listOf("TODAY", "THIS WEEK", "NEXT WEEK", "THIS MONTH", "NEXT MONTH", "LATER")
                order.forEach { groupName ->
                    val movies = groupedUpcoming[groupName] ?: emptyList()
                    if (movies.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = groupName
                            )
                        }
                        items(movies, key = { it.id }) { movie ->
                            UpcomingMovieCard(
                                movie = movie,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onClick = { navController.navigate(Screen.Detail.createRoute(movie.id, movie.mediaType.name)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovieWatchlistCard(
    movie: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Background,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SurfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            AsyncImage(
                model = movie.posterPath,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp, 98.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceColor)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(98.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = movie.releaseDate.split("-").firstOrNull() ?: "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", movie.rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun UpcomingMovieCard(
    movie: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val releaseDateFormatted = remember(movie.releaseDate) {
        val date = com.alok.justrack.util.DateUtils.parseDate(movie.releaseDate)
        if (date == null) "" else {
            val dayOfWeek = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            val datePart = date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.US))
            "$dayOfWeek, $datePart"
        }
    }
    
    val daysAway = remember(movie.releaseDate) {
        val date = com.alok.justrack.util.DateUtils.parseDate(movie.releaseDate)
        if (date != null) {
            java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), date)
        } else null
    }

    val statusColor = when {
        daysAway == 0L -> AccentPrimary
        daysAway == 1L -> Color(0xFFFFA500) // Orange
        else -> TextSecondary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = movie.posterPath,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp, 90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (daysAway == 0L) "RELEASING TODAY" else releaseDateFormatted.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = movie.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }

            if (daysAway != null && daysAway > 0) {
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(44.dp)
                ) {
                    Text(
                        text = daysAway.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (daysAway == 1L) "DAY" else "DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp
                    )
                }
            } else if (daysAway == 0L) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Rounded.Celebration,
                    contentDescription = "Released Today",
                    tint = AccentPrimary,
                    modifier = Modifier.size(24.dp)
                )
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
