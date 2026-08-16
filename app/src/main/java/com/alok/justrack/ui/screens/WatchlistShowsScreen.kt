package com.alok.justrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun WatchlistShowsScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: WatchlistViewModel = hiltViewModel(),
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
            0 -> WatchlistTabContent(
                uiState = uiState, 
                viewModel = viewModel, 
                navController = navController,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
            1 -> UpcomingTabContent(
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
private fun WatchlistTabContent(
    uiState: WatchlistViewModel.WatchlistUiState,
    viewModel: WatchlistViewModel,
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val groupedEpisodes by viewModel.groupedWatchlistEpisodes.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (groupedEpisodes.isNotEmpty()) {
            WatchlistToolbar(
                title = groupedEpisodes.keys.first(),
                isGridView = isGridView,
                onToggleView = { viewModel.toggleGridView() }
            )
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
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        groupedEpisodes.entries.forEachIndexed { index, (header, items) ->
                            if (index > 0) {
                                item {
                                    CapsuleHeader(header)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                            item {
                                // Nested Grid-like layout in LazyColumn to support headers
                                val chunkedItems = items.chunked(3)
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    chunkedItems.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowItems.forEach { progress ->
                                                EpisodeGridItem(
                                                    progress = progress,
                                                    sharedTransitionScope = sharedTransitionScope,
                                                    animatedVisibilityScope = animatedVisibilityScope,
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
                                Spacer(modifier = Modifier.height(20.dp))
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
                                    if (header == "HAVEN'T STARTED" || header == "WATCH LATER") {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            thickness = 2.dp,
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                    CapsuleHeader(header)
                                }
                            }
                            items(items, key = { it.showId }) { progress ->
                                EpisodeTrackingCard(
                                    progress = progress,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onMarkWatched = {
                                        val ep = progress.episode
                                        viewModel.markEpisodeWatched(progress.showId, ep.seasonNumber, ep.episodeNumber)
                                    },
                                    onWatchLaterToggle = {
                                        viewModel.toggleWatchLater(progress.showId, progress.isWatchLater)
                                    },
                                    onStoppedWatching = {
                                        viewModel.removeFromWatchlist(progress.showId)
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EpisodeGridItem(
    progress: WatchlistViewModel.WatchlistEpisodeItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    with(sharedTransitionScope) {
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
                        .sharedElement(
                            rememberSharedContentState(key = "poster-${progress.showId}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                if (progress.isNew) {
                    Box(modifier = Modifier.padding(4.dp)) {
                        WatchlistBadge(text = "NEW", color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                if (progress.isSyncing) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
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
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun UpcomingTabContent(
    uiState: WatchlistViewModel.WatchlistUiState,
    viewModel: WatchlistViewModel,
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
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
                val order = listOf("TODAY", "THIS WEEK", "NEXT WEEK", "THIS MONTH", "NEXT MONTH", "LATER")
                order.forEach { groupName ->
                    val episodes = groupedUpcoming[groupName] ?: emptyList()
                    if (episodes.isNotEmpty()) {
                        item {
                            CapsuleHeader(
                                title = groupName
                            )
                        }
                        items(episodes, key = { it.showId + "S${it.episode.seasonNumber}E${it.episode.episodeNumber}" }) { episode ->
                            UpcomingEpisodeCard(
                                episode = episode,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EpisodeTrackingCard(
    progress: WatchlistViewModel.WatchlistEpisodeItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMarkWatched: () -> Unit,
    onWatchLaterToggle: () -> Unit,
    onStoppedWatching: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val episode = progress.episode
    val showName = progress.showName
    val showPosterPath = progress.showPosterPath

    // Animation state for Color Swipe (Sheen)
    var isSwiping by remember(progress.showId) { mutableStateOf(false) }

    val swipeProgress by animateFloatAsState(
        targetValue = if (isSwiping) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "swipe_progress"
    )

    // Trigger data update
    LaunchedEffect(isSwiping) {
        if (isSwiping) {
            kotlinx.coroutines.delay(180) // Timed with glint reaching text area
            onMarkWatched()
            kotlinx.coroutines.delay(320)
            isSwiping = false
        }
    }

    // Swipe to Watch Later State
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // --- Background Icons ---
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        offsetX.value < 0 -> if (progress.isWatchLater) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        offsetX.value > 0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else -> Color.Transparent
                    }
                )
                .padding(horizontal = 24.dp)
        ) {
            // Watch Later (Left Swipe - appears on right)
            if (offsetX.value < 0) {
                val iconScale by animateFloatAsState(
                    targetValue = if (offsetX.value < -200f) 1.2f else 0.8f,
                    label = "icon_scale_left"
                )
                Icon(
                    imageVector = if (progress.isWatchLater) Icons.Rounded.RestartAlt else Icons.Rounded.WatchLater,
                    contentDescription = "Watch Later",
                    tint = if (progress.isWatchLater) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterEnd)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            alpha = (kotlin.math.abs(offsetX.value) / 300f).coerceIn(0f, 1f)
                        }
                )
            }

            // Stopped Watching (Right Swipe - appears on left)
            if (offsetX.value > 0) {
                val iconScale by animateFloatAsState(
                    targetValue = if (offsetX.value > 200f) 1.2f else 0.8f,
                    label = "icon_scale_right"
                )
                Icon(
                    imageVector = Icons.Rounded.VisibilityOff,
                    contentDescription = "Stopped Watching",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterStart)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            alpha = (offsetX.value / 300f).coerceIn(0f, 1f)
                        }
                )
            }
        }

        // --- Foreground Card ---
        Surface(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    if (swipeProgress > 0f && swipeProgress < 1f) {
                        // Diagonal sheen glint
                        val width = size.width
                        val height = size.height
                        val xOffset = width * 1.5f * swipeProgress - width * 0.25f
                        
                        val brush = Brush.linearGradient(
                            0.0f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.25f),
                            1.0f to Color.Transparent,
                            start = Offset(xOffset, 0f),
                            end = Offset(xOffset + width * 0.3f, height)
                        )
                        drawRect(brush = brush)
                    }
                }
                .pointerInput(progress.showId) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                // Allow bidirectional swipe
                                val newOffset = offsetX.value + dragAmount
                                offsetX.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -400f) {
                                    // Full swipe Left -> Watch Later
                                    offsetX.animateTo(-size.width.toFloat(), tween(300))
                                    onWatchLaterToggle()
                                    kotlinx.coroutines.delay(500)
                                    offsetX.snapTo(0f)
                                } else if (offsetX.value > 400f) {
                                    // Full swipe Right -> Stopped Watching
                                    offsetX.animateTo(size.width.toFloat(), tween(300))
                                    onStoppedWatching()
                                    kotlinx.coroutines.delay(500)
                                    offsetX.snapTo(0f)
                                } else if (offsetX.value < -150f) {
                                    // Partial swipe Left -> Reveal Watch Later icon
                                    offsetX.animateTo(-250f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                } else if (offsetX.value > 150f) {
                                    // Partial swipe Right -> Reveal Stopped icon
                                    offsetX.animateTo(250f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                } else {
                                    // Cancel swipe
                                    offsetX.animateTo(0f, spring())
                                }
                            }
                        }
                    )
                }
                .clickable { onClick() },
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Image on the left (Poster)
                    with(sharedTransitionScope) {
                        AsyncImage(
                            model = showPosterPath,
                            contentDescription = showName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "poster-${progress.showId}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                                .size(68.dp, 98.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
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
                        // Show title Capsule at TOP
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.clickable { onClick() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = showName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }

                        // Episode Details
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AnimatedContent(
                                targetState = if (isSwiping && swipeProgress < 0.5f) null else (episode to progress.remainingCount),
                                transitionSpec = {
                                    if (targetState == null) {
                                        EnterTransition.None togetherWith (fadeOut(tween(120)) + slideOutHorizontally { -20 })
                                    } else {
                                        (fadeIn(tween(180)) + slideInHorizontally { 20 }) togetherWith ExitTransition.None
                                    }
                                },
                                label = "episode_header_transition"
                            ) { state ->
                                if (state != null) {
                                    val (targetEpisode, targetRemaining) = state
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "S%02d | E%02d".format(Locale.US, targetEpisode.seasonNumber, targetEpisode.episodeNumber),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        if (targetRemaining > 0) {
                                            Text(
                                                text = " +$targetRemaining",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp
                                            )
                                        }

                                        // Badges
                                        Row(modifier = Modifier.padding(start = 4.dp)) {
                                            if (progress.isNew) {
                                                WatchlistBadge(text = "NEW", color = MaterialTheme.colorScheme.primary)
                                            } else if (progress.isPremiere) {
                                                WatchlistBadge(text = "PREMIERE", color = WatchedGreen)
                                            } else if (progress.isFinale) {
                                                WatchlistBadge(text = "FINALE", color = MaterialTheme.colorScheme.tertiary)
                                            }
                                        }
                                    }
                                }
                            }

                            AnimatedContent(
                                targetState = if (isSwiping && swipeProgress < 0.5f) null else if (progress.isSyncing) "Fetching data..." else episode.name,
                                transitionSpec = {
                                    if (targetState == null) {
                                        EnterTransition.None togetherWith (fadeOut(tween(120)) + slideOutHorizontally { -20 })
                                    } else {
                                        (fadeIn(tween(180)) + slideInHorizontally { 20 }) togetherWith ExitTransition.None
                                    }
                                },
                                label = "episode_name_transition"
                            ) { targetName ->
                                if (targetName != null) {
                                    Text(
                                        text = targetName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Light,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable(enabled = !progress.isSyncing && !isSwiping) {
                                isSwiping = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (progress.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Mark Watched",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
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
fun UpcomingEpisodeCard(
    episode: WatchlistViewModel.UpcomingEpisodeItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showName = episode.showName
    val showPosterPath = episode.showPosterPath
    val ep = episode.episode
    val daysAway = episode.daysAway

    val statusColor = when {
        daysAway == 0L -> MaterialTheme.colorScheme.primary
        daysAway == 1L -> Color(0xFFFFA500) // Orange
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val airDateFormatted = remember(ep.airDate) {
        val date = com.alok.justrack.util.DateUtils.parseDate(ep.airDate)
        if (date == null) "" else {
            val dayOfWeek = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            val datePart = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
            "$dayOfWeek, $datePart"
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical Poster on the Left
            with(sharedTransitionScope) {
                AsyncImage(
                    model = showPosterPath,
                    contentDescription = showName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .sharedElement(
                            rememberSharedContentState(key = "poster-${episode.showId}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .size(60.dp, 90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details in the Middle
            Column(modifier = Modifier.weight(1f)) {
                // Air Date & Time (Top)
                Text(
                    text = if (daysAway == 0L) "AIRING TODAY" else airDateFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Show Name
                Text(
                    text = showName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Season | Episode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "S%02d | E%02d".format(Locale.US, ep.seasonNumber, ep.episodeNumber),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    
                    if (ep.seasonNumber == 1 && ep.episodeNumber == 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        WatchlistBadge(text = "PREMIERE", color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Episode Name
                Text(
                    text = ep.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Compact Countdown on the Right
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
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = "Airing Today",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
