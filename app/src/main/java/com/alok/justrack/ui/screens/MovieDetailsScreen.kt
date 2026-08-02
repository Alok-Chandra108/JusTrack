package com.alok.justrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MovieDetails
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
    val isWatchlisted by viewModel.isWatchlisted.collectAsState()
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
                isWatchlisted = isWatchlisted,
                isWatched = isWatched,
                onBackClick = { navController.popBackStack() },
                onWatchlistToggle = { viewModel.toggleWatchlist(state.item) },
                onWatchedToggle = { viewModel.toggleWatched(state.item.id) },
                onMoreClick = { showMoreSheet = true },
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
    isWatchlisted: Boolean,
    isWatched: Boolean,
    onBackClick: () -> Unit,
    onWatchlistToggle: () -> Unit,
    onWatchedToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
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
                    isWatchlisted = isWatchlisted,
                    isWatched = isWatched,
                    onWatchlistToggle = onWatchlistToggle,
                    onWatchedToggle = onWatchedToggle
                )
                Spacer(modifier = Modifier.height(16.dp))
                CollapsibleDescription(description = movie.overview)
                Spacer(modifier = Modifier.height(20.dp))
                CastSection(cast = movie.cast)
                Spacer(modifier = Modifier.height(20.dp))
                RecommendationsSection(recommendations = movie.recommendations, onRecommendationClick = onRecommendationClick)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
