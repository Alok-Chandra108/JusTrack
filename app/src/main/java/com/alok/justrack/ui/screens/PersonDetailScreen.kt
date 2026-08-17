package com.alok.justrack.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.alok.justrack.data.model.PersonDetails
import com.alok.justrack.ui.components.SkeletonBox
import com.alok.justrack.ui.navigation.Screen
import com.alok.justrack.ui.theme.*
import com.alok.justrack.ui.viewmodel.PersonUiState
import com.alok.justrack.ui.viewmodel.PersonViewModel

@Composable
fun PersonDetailScreen(
    navController: NavController,
    id: String,
    viewModel: PersonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(id) {
        viewModel.loadPersonDetails(id)
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (uiState is PersonUiState.Success) {
                    Text(
                        text = (uiState as PersonUiState.Success).person.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is PersonUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPrimary)
                }
            }
            is PersonUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = HeartRed)
                }
            }
            is PersonUiState.Success -> {
                PersonContent(
                    person = state.person,
                    navController = navController,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun PersonContent(
    person: PersonDetails,
    navController: NavController,
    viewModel: PersonViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        // Profile Header
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = person.profilePath,
                contentDescription = person.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(SurfaceColor)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = person.knownForDepartment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (!person.birthday.isNullOrEmpty()) {
                    Text(
                        text = "Born: ${person.birthday}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (!person.placeOfBirth.isNullOrEmpty()) {
                    Text(
                        text = person.placeOfBirth,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Biography
        if (person.biography.isNotEmpty()) {
            var isExpanded by remember { mutableStateOf(false) }
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Biography",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = person.biography,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
                if (person.biography.length > 200) {
                    Text(
                        text = if (isExpanded) "Read less" else "Read more",
                        color = AccentPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp).clickable { isExpanded = !isExpanded }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filmography - Movies
        if (person.movieCredits.isNotEmpty()) {
            val sortedMovies = remember(person.movieCredits) {
                person.movieCredits
                    .distinctBy { it.id + it.mediaType.name }
                    .sortedByDescending { it.releaseDate }
            }
            FilmographySection(
                title = "Movies",
                items = sortedMovies,
                viewModel = viewModel,
                onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id, it.mediaType.name)) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Filmography - TV Shows
        if (person.tvCredits.isNotEmpty()) {
            val sortedTv = remember(person.tvCredits) {
                person.tvCredits
                    .distinctBy { it.id + it.mediaType.name }
                    .sortedByDescending { it.releaseDate }
            }
            FilmographySection(
                title = "TV Shows",
                items = sortedTv,
                viewModel = viewModel,
                onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id, it.mediaType.name)) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FilmographySection(
    title: String,
    items: List<MediaItem>,
    viewModel: PersonViewModel,
    onItemClick: (MediaItem) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        // 3 columns wide, 2 rows high, horizontal scroll
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier
                .height(420.dp) // Increased height slightly to accommodate buttons
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(items, key = { "${it.id}-${it.mediaType.name}" }) { item ->
                PersonFilmographyCard(
                    item = item,
                    onWatchlistClick = {
                        if (item.inWatchlist) viewModel.removeFromWatchlist(item.id)
                        else viewModel.addToWatchlist(item)
                    },
                    onWatchedClick = {
                        viewModel.toggleWatched(item)
                    },
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun PersonFilmographyCard(
    item: MediaItem,
    onWatchlistClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
    ) {
        Box {
            AsyncImage(
                model = item.posterPath,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(160.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceColor)
            )

            // Quick Actions
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Watchlist Button
                Surface(
                    onClick = onWatchlistClick,
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = if (item.inWatchlist) AccentPrimary else Color.Black.copy(alpha = 0.6f),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (item.inWatchlist) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd,
                            contentDescription = if (item.inWatchlist) "In Watchlist" else "Add to Watchlist",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Watched Button
                Surface(
                    onClick = onWatchedClick,
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = if (item.isWatched) WatchedGreen else Color.Black.copy(alpha = 0.6f),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (item.isWatched) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = if (item.isWatched) "Mark Unwatched" else "Mark Watched",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}
