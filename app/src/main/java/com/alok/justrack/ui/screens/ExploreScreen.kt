package com.alok.justrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
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
import com.alok.justrack.ui.components.*
import com.alok.justrack.ui.navigation.Screen
import com.alok.justrack.ui.theme.*
import com.alok.justrack.ui.viewmodel.ExploreSearchUiState
import com.alok.justrack.ui.viewmodel.ExploreUiState
import com.alok.justrack.ui.viewmodel.ExploreViewModel
import com.alok.justrack.ui.viewmodel.Genre
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showLongPressSheet by remember { mutableStateOf(false) }
    var longPressItem by remember { mutableStateOf<MediaItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            placeholder = { Text("Search movies, shows...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(26.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )

        when (val state = uiState) {
            is ExploreUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ExploreUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is ExploreUiState.Success -> {
                // Search results appear above sections
                when (val search = searchState) {
                    is ExploreSearchUiState.Searching -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        }
                    }
                    is ExploreSearchUiState.Results -> {
                        if (search.items.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(search.items, key = { it.id }) { item ->
                                    ExploreMediaCard(
                                        item = item,
                                        onClick = { navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name)) },
                                        onLongPress = { longPressItem = it; showLongPressSheet = true }
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // Default explore content
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            // Featured Banner
                            if (state.bannerItems.isNotEmpty()) {
                                item { FeaturedBanner(items = state.bannerItems, navController = navController) }
                            }

                            // Genre Chips
                            if (state.genres.isNotEmpty()) {
                                item {
                                    GenreChips(
                                        genres = state.genres,
                                        selectedGenre = state.selectedGenre,
                                        onGenreSelected = { viewModel.selectGenre(it) },
                                        onGenreCleared = { viewModel.clearGenreSelection() }
                                    )
                                }
                            }

                            // Genre Results
                            if (state.selectedGenre != null && state.genreResults.isNotEmpty()) {
                                item {
                                    ExploreSection(
                                        title = "${state.selectedGenre.name} Picks",
                                        items = state.genreResults,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id, it.mediaType.name)) },
                                        onItemLongPress = { longPressItem = it; showLongPressSheet = true }
                                    )
                                }
                            }

                            // Trending
                            if (state.trending.isNotEmpty()) {
                                item {
                                    ExploreSection(
                                        title = "Trending Now",
                                        items = state.trending,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id, it.mediaType.name)) },
                                        onItemLongPress = { longPressItem = it; showLongPressSheet = true },
                                        onLoadMore = { }
                                    )
                                }
                            }

                            // Popular Movies
                            item {
                                ExploreSectionLazy(
                                    title = "Popular Movies",
                                    items = state.popularMovies,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id, it.mediaType.name)) },
                                    onItemLongPress = { longPressItem = it; showLongPressSheet = true },
                                    onLoadMore = { viewModel.loadSection("popular_movies") }
                                )
                            }

                            // Popular TV
                            item {
                                ExploreSectionLazy(
                                    title = "Popular TV Shows",
                                    items = state.popularTv,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id, it.mediaType.name)) },
                                    onItemLongPress = { longPressItem = it; showLongPressSheet = true },
                                    onLoadMore = { viewModel.loadSection("popular_tv") }
                                )
                            }

                            // Top Rated Movies
                            item {
                                ExploreSectionLazy(
                                    title = "Top Rated Movies",
                                    items = state.topRatedMovies,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id, it.mediaType.name)) },
                                    onItemLongPress = { longPressItem = it; showLongPressSheet = true },
                                    onLoadMore = { viewModel.loadSection("top_rated_movies") }
                                )
                            }

                            // Top Rated TV
                            item {
                                ExploreSectionLazy(
                                    title = "Top Rated TV Shows",
                                    items = state.topRatedTv,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id, it.mediaType.name)) },
                                    onItemLongPress = { longPressItem = it; showLongPressSheet = true },
                                    onLoadMore = { viewModel.loadSection("top_rated_tv") }
                                )
                            }

                            // Upcoming Movies
                            item {
                                ExploreSectionLazy(
                                    title = "Upcoming Movies",
                                    items = state.upcomingMovies,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id, it.mediaType.name)) },
                                    onItemLongPress = { longPressItem = it; showLongPressSheet = true },
                                    onLoadMore = { viewModel.loadSection("upcoming_movies") },
                                    showDate = true
                                )
                            }

                            // On The Air TV
                            item {
                                ExploreSectionLazy(
                                    title = "Currently Airing TV",
                                    items = state.onTheAirTv,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onItemClick = { navController.navigate(Screen.Detail.createRoute(it.id, it.mediaType.name)) },
                                    onItemLongPress = { longPressItem = it; showLongPressSheet = true },
                                    onLoadMore = { viewModel.loadSection("on_the_air_tv") }
                                )
                            }
                        }
                    }
                }

            }
        }
    }

    // Long Press Bottom Sheet
    if (showLongPressSheet && longPressItem != null) {
        ExploreLongPressSheet(
            item = longPressItem!!,
            viewModel = viewModel,
            onDismiss = { showLongPressSheet = false },
            onNavigateToDetail = { navController.navigate(Screen.Detail.createRoute(it.id, it.mediaType.name)) }
        )
    }
}

@Composable
private fun FeaturedBanner(
    items: List<MediaItem>,
    navController: NavController
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % items.size
            scope.launch { pagerState.animateScrollToPage(nextPage) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { navController.navigate(Screen.Detail.createRoute(item.id, item.mediaType.name)) }
            ) {
                AsyncImage(
                    model = item.backdropPath ?: item.posterPath,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 100f
                            )
                        )
                )
                // Title and rating
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", item.rating),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.mediaType.name,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        // Page indicator dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(items.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
private fun GenreChips(
    genres: List<Genre>,
    selectedGenre: Genre?,
    onGenreSelected: (Genre) -> Unit,
    onGenreCleared: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(genres) { genre ->
            val isSelected = selectedGenre?.id == genre.id
            Surface(
                onClick = {
                    if (isSelected) onGenreCleared() else onGenreSelected(genre)
                },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Text(
                    text = genre.name,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExploreSection(
    title: String,
    items: List<MediaItem>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onItemClick: (MediaItem) -> Unit,
    onItemLongPress: (MediaItem) -> Unit,
    onLoadMore: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                ExplorePosterCard(
                    item = item,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = { onItemClick(item) },
                    onLongPress = { onItemLongPress(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExploreSectionLazy(
    title: String,
    items: List<MediaItem>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onItemClick: (MediaItem) -> Unit,
    onItemLongPress: (MediaItem) -> Unit,
    onLoadMore: () -> Unit,
    showDate: Boolean = false
) {
    LaunchedEffect(Unit) { onLoadMore() }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (items.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    SkeletonBox(
                        modifier = Modifier.width(120.dp).height(180.dp),
                        cornerRadius = 12
                    )
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ExplorePosterCard(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { onItemClick(item) },
                        onLongPress = { onItemLongPress(item) },
                        showDate = showDate
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun ExplorePosterCard(
    item: MediaItem,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    showDate: Boolean = false
) {
    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { onLongPress() }
                )
        ) {
            AsyncImage(
                model = item.posterPath,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .sharedElement(
                        rememberSharedContentState(key = "poster-${item.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    .height(180.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            // Date badge (bottom)
            if (showDate && item.releaseDate.isNotBlank()) {
                val formattedDate = remember(item.releaseDate) {
                    val date = com.alok.justrack.util.DateUtils.parseDate(item.releaseDate)
                    date?.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.US)) ?: ""
                }
                
                if (formattedDate.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = formattedDate.uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Rating badge
            if (item.rating > 0 && !showDate) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", item.rating),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExploreMediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    onLongPress: (MediaItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongPress(item) }
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.posterPath,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(60.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f", item.rating),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.mediaType.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreLongPressSheet(
    item: MediaItem,
    viewModel: ExploreViewModel,
    onDismiss: () -> Unit,
    onNavigateToDetail: (MediaItem) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isInWatchlist by remember { mutableStateOf(false) }
    var isFavourite by remember { mutableStateOf(false) }
    val lists by viewModel.getLists().collectAsState(initial = emptyList())
    var showCreateList by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    LaunchedEffect(item) {
        isInWatchlist = viewModel.isInWatchlist(item.id)
        isFavourite = viewModel.isFavourite(item.id, item.mediaType)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Item preview
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = item.posterPath,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(50.dp)
                        .height(75.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.mediaType.name,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Add to Watchlist
            BottomSheetOption(
                icon = if (isInWatchlist) Icons.Rounded.BookmarkRemove else Icons.Rounded.BookmarkAdd,
                label = if (isInWatchlist) "Remove from Watchlist" else "Add to Watchlist",
                iconTint = if (isInWatchlist) Color.Red else MaterialTheme.colorScheme.primary,
                onClick = {
                    scope.launch {
                        if (isInWatchlist) viewModel.removeFromWatchlist(item.id) else viewModel.addToWatchlist(item)
                        isInWatchlist = !isInWatchlist
                    }
                }
            )

            // Mark as Watched
            val isItemWatched = remember { mutableStateOf(false) }
            LaunchedEffect(item) {
                isItemWatched.value = viewModel.isWatched(item.id)
            }
            
            BottomSheetOption(
                icon = if (isItemWatched.value) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                label = if (isItemWatched.value) "Watched" else "Mark as Watched",
                iconTint = MaterialTheme.colorScheme.secondary,
                onClick = {
                    scope.launch {
                        viewModel.toggleWatched(item)
                        isItemWatched.value = !isItemWatched.value
                        if (isItemWatched.value) isInWatchlist = false
                    }
                    if (isItemWatched.value) onDismiss()
                }
            )

            // Toggle Favourite
            BottomSheetOption(
                icon = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = if (isFavourite) "Remove from Favourites" else "Add to Favourites",
                iconTint = if (isFavourite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = {
                    scope.launch {
                        viewModel.toggleFavourite(item)
                        isFavourite = !isFavourite
                    }
                }
            )

            // Add to List
            BottomSheetOption(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                label = "Add to List",
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = { showCreateList = true }
            )

            // View Details
            BottomSheetOption(
                icon = Icons.Rounded.Info,
                label = "View Details",
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = {
                    onDismiss()
                    onNavigateToDetail(item)
                }
            )
        }
    }

    // Create List Dialog
    if (showCreateList) {
        AlertDialog(
            onDismissRequest = { showCreateList = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Add to List", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    lists.forEach { (listId, listName) ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    viewModel.addToList(listId, item)
                                    showCreateList = false
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(listName, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("Create new list") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newListName.isNotBlank()) {
                            scope.launch {
                                viewModel.createList(newListName)
                                newListName = ""
                            }
                        }
                    }
                ) {
                    Text("Create", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateList = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun BottomSheetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
