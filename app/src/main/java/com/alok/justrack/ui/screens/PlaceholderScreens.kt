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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MoviesScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabTitles = listOf("WATCHLIST", "UPCOMING")
    var selectedTab by remember { mutableIntStateOf(0) }

    PremiumTabScaffold(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        tabTitles = tabTitles
    ) { tab ->
        when (tab) {
            0 -> MovieWatchlistTabContent(
                uiState = uiState, 
                viewModel = viewModel, 
                navController = navController,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
            1 -> MovieUpcomingTabContent(
                uiState = uiState, 
                viewModel = viewModel, 
                navController = navController,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MovieWatchlistTabContent(
    uiState: WatchlistUiState,
    viewModel: WatchlistViewModel,
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val isGridView by viewModel.isMovieGridView.collectAsState()
    val movies = if (uiState is WatchlistUiState.Success) {
        uiState.items.filter { it.mediaType == MediaType.MOVIE && it.inWatchlist }
    } else emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        WatchlistToolbar(
            title = "MY MOVIES",
            isGridView = isGridView,
            onToggleView = { viewModel.toggleMovieGridView() }
        )

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
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(movies, key = { it.id }) { movie ->
                            PosterOnlyCard(
                                item = movie,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
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
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onClick = { navController.navigate(Screen.Detail.createRoute(movie.id, movie.mediaType.name)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MovieUpcomingTabContent(
    uiState: WatchlistUiState,
    viewModel: WatchlistViewModel,
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
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
                            CapsuleHeader(
                                title = groupName
                            )
                        }
                        items(movies, key = { it.id }) { movie ->
                            UpcomingMovieCard(
                                movie = movie,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MovieWatchlistCard(
    movie: MediaItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            with(sharedTransitionScope) {
                AsyncImage(
                    model = movie.posterPath,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .sharedElement(
                            rememberSharedContentState(key = "poster-${movie.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .size(68.dp, 98.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = movie.releaseDate.split("-").firstOrNull() ?: "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", movie.rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UpcomingMovieCard(
    movie: MediaItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val releaseDateFormatted = remember(movie.releaseDate) {
        val date = com.alok.justrack.util.DateUtils.parseDate(movie.releaseDate)
        if (date == null) "" else {
            val dayOfWeek = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            val datePart = date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
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
        daysAway == 0L -> MaterialTheme.colorScheme.primary
        daysAway == 1L -> Color(0xFFFFA500) // Orange
        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
            with(sharedTransitionScope) {
                AsyncImage(
                    model = movie.posterPath,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .sharedElement(
                            rememberSharedContentState(key = "poster-${movie.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .size(60.dp, 90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = movie.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (daysAway == 1L) "DAY" else "DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp
                    )
                }
            } else if (daysAway == 0L) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Rounded.Celebration,
                    contentDescription = "Released Today",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// PROFILE SCREEN
// ─────────────────────────────────────────────
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
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
            .background(MaterialTheme.colorScheme.background)
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
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

                    // 2. Stats Section (Horizontal Scrollable)
                    stats?.let { statsData ->
                        StatsHorizontalRow(stats = statsData)
                    } ?: run {
                        DualStatsRow(
                            movieCount = stats?.movieCount ?: 0,
                            showCount = stats?.tvCount ?: 0
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. Lists Section (Horizontal Previews)
                    listsWithPreviews.forEach { (listName, items) ->
                        HorizontalSection(
                            title = listName,
                            items = items,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
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

                    // 4. Shows (Watched & In Progress)
                    HorizontalSection(
                        title = "Shows",
                        items = watchedShows,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = { item ->
                            navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name))
                        },
                        onViewAllClick = {
                            navController.navigate(Screen.ViewAll.createRoute("Shows", "tv"))
                        },
                        emptyMessage = "No shows in progress or watched"
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Favorite shows (Hearted only)
                    HorizontalSection(
                        title = "Favorite shows",
                        items = favoriteShows,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = { item ->
                            navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name))
                        },
                        onViewAllClick = {
                            navController.navigate(Screen.ViewAll.createRoute("Favorite Shows", "favorite_tv"))
                        },
                        icon = Icons.Rounded.Favorite,
                        iconTint = MaterialTheme.colorScheme.error,
                        emptyMessage = "No favorite shows yet"
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. Movies (Watched only)
                    HorizontalSection(
                        title = "Movies",
                        items = watchedMovies,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
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
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = { item ->
                            navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name))
                        },
                        onViewAllClick = {
                            navController.navigate(Screen.ViewAll.createRoute("Favorite Movies", "favorite_movie"))
                        },
                        icon = Icons.Rounded.Favorite,
                        iconTint = MaterialTheme.colorScheme.error,
                        emptyMessage = "No favorite movies yet"
                    )
                }
            }
        }
    }
}
