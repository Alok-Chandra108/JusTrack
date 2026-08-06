package com.alok.justrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.model.MovieDetails
import com.alok.justrack.data.model.Season
import com.alok.justrack.data.model.Episode
import com.alok.justrack.ui.components.*
import com.alok.justrack.ui.theme.*
import com.alok.justrack.ui.viewmodel.DetailUiState
import com.alok.justrack.ui.viewmodel.DetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavController,
    id: String,
    mediaType: String = "MOVIE",
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isInWatchlist by viewModel.isInWatchlist.collectAsState()
    val isWatched by viewModel.isWatched.collectAsState()
    val isFavourite by viewModel.isFavourite.collectAsState()
    val lists by viewModel.lists.collectAsState()
    val mediaLists by viewModel.mediaLists.collectAsState()
    val posterImages by viewModel.posterImages.collectAsState()
    val backdropImages by viewModel.backdropImages.collectAsState()

    var showMoreSheet by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }
    var showPosterPicker by remember { mutableStateOf(false) }
    var showBackdropPicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(id, mediaType) {
        viewModel.loadDetail(id, mediaType)
    }

    when (val state = uiState) {
        is DetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentPrimary)
            }
        }
        is DetailUiState.Success -> {
            MovieDetailsScreen(
                movie = state.item,
                isInWatchlist = isInWatchlist,
                isWatched = isWatched,
                onBackClick = { navController.popBackStack() },
                onWatchlistToggle = { viewModel.toggleWatchlist(state.item) },
                onWatchedToggle = { viewModel.toggleWatched(state.item.id) },
                onMoreClick = { showMoreSheet = true },
                onSeasonClick = { seasonNumber ->
                    viewModel.loadSeason(seasonNumber)
                },
                onEpisodeWatchedToggle = { seasonNum, epNum, watched ->
                    viewModel.markEpisodeWatched(seasonNum, epNum, watched)
                },
                onRecommendationClick = { item ->
                    navController.navigate(com.alok.justrack.ui.navigation.Screen.Detail.createRoute(item.id, item.mediaType.name))
                }
            )

            if (showMoreSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreSheet = false },
                    sheetState = sheetState,
                    containerColor = Background,
                    dragHandle = null
                ) {
                    MoreOptionsBottomSheet(
                        isFavourite = isFavourite,
                        onDismiss = { showMoreSheet = false },
                        onFavouriteClick = { viewModel.toggleFavourite() },
                        onChangePosterClick = {
                            viewModel.loadImages()
                            showPosterPicker = true
                        },
                        onChangeBackdropClick = {
                            viewModel.loadImages()
                            showBackdropPicker = true
                        },
                        onAddToListClick = { showListPicker = true }
                    )
                }
            }

            if (showListPicker) {
                ListPickerDialog(
                    lists = lists,
                    mediaListIds = mediaLists,
                    onDismiss = { showListPicker = false },
                    onListSelected = { listId ->
                        if (listId in mediaLists) {
                            viewModel.removeFromList(listId)
                        } else {
                            viewModel.addToList(listId)
                        }
                    },
                    onCreateList = { name ->
                        viewModel.createList(name)
                    }
                )
            }

            if (showPosterPicker && posterImages.isNotEmpty()) {
                FullScreenImagePicker(
                    title = "Change Poster",
                    images = posterImages,
                    onDismiss = { showPosterPicker = false },
                    onImageSelected = { url -> viewModel.changePoster(url) }
                )
            }

            if (showBackdropPicker && backdropImages.isNotEmpty()) {
                FullScreenImagePicker(
                    title = "Change Backdrop",
                    images = backdropImages,
                    onDismiss = { showBackdropPicker = false },
                    onImageSelected = { url -> viewModel.changeBackdrop(url) }
                )
            }
        }
        is DetailUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
                Text("Error: ${state.message}", color = HeartRed)
            }
        }
    }
}

@Composable
fun MovieDetailsScreen(
    movie: MovieDetails,
    isInWatchlist: Boolean,
    isWatched: Boolean,
    onBackClick: () -> Unit,
    onWatchlistToggle: () -> Unit,
    onWatchedToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
    onSeasonClick: (Int) -> Unit = {},
    onEpisodeWatchedToggle: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    onRecommendationClick: (MediaItem) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ABOUT", "EPISODES")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            BackdropHeader(
                backdropUrl = movie.backdropPath,
                onBackClick = onBackClick,
                onShareClick = {},
                onMoreClick = onMoreClick
            )

            if (movie.mediaType == MediaType.TV) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = AccentPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = TextPrimary
                        )
                    },
                    divider = {
                        HorizontalDivider(color = SurfaceVariant, thickness = 1.dp)
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) TextPrimary else TextSecondary
                                )
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
                if (movie.mediaType == MediaType.TV) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    when (selectedTab) {
                        0 -> TvShowAboutSection(
                            movie = movie,
                            isInWatchlist = isInWatchlist,
                            isWatched = isWatched,
                            onWatchlistToggle = onWatchlistToggle,
                            onWatchedToggle = onWatchedToggle,
                            onRecommendationClick = onRecommendationClick
                        )
                        1 -> TvShowEpisodesSection(
                            seasons = movie.seasons,
                            onSeasonClick = onSeasonClick,
                            onEpisodeWatchedToggle = onEpisodeWatchedToggle
                        )
                    }
                } else {
                    // Movie Layout
                    Spacer(modifier = Modifier.height(12.dp))
                    PosterInfoRow(movie = movie)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ActionButtons(
                        isInWatchlist = isInWatchlist,
                        isWatched = isWatched,
                        releaseDate = movie.releaseDate,
                        onWatchlistToggle = onWatchlistToggle,
                        onWatchedToggle = onWatchedToggle
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    CollapsibleDescription(description = movie.overview)
                    Spacer(modifier = Modifier.height(20.dp))
                    CastSection(cast = movie.cast)
                    Spacer(modifier = Modifier.height(20.dp))
                    RecommendationsSection(recommendations = movie.recommendations, onRecommendationClick = onRecommendationClick)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TvShowAboutSection(
    movie: MovieDetails,
    isInWatchlist: Boolean,
    isWatched: Boolean,
    onWatchlistToggle: () -> Unit,
    onWatchedToggle: () -> Unit,
    onRecommendationClick: (MediaItem) -> Unit
) {
    Column {
        PosterInfoRow(movie = movie)
        Spacer(modifier = Modifier.height(16.dp))
        ActionButtons(
            isInWatchlist = isInWatchlist,
            isWatched = isWatched,
            releaseDate = movie.releaseDate,
            onWatchlistToggle = onWatchlistToggle,
            onWatchedToggle = onWatchedToggle
        )
        Spacer(modifier = Modifier.height(16.dp))
        CollapsibleDescription(description = movie.overview)
        Spacer(modifier = Modifier.height(20.dp))
        CastSection(cast = movie.cast)
        Spacer(modifier = Modifier.height(20.dp))
        RecommendationsSection(recommendations = movie.recommendations, onRecommendationClick = onRecommendationClick)
    }
}

@Composable
fun TvShowEpisodesSection(
    seasons: List<Season>,
    onSeasonClick: (Int) -> Unit,
    onEpisodeWatchedToggle: (Int, Int, Boolean) -> Unit
) {
    var expandedSeason by remember { mutableIntStateOf(-1) }

    Column {
        Text(
            text = "All episodes",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            seasons.filter { it.seasonNumber > 0 }.forEach { season ->
                val isExpanded = expandedSeason == season.seasonNumber
                SeasonCard(
                    season = season,
                    isExpanded = isExpanded,
                    onClick = {
                        if (isExpanded) {
                            expandedSeason = -1
                        } else {
                            expandedSeason = season.seasonNumber
                            onSeasonClick(season.seasonNumber)
                        }
                    },
                    onEpisodeWatchedToggle = onEpisodeWatchedToggle
                )
            }
        }
    }
}

@Composable
fun SeasonCard(
    season: Season,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onEpisodeWatchedToggle: (Int, Int, Boolean) -> Unit
) {
    val isCompleted = season.episodeCount > 0 && season.watchedCount == season.episodeCount
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicShadow(cornerRadius = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Background)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = season.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = rotationState }
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${season.watchedCount}/${season.episodeCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = if (isCompleted) "Completed" else "Mark all watched",
                    tint = if (isCompleted) WatchedGreen else TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (season.episodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentPrimary, modifier = Modifier.size(24.dp))
                    }
                } else {
                    season.episodes.forEach { episode ->
                        EpisodeRow(
                            episode = episode,
                            onWatchedToggle = { watched ->
                                onEpisodeWatchedToggle(season.seasonNumber, episode.episodeNumber, watched)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeRow(
    episode: Episode,
    onWatchedToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = episode.stillPath,
            contentDescription = episode.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(100.dp, 60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceColor)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "S${episode.seasonNumber.toString().padStart(2, '0')} | E${episode.episodeNumber.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = episode.name,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Circular Solid Check Button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (episode.isWatched) WatchedGreen else TextSecondary.copy(alpha = 0.1f))
                .clickable { onWatchedToggle(!episode.isWatched) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Mark Watched",
                tint = if (episode.isWatched) Color.White else TextSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
