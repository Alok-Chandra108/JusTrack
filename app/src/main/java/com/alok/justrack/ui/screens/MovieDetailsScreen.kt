package com.alok.justrack.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.alok.justrack.data.model.CastMember
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.model.MovieDetails
import com.alok.justrack.data.model.RatingSource
import com.alok.justrack.ui.components.*
import com.alok.justrack.ui.theme.*
import com.alok.justrack.ui.viewmodel.DetailUiState
import com.alok.justrack.ui.viewmodel.DetailViewModel

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

    var showMoreMenu by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }
    var showPosterPicker by remember { mutableStateOf(false) }
    var showBackdropPicker by remember { mutableStateOf(false) }

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
            Box(modifier = Modifier.fillMaxSize()) {
                MovieDetailsScreen(
                    movie = state.item,
                    isWatchlisted = isWatchlisted,
                    isWatched = isWatched,
                    onBackClick = { navController.popBackStack() },
                    onWatchlistToggle = { viewModel.toggleWatchlist(state.item) },
                    onWatchedToggle = { viewModel.toggleWatched(state.item.id) },
                    onMoreClick = { showMoreMenu = true },
                    onRecommendationClick = { item ->
                        navController.navigate(com.alok.justrack.ui.navigation.Screen.Detail.createRoute(item.id, item.mediaType.name))
                    }
                )

                if (showMoreMenu) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showMoreMenu = false },
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Box(modifier = Modifier.padding(top = 50.dp, end = 8.dp)) {
                            MoreOptionsDropdown(
                                isFavourite = isFavourite,
                                onDismiss = { showMoreMenu = false },
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
                ImagePickerDialog(
                    title = "Change Poster",
                    images = posterImages,
                    onDismiss = { showPosterPicker = false },
                    onImageSelected = { url -> viewModel.changePoster(url) }
                )
            }

            if (showBackdropPicker && backdropImages.isNotEmpty()) {
                ImagePickerDialog(
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
    onMoreClick: () -> Unit = {},
    onRecommendationClick: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier
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

@Preview(showBackground = true, backgroundColor = 0xFF111315)
@Composable
fun MovieDetailsPreview() {
    val sampleMovie = MovieDetails(
        id = "1",
        title = "Avatar Aang: The Last Airb...",
        overview = "Avatar Aang, the world's last Airbender, learns of an ancient power that could save his culture from extinction. With the help of his friends, he embarks on a global quest to find it before it falls into the wrong hands and threatens to upend the peace they sacrificed everything to achieve.",
        posterPath = null,
        backdropPath = null,
        rating = 9.4,
        releaseDate = "25 Jul 2026",
        runtime = "1h 39m",
        certification = "PG",
        director = listOf("Lauren Montgomery"),
        cast = listOf(
            CastMember("1", "Aang", "Avatar", null),
            CastMember("2", "Katara", "Waterbender", null),
            CastMember("3", "Sokka", "Warrior", null),
            CastMember("4", "Toph", "Earthbender", null)
        ),
        ratings = listOf(
            RatingSource("My rating", "-"),
            RatingSource("TMDb", "9.4"),
            RatingSource("Trakt", "8.6"),
            RatingSource("IMDb", "-"),
            RatingSource("Rotten T...", "-")
        ),
        recommendations = listOf(
            MediaItem("2", "The Legend of Korra", "Overview", null, null, 8.5, "2012-04-14", MediaType.TV),
            MediaItem("3", "The Dragon Prince", "Overview", null, null, 8.4, "2018-09-14", MediaType.TV)
        )
    )

    JusTrackTheme {
        MovieDetailsScreen(
            movie = sampleMovie,
            isWatchlisted = false,
            isWatched = false,
            onBackClick = {},
            onWatchlistToggle = {},
            onWatchedToggle = {},
            onRecommendationClick = {}
        )
    }
}
