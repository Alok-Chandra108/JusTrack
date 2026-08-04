package com.alok.justrack.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
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
    val selectedSeason by viewModel.selectedSeason.collectAsState()

    var showMoreSheet by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }
    var showPosterPicker by remember { mutableStateOf(false) }
    var showBackdropPicker by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val episodeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    showEpisodeSheet = true
                },
                onRecommendationClick = { item ->
                    navController.navigate(com.alok.justrack.ui.navigation.Screen.Detail.createRoute(item.id, item.mediaType.name))
                }
            )

            if (showEpisodeSheet && selectedSeason != null) {
                ModalBottomSheet(
                    onDismissRequest = { showEpisodeSheet = false },
                    sheetState = episodeSheetState,
                    containerColor = Background,
                    dragHandle = null
                ) {
                    EpisodeListBottomSheet(
                        season = selectedSeason!!,
                        onEpisodeWatchedToggle = { seasonNum, epNum, watched ->
                            viewModel.markEpisodeWatched(seasonNum, epNum, watched)
                        },
                        onDismiss = { showEpisodeSheet = false }
                    )
                }
            }

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
    onRecommendationClick: (MediaItem) -> Unit = {}
) {
    val scrollState = rememberScrollState()

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

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
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
                
                if (movie.mediaType == MediaType.TV && movie.seasons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    SeasonsSection(seasons = movie.seasons, onSeasonClick = onSeasonClick)
                }

                Spacer(modifier = Modifier.height(20.dp))
                CastSection(cast = movie.cast)
                Spacer(modifier = Modifier.height(20.dp))
                RecommendationsSection(recommendations = movie.recommendations, onRecommendationClick = onRecommendationClick)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SeasonsSection(
    seasons: List<Season>,
    onSeasonClick: (Int) -> Unit
) {
    Column {
        Text(
            text = "Seasons",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            seasons.forEach { season ->
                SeasonCard(season = season, onClick = { onSeasonClick(season.seasonNumber) })
            }
        }
    }
}

@Composable
fun SeasonCard(
    season: Season,
    onClick: () -> Unit
) {
    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = season.posterPath,
                contentDescription = season.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp, 90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceColor)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = season.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${season.episodeCount} Episodes",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress Bar
                val progress = if (season.episodeCount > 0) season.watchedCount.toFloat() / season.episodeCount else 0f
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${season.watchedCount}/${season.episodeCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = AccentPrimary,
                        trackColor = SurfaceColor
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeListBottomSheet(
    season: Season,
    onEpisodeWatchedToggle: (Int, Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .background(Background)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = season.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Episodes",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextPrimary)
                }
            }
            
            HorizontalDivider(color = SurfaceColor, thickness = 1.dp)
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(season.episodes) { episode ->
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
                text = "EP ${episode.episodeNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = episode.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Circular Check Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .neumorphicShadow(cornerRadius = 18.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (episode.isWatched) AccentPrimary.copy(alpha = 0.1f) else Background)
                .clickable { onWatchedToggle(!episode.isWatched) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Mark Watched",
                tint = if (episode.isWatched) AccentPrimary else TextSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
