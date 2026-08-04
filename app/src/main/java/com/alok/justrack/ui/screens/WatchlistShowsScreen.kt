package com.alok.justrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.ui.components.*
import com.alok.justrack.ui.navigation.Screen
import com.alok.justrack.ui.theme.*
import com.alok.justrack.ui.viewmodel.WatchlistViewModel
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
            containerColor = Background,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    SecondaryIndicator(
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
            label = "tab_content"
        ) { tab ->
            when (tab) {
                0 -> WatchlistTabContent(uiState = uiState, viewModel = viewModel, navController = navController)
                1 -> UpcomingTabContent(uiState = uiState, viewModel = viewModel, navController = navController)
            }
        }
    }
}

@Composable
private fun WatchlistTabContent(
    uiState: WatchlistViewModel.WatchlistUiState,
    viewModel: WatchlistViewModel,
    navController: NavController
) {
    val groupedEpisodes by viewModel.groupedWatchlistEpisodes.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar with Grid/List toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.toggleGridView() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant)
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Rounded.List else Icons.Rounded.GridView,
                    contentDescription = "Toggle View",
                    tint = TextPrimary
                )
            }
        }

        when {
            uiState is WatchlistViewModel.WatchlistUiState.Loading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(5) { SkeletonBox(modifier = Modifier.fillMaxWidth().height(120.dp), cornerRadius = 12) }
                }
            }
            uiState is WatchlistViewModel.WatchlistUiState.Success && groupedEpisodes.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PremiumEmptyState(
                        title = "Your watchlist is empty!",
                        subtitle = "Add shows you want to watch.",
                        buttonLabel = "BROWSE",
                        onClick = { navController.navigate(Screen.Explore.route) },
                        illustration = { TvShowIllustration() }
                    )
                }
            }
            else -> {
                if (isGridView) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        groupedEpisodes.forEach { (header, items) ->
                            item {
                                SectionHeader(header)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            item {
                                // Nested Grid-like layout in LazyColumn to support headers
                                val chunkedItems = items.chunked(3)
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    chunkedItems.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowItems.forEach { progress ->
                                                EpisodeGridItem(
                                                    progress = progress,
                                                    modifier = Modifier.weight(1f),
                                                    onClick = { navController.navigate(Screen.Detail.createRoute(progress.showId, MediaType.TV.name)) }
                                                )
                                            }
                                            // Fill empty slots in the last row
                                            repeat(3 - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        groupedEpisodes.forEach { (header, items) ->
                            item {
                                SectionHeader(header)
                            }
                            items(items, key = { it.showId }) { progress ->
                                EpisodeTrackingCard(
                                    progress = progress,
                                    onMarkWatched = {
                                        val ep = progress.episode
                                        viewModel.markEpisodeWatched(progress.showId, ep.seasonNumber, ep.episodeNumber)
                                    },
                                    onClick = { navController.navigate(Screen.Detail.createRoute(progress.showId, MediaType.TV.name)) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Surface(
        color = SurfaceVariant,
        shape = CircleShape,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun EpisodeGridItem(
    progress: WatchlistViewModel.WatchlistEpisodeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Box {
            AsyncImage(
                model = progress.showPosterPath,
                contentDescription = progress.showName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant)
            )
            
            // Progress Bar at the bottom
            val progressPercent = if (progress.totalCount > 0) progress.watchedCount.toFloat() / progress.totalCount else 0f
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressPercent)
                        .fillMaxHeight()
                        .background(AccentPrimary)
                )
            }
        }
    }
}

@Composable
private fun UpcomingTabContent(
    uiState: WatchlistViewModel.WatchlistUiState,
    viewModel: WatchlistViewModel,
    navController: NavController
) {
    val upcomingEpisodes by viewModel.upcomingEpisodes.collectAsState()

    when {
        uiState is WatchlistViewModel.WatchlistUiState.Loading -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(5) { SkeletonBox(modifier = Modifier.fillMaxWidth().height(120.dp), cornerRadius = 12) }
            }
        }
        uiState is WatchlistViewModel.WatchlistUiState.Success && upcomingEpisodes.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PremiumEmptyState(
                    title = "No Upcoming Episodes",
                    subtitle = "We'll notify you when shows in your watchlist have new episodes scheduled.",
                    buttonLabel = "BROWSE",
                    onClick = { navController.navigate(Screen.Explore.route) },
                    illustration = { TvShowIllustration() }
                )
            }
        }
        uiState is WatchlistViewModel.WatchlistUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.message}", color = MaterialTheme.colorScheme.error)
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(upcomingEpisodes, key = { it.showId + "S${it.episode.seasonNumber}E${it.episode.episodeNumber}" }) { episode ->
                    UpcomingEpisodeCard(
                        episode = episode,
                        onClick = { navController.navigate(Screen.Detail.createRoute(episode.showId, MediaType.TV.name)) }
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeTrackingCard(
    progress: WatchlistViewModel.WatchlistEpisodeItem,
    onMarkWatched: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val episode = progress.episode
    val showName = progress.showName
    val showPosterPath = progress.showPosterPath

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Background,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SurfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            // Image on the left (Poster-like aspect)
            AsyncImage(
                model = showPosterPath,
                contentDescription = showName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp, 100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceColor)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Show title Capsule
                Surface(
                    color = Background,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, SurfaceVariant),
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = showName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "S%02d | E%02d".format(Locale.US, episode.seasonNumber, episode.episodeNumber),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = episode.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Checkmark Icon on the far right
            IconButton(onClick = onMarkWatched) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Mark Watched",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun UpcomingEpisodeCard(
    episode: WatchlistViewModel.UpcomingEpisodeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showName = episode.showName
    val showPosterPath = episode.showPosterPath
    val ep = episode.episode
    val daysAway = episode.daysAway

    val countdownText = when {
        daysAway == null -> ""
        daysAway == 0L -> "Available Today"
        daysAway == 1L -> "Tomorrow"
        daysAway < 0 -> "Released ${(-daysAway).toInt()} days ago"
        else -> "Releases in $daysAway days"
    }

    NeuCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                AsyncImage(
                    model = ep.stillPath ?: showPosterPath,
                    contentDescription = ep.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp, 68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceColor)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "S%02d | E%02d".format(Locale.US, ep.seasonNumber, ep.episodeNumber),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = ep.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = showName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = countdownText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (daysAway == 0L) AccentPrimary else TextSecondary,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (ep.airDate != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val airDateString = try {
                    LocalDate.parse(ep.airDate).format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US))
                } catch (_: Exception) {
                    ep.airDate
                }
                Text(
                    text = airDateString,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
