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
        // Toolbar with Grid/List toggle and centered Group Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Centered Header
            if (groupedEpisodes.isNotEmpty()) {
                SectionHeader(
                    title = groupedEpisodes.keys.first(),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Grid/List toggle on the right
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariant)
                    .clickable { viewModel.toggleGridView() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Rounded.List else Icons.Rounded.GridView,
                    contentDescription = "Toggle View",
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
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
                        groupedEpisodes.entries.forEachIndexed { index, (header, items) ->
                            if (index > 0) {
                                item {
                                    SectionHeader(header)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
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
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedEpisodes.entries.forEachIndexed { index, (header, items) ->
                            if (index > 0) {
                                item {
                                    if (header == "HAVEN'T STARTED") {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            thickness = 2.dp,
                                            color = SurfaceVariant
                                        )
                                    }
                                    SectionHeader(header)
                                }
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = SurfaceVariant.copy(alpha = 0.5f),
            shape = CircleShape
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
            )
        }
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

            if (progress.isNew) {
                Box(modifier = Modifier.padding(4.dp)) {
                    WatchlistBadge(text = "NEW", color = AccentPrimary)
                }
            }
            
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
    val groupedUpcoming by viewModel.groupedUpcomingEpisodes.collectAsState()

    when {
        uiState is WatchlistViewModel.WatchlistUiState.Loading -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(5) { SkeletonBox(modifier = Modifier.fillMaxWidth().height(120.dp), cornerRadius = 12) }
            }
        }
        uiState is WatchlistViewModel.WatchlistUiState.Success && groupedUpcoming.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PremiumEmptyState(
                    title = "No Upcoming Episodes",
                    subtitle = "Add more shows to your watchlist to track their upcoming releases.",
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
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val order = listOf("THIS WEEK", "NEXT WEEK", "THIS MONTH", "NEXT MONTH", "LATER")
                order.forEach { groupName ->
                    val episodes = groupedUpcoming[groupName] ?: emptyList()
                    if (episodes.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = groupName
                            )
                        }
                        items(episodes, key = { it.showId + "S${it.episode.seasonNumber}E${it.episode.episodeNumber}" }) { episode ->
                            UpcomingEpisodeCard(
                                episode = episode,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onClick = { navController.navigate(Screen.Detail.createRoute(episode.showId, MediaType.TV.name)) }
                            )
                        }
                    }
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
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SurfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            // Image on the left (Poster) - Restored Size
            AsyncImage(
                model = showPosterPath,
                contentDescription = showName,
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
                // Show title Capsule at TOP
                Surface(
                    color = Background,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, SurfaceVariant),
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = showName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                // Episode Details - Positioned slightly above the bottom with spacing
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "S%02d | E%02d".format(Locale.US, episode.seasonNumber, episode.episodeNumber),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        if (progress.remainingCount > 0) {
                            Text(
                                text = " +${progress.remainingCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                        
                        // Badges
                        Row(modifier = Modifier.padding(start = 4.dp)) {
                            if (progress.isNew) {
                                WatchlistBadge(text = "NEW", color = AccentPrimary)
                            } else if (progress.isPremiere) {
                                WatchlistBadge(text = "PREMIERE", color = WatchedGreen)
                            } else if (progress.isFinale) {
                                WatchlistBadge(text = "FINALE", color = GoldAccent)
                            }
                        }
                    }

                    Text(
                        text = episode.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Light,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Checkmark Icon on the right with grey circular background - VERTICALLY CENTERED
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant.copy(alpha = 0.4f))
                    .clickable { onMarkWatched() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Mark Watched",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun WatchlistBadge(text: String, color: Color) {
    Surface(
        color = Background,
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = .2.dp)
        )
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

    val statusColor = when {
        daysAway == 0L -> AccentPrimary
        daysAway == 1L -> Color(0xFFFFA500) // Orange
        else -> TextSecondary
    }

    val airDateFormatted = remember(ep.airDate) {
        if (ep.airDate == null) "" else {
            try {
                val date = LocalDate.parse(ep.airDate)
                val dayOfWeek = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
                val datePart = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
                "$dayOfWeek, $datePart"
            } catch (_: Exception) {
                ep.airDate
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Vertical Poster on the Left
            AsyncImage(
                model = showPosterPath,
                contentDescription = showName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp, 120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceColor)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Details on the Right
            Column(modifier = Modifier.weight(1f)) {
                // Air Date & Time (Top)
                Text(
                    text = airDateFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Show Name
                Text(
                    text = showName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Season | Episode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "S%02d | E%02d".format(Locale.US, ep.seasonNumber, ep.episodeNumber),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (ep.seasonNumber == 1 && ep.episodeNumber == 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = AccentPrimary.copy(alpha = 0.2f), contentColor = AccentPrimary) {
                            Text("PREMIERE", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Episode Name
                Text(
                    text = ep.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Brief Synopsis
                Text(
                    text = ep.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
