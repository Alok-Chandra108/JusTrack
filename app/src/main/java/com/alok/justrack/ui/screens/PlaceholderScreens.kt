package com.alok.justrack.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.ui.components.*
import com.alok.justrack.ui.navigation.Screen
import com.alok.justrack.ui.theme.Background
import com.alok.justrack.ui.theme.TextPrimary
import com.alok.justrack.ui.theme.TextSecondary
import com.alok.justrack.ui.viewmodel.*

// ─────────────────────────────────────────────
// WATCHLIST SCREEN
// ─────────────────────────────────────────────
@Composable
fun WatchlistScreen(
    navController: NavController,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "My Watchlist",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is WatchlistUiState.Loading -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(5) { SkeletonCard() }
                }
            }
            is WatchlistUiState.Success -> {
                if (state.items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistAdd,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Your watchlist is empty", color = TextSecondary, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Search for movies & shows to add!", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(state.items, key = { it.id }) { item ->
                            MediaCard(
                                item = item,
                                onClick = { navController.navigate(Screen.Detail.createRoute(item.id)) }
                            )
                        }
                    }
                }
            }
            is WatchlistUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// LISTS SCREEN
// ─────────────────────────────────────────────
@Composable
fun ListsScreen(
    navController: NavController,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Movies", "TV Shows")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Lists", color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Tab Row
        NeuCard(modifier = Modifier.fillMaxWidth().height(48.dp), cornerRadius = 24.dp) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, label ->
                    val selected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedTab = index }
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is WatchlistUiState.Loading -> {
                LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(6) { NeuCard(modifier = Modifier.height(100.dp)) { SkeletonBox(modifier = Modifier.fillMaxSize()) } }
                }
            }
            is WatchlistUiState.Success -> {
                val filtered = when (selectedTab) {
                    1 -> state.items.filter { it.mediaType == MediaType.MOVIE }
                    2 -> state.items.filter { it.mediaType == MediaType.TV }
                    else -> state.items
                }
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nothing here yet", color = TextSecondary)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered.size) { i ->
                            val item = filtered[i]
                            NeuCard(
                                modifier = Modifier
                                    .height(160.dp)
                                    .clickable { navController.navigate(Screen.Detail.createRoute(item.id)) },
                                cornerRadius = 12.dp
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!item.posterPath.isNullOrBlank()) {
                                        AsyncImage(
                                            model = item.posterPath,
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                                    ) {
                                        Text(item.title, color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                                        Text("★ ${item.rating}", color = androidx.compose.ui.graphics.Color.Yellow, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is WatchlistUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// SEARCH SCREEN
// ─────────────────────────────────────────────
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        NeuCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search Icon", tint = TextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search movies & TV shows...", color = TextSecondary)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear search", tint = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (val state = uiState) {
            is SearchUiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Rounded.Movie, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Discover movies & shows", color = TextSecondary, fontSize = 15.sp)
                    }
                }
            }
            is SearchUiState.Loading -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(5) { SkeletonCard() }
                }
            }
            is SearchUiState.Success -> {
                if (state.items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No results for \"$query\"", color = TextSecondary)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(state.items) { item ->
                            MediaCard(item = item, onClick = { navController.navigate(Screen.Detail.createRoute(item.id)) })
                        }
                    }
                }
            }
            is SearchUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// STATS SCREEN
// ─────────────────────────────────────────────
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Stats", color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        if (stats == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Rounded.BarChart, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Add items to your watchlist to see stats!", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            val s = stats!!
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(4) { index ->
                    NeuCard(modifier = Modifier.height(120.dp)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when (index) {
                                0 -> {
                                    Icon(Icons.Rounded.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("${s.totalItems}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("Total Items", color = TextSecondary, fontSize = 12.sp)
                                }
                                1 -> {
                                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("${s.movieCount}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("Movies", color = TextSecondary, fontSize = 12.sp)
                                }
                                2 -> {
                                    Icon(Icons.Rounded.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("${s.tvCount}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("TV Shows", color = TextSecondary, fontSize = 12.sp)
                                }
                                3 -> {
                                    Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("${s.averageRating}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("Avg Rating", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Top Rated", color = TextSecondary, fontSize = 12.sp)
                        Text(s.topRatedTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// PROFILE SCREEN
// ─────────────────────────────────────────────
@Composable
fun ProfileScreen(viewModel: WatchlistViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val count = (uiState as? WatchlistUiState.Success)?.items?.size ?: 0
    val movieCount = (uiState as? WatchlistUiState.Success)?.items?.count { it.mediaType == MediaType.MOVIE } ?: 0
    val tvCount = (uiState as? WatchlistUiState.Success)?.items?.count { it.mediaType == MediaType.TV } ?: 0

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        NeuCard(modifier = Modifier.size(100.dp), cornerRadius = 50.dp) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(56.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("JusTrack User", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Movie & TV enthusiast", color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // Quick summary row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("$count" to "Watchlist", "$movieCount" to "Movies", "$tvCount" to "TV Shows").forEach { (value, label) ->
                NeuCard(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(label, color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        listOf(
            Icons.Rounded.Notifications to "Notifications",
            Icons.Rounded.Palette to "Appearance",
            Icons.Rounded.Info to "About JusTrack"
        ).forEach { (icon, label) ->
            NeuCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(label, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// DETAIL SCREEN
// ─────────────────────────────────────────────
@Composable
fun DetailScreen(
    id: String?,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isWatchlisted by viewModel.isWatchlisted.collectAsState()

    LaunchedEffect(id) {
        id?.let { viewModel.loadDetail(it) }
    }

    when (val state = uiState) {
        is DetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DetailUiState.Success -> {
            val item = state.item
            val heroImage = item.backdropPath ?: item.posterPath
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    // Hero Image
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                        if (!heroImage.isNullOrBlank()) {
                            AsyncImage(
                                model = heroImage,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(modifier = Modifier.fillMaxSize().background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(androidx.compose.ui.graphics.Color.Transparent, Background)
                                )
                            ))
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(TextSecondary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) { Text(item.title, color = TextSecondary) }
                        }
                    }
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (item.mediaType == MediaType.MOVIE) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    item.mediaType.name,
                                    color = if (item.mediaType == MediaType.MOVIE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("★ ${item.rating}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(item.releaseDate.take(4), color = TextSecondary, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.title, color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Watchlist Toggle Button
                        val btnColor by animateColorAsState(
                            targetValue = if (isWatchlisted) MaterialTheme.colorScheme.primary else Background,
                            animationSpec = tween(300)
                        )
                        NeuCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clickable { viewModel.toggleWatchlist(item) },
                            cornerRadius = 14.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(btnColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isWatchlisted) Icons.Rounded.Check else Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = if (isWatchlisted) androidx.compose.ui.graphics.Color.White else TextPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isWatchlisted) "In Watchlist" else "Add to Watchlist",
                                        color = if (isWatchlisted) androidx.compose.ui.graphics.Color.White else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Overview", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.overview.ifBlank { "No overview available." }, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Cast", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(5) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    SkeletonBox(modifier = Modifier.size(60.dp), cornerRadius = 30)
                                    Spacer(Modifier.height(4.dp))
                                    SkeletonBox(modifier = Modifier.width(50.dp).height(10.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
        is DetailUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
