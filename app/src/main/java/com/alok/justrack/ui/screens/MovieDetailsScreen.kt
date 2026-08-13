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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.alok.justrack.data.model.MovieDetails
import com.alok.justrack.data.model.Season
import com.alok.justrack.data.model.Episode
import com.alok.justrack.data.model.WatchProviders
import com.alok.justrack.data.model.WatchProvider
import com.alok.justrack.ui.components.*
import com.alok.justrack.ui.theme.*
import com.alok.justrack.ui.viewmodel.DetailUiState
import com.alok.justrack.ui.viewmodel.DetailViewModel
import com.alok.justrack.util.ConfettiManager
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartySystem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    navController: NavController,
    id: String,
    mediaType: String = "MOVIE",
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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
    val episodeConfirmation by viewModel.episodeMarkConfirmation.collectAsState()
    val showProgress by viewModel.showProgress.collectAsState()

    var showMoreSheet by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }
    var showPosterPicker by remember { mutableStateOf(false) }
    var showBackdropPicker by remember { mutableStateOf(false) }
    
    var confettiParties by remember { mutableStateOf<List<Party>>(emptyList()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(id, mediaType) {
        viewModel.loadDetail(id, mediaType)
    }
    
    LaunchedEffect(Unit) {
        viewModel.showCompletionEvents.collect { showId ->
            if (showId == id) {
                confettiParties = ConfettiManager.getCelebrationParty()
            }
        }
    }

    when (val state = uiState) {
        is DetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is DetailUiState.Success -> {
            Box(modifier = Modifier.fillMaxSize()) {
                MovieDetailsScreen(
                    movie = state.item,
                    isInWatchlist = isInWatchlist,
                    isWatched = isWatched,
                    showProgress = showProgress,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
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
                    onSeasonWatchedToggle = { season ->
                        viewModel.toggleSeasonWatched(season)
                    },
                    onRecommendationClick = { item ->
                        navController.navigate(com.alok.justrack.ui.navigation.Screen.Detail.createRoute(item.id, item.mediaType.name))
                    },
                    onRecommendationWatchlistToggle = { item ->
                        viewModel.toggleWatchlistForRecommendation(item)
                    },
                    onRecommendationRefresh = {
                        viewModel.refreshRecommendations()
                    },
                    onPersonClick = { person ->
                        navController.navigate(com.alok.justrack.ui.navigation.Screen.Person.createRoute(person.id))
                    }
                )

                if (confettiParties.isNotEmpty()) {
                    KonfettiView(
                        modifier = Modifier.fillMaxSize(),
                        parties = confettiParties,
                        updateListener = object : OnParticleSystemUpdateListener {
                            override fun onParticleSystemEnded(system: PartySystem, activeSystems: Int) {
                                if (activeSystems == 0) {
                                    confettiParties = emptyList()
                                }
                            }
                        }
                    )
                }
            }

            if (showMoreSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreSheet = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.background,
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

            episodeConfirmation?.let { confirmation ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissMarkPreviousConfirmation() },
                    title = { Text("Mark previous as watched?") },
                    text = { 
                        Text("Marking episode ${confirmation.episodeNumber} as watched will also mark all previous unwatched episodes in this season as watched.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.confirmMarkPreviousWatched() }
                        ) {
                            Text("Confirm", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.dismissMarkPreviousConfirmation() }
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        is DetailUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MovieDetailsScreen(
    movie: MovieDetails,
    isInWatchlist: Boolean,
    isWatched: Boolean,
    showProgress: com.alok.justrack.data.model.ShowProgress?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    onWatchlistToggle: () -> Unit,
    onWatchedToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
    onSeasonClick: (Int) -> Unit = {},
    onEpisodeWatchedToggle: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    onSeasonWatchedToggle: (Season) -> Unit = {},
    onRecommendationClick: (MediaItem) -> Unit = {},
    onRecommendationWatchlistToggle: (MediaItem) -> Unit = {},
    onRecommendationRefresh: () -> Unit = {},
    onPersonClick: (com.alok.justrack.data.model.Person) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ABOUT", "EPISODES")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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

            if (movie.mediaType == MediaType.TV && showProgress != null) {
                EpisodeProgressBar(
                    progress = showProgress,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 0.dp)
                )
            }

            if (movie.mediaType == MediaType.TV) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    divider = {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
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
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
            ) {
                if (movie.mediaType == MediaType.TV) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    when (selectedTab) {
                        0 -> TvShowAboutSection(
                            movie = movie,
                            isInWatchlist = isInWatchlist,
                            isWatched = isWatched,
                            showProgress = showProgress,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onWatchlistToggle = onWatchlistToggle,
                            onWatchedToggle = onWatchedToggle,
                            onRecommendationClick = onRecommendationClick,
                            onRecommendationWatchlistToggle = onRecommendationWatchlistToggle,
                            onRecommendationRefresh = onRecommendationRefresh,
                            onPersonClick = onPersonClick
                        )
                        1 -> TvShowEpisodesSection(
                            seasons = movie.seasons,
                            onSeasonClick = onSeasonClick,
                            onEpisodeWatchedToggle = onEpisodeWatchedToggle,
                            onSeasonWatchedToggle = onSeasonWatchedToggle
                        )
                    }
                } else {
                    // Movie Layout
                    Spacer(modifier = Modifier.height(12.dp))
                    PosterInfoRow(
                        movie = movie, 
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onPersonClick = onPersonClick
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ActionButtons(
                        isInWatchlist = isInWatchlist,
                        isWatched = isWatched,
                        releaseDate = movie.rawReleaseDate.ifEmpty { movie.releaseDate },
                        isStopped = false,
                        onWatchlistToggle = onWatchlistToggle,
                        onWatchedToggle = onWatchedToggle
                    )

                    movie.watchProviders?.let { providers ->
                        Spacer(modifier = Modifier.height(16.dp))
                        WatchProvidersSection(providers = providers)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    CollapsibleDescription(description = movie.overview)
                    Spacer(modifier = Modifier.height(20.dp))
                    CastSection(cast = movie.cast, onCastClick = { castMember -> 
                        onPersonClick(com.alok.justrack.data.model.Person(castMember.id, castMember.name)) 
                    })
                    Spacer(modifier = Modifier.height(20.dp))
                    RecommendationsSection(
                        recommendations = movie.recommendations, 
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onRecommendationClick = onRecommendationClick,
                        onWatchlistToggle = onRecommendationWatchlistToggle,
                        onRefreshClick = onRecommendationRefresh
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TvShowAboutSection(
    movie: MovieDetails,
    isInWatchlist: Boolean,
    isWatched: Boolean,
    showProgress: com.alok.justrack.data.model.ShowProgress?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onWatchlistToggle: () -> Unit,
    onWatchedToggle: () -> Unit,
    onRecommendationClick: (MediaItem) -> Unit,
    onRecommendationWatchlistToggle: (MediaItem) -> Unit,
    onRecommendationRefresh: () -> Unit,
    onPersonClick: (com.alok.justrack.data.model.Person) -> Unit
) {
    Column {
        PosterInfoRow(
            movie = movie, 
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onPersonClick = onPersonClick
        )
        Spacer(modifier = Modifier.height(16.dp))
        ActionButtons(
            isInWatchlist = isInWatchlist,
            isWatched = isWatched,
            releaseDate = movie.rawReleaseDate.ifEmpty { movie.releaseDate },
            isStopped = !isInWatchlist && (showProgress?.watched ?: 0) > 0 && (showProgress?.percentage ?: 0) < 100,
            onWatchlistToggle = onWatchlistToggle,
            onWatchedToggle = onWatchedToggle
        )
        
        movie.watchProviders?.let { providers ->
            Spacer(modifier = Modifier.height(16.dp))
            WatchProvidersSection(providers = providers)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        CollapsibleDescription(description = movie.overview)
        Spacer(modifier = Modifier.height(20.dp))
        CastSection(cast = movie.cast, onCastClick = { castMember -> 
            onPersonClick(com.alok.justrack.data.model.Person(castMember.id, castMember.name)) 
        })
        Spacer(modifier = Modifier.height(20.dp))
        RecommendationsSection(
            recommendations = movie.recommendations, 
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onRecommendationClick = onRecommendationClick,
            onWatchlistToggle = onRecommendationWatchlistToggle,
            onRefreshClick = onRecommendationRefresh
        )
    }
}

@Composable
fun TvShowEpisodesSection(
    seasons: List<Season>,
    onSeasonClick: (Int) -> Unit,
    onEpisodeWatchedToggle: (Int, Int, Boolean) -> Unit,
    onSeasonWatchedToggle: (Season) -> Unit
) {
    var expandedSeason by remember { mutableIntStateOf(-1) }

    Column {
        Text(
            text = "All episodes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    onEpisodeWatchedToggle = onEpisodeWatchedToggle,
                    onSeasonWatchedToggle = onSeasonWatchedToggle
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
    onEpisodeWatchedToggle: (Int, Int, Boolean) -> Unit,
    onSeasonWatchedToggle: (Season) -> Unit
) {
    val isCompleted = season.episodeCount > 0 && season.watchedCount == season.episodeCount
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicShadow(cornerRadius = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = rotationState }
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${season.watchedCount}/${season.episodeCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                
                // Clickable Solid Checkmark
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) WatchedGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                        .clickable { onSeasonWatchedToggle(season) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = if (isCompleted) "Completed" else "Mark all watched",
                        tint = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
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
        // Thumbnail - NO CLICKABLE
        AsyncImage(
            model = episode.stillPath,
            contentDescription = episode.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(100.dp, 60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Text Info - NO CLICKABLE
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "S${episode.seasonNumber.toString().padStart(2, '0')} | E${episode.episodeNumber.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = episode.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        val daysUntil = remember(episode.airDate) {
            com.alok.justrack.util.DateUtils.getDaysUntil(episode.airDate)
        }
        
        if (daysUntil != null && daysUntil > 0) {
            // Countdown Timer for Upcoming Episodes
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                Text(
                    text = daysUntil.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (daysUntil == 1L) "DAY" else "DAYS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Circular Solid Check Button - ONLY CLICKABLE PART
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (episode.isWatched) WatchedGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    .clickable { onWatchedToggle(!episode.isWatched) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Mark Watched",
                    tint = if (episode.isWatched) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun WatchProvidersSection(
    providers: WatchProviders,
    modifier: Modifier = Modifier
) {
    if (providers.stream.isEmpty() && providers.rent.isEmpty() && providers.buy.isEmpty()) {
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "Where to Watch",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (providers.stream.isNotEmpty()) {
            ProviderRow(providers = providers.stream)
        } else if (providers.rent.isNotEmpty()) {
            ProviderRow(providers = providers.rent)
        } else if (providers.buy.isNotEmpty()) {
            ProviderRow(providers = providers.buy)
        }

        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Powered by JustWatch",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}

@Composable
fun ProviderRow(providers: List<WatchProvider>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        providers.take(6).forEach { provider ->
            AsyncImage(
                model = provider.logoUrl,
                contentDescription = provider.name,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun EpisodeProgressBar(
    progress: com.alok.justrack.data.model.ShowProgress,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.percentage / 100f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.EaseOutExpo),
        label = "progress"
    )

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = progress.label,
                style = MaterialTheme.typography.labelSmall,
                color = progress.color,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))

        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(progress.color)
            )
        }
    }
}

