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
    val episodeProgress by viewModel.watchlistEpisodes.collectAsState()

    when {
        uiState is WatchlistViewModel.WatchlistUiState.Loading -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(5) { SkeletonBox(modifier = Modifier.fillMaxWidth().height(120.dp), cornerRadius = 12) }
            }
        }
        uiState is WatchlistViewModel.WatchlistUiState.Success && episodeProgress.isEmpty() -> {
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(episodeProgress, key = { it.showId }) { progress ->
                    EpisodeTrackingCard(
                        progress = progress,
                        onMarkWatched = {
                            val ep = progress.episode
                            viewModel.markEpisodeWatched(progress.showId, ep.seasonNumber, ep.episodeNumber)
                        },
                        onClick = { navController.navigate(Screen.Detail.createRoute(progress.showId, MediaType.TV.name)) }
                    )
                }
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

    // Neumorphic 'pop' animation container would go here
    NeuCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = episode.stillPath ?: showPosterPath,
                contentDescription = episode.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp, 60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceColor)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (episode.seasonNumber == 1 && episode.episodeNumber == 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = AccentPrimary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PREMIERE",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentPrimary,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "S%02dE%02d".format(Locale.US, episode.seasonNumber, episode.episodeNumber),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "S%02dE%02d".format(Locale.US, episode.seasonNumber, episode.episodeNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = episode.name,
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
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Neumorphic Check Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .neumorphicShadow(cornerRadius = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Background)
                    .clickable { onMarkWatched() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Mark Watched",
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = showName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "S%02dE%02d".format(Locale.US, ep.seasonNumber, ep.episodeNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ep.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = countdownText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (daysAway == 0L) AccentPrimary else TextSecondary,
                        fontWeight = FontWeight.Bold
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
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
