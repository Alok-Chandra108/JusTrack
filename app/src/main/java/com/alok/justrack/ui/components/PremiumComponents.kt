package com.alok.justrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alok.justrack.data.model.CastMember
import com.alok.justrack.data.model.MediaItem
import com.alok.justrack.data.model.MovieDetails
import com.alok.justrack.ui.theme.*
import java.util.Locale

@Composable
fun BackdropHeader(
    backdropUrl: String?,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        endY = 300f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Background.copy(alpha = 0.3f),
                            Background
                        ),
                        startY = 150f
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Row {
                IconButton(onClick = onShareClick) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", tint = TextPrimary)
                }
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More", tint = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun PosterInfoRow(movie: MovieDetails) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = movie.posterPath,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 120.dp, height = 175.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                ),
                color = TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (movie.certification.isNotBlank() && movie.certification != "-") {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, TextPrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = movie.certification,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "${movie.releaseDate} · ${movie.runtime}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val directedLabel = if (movie.mediaType == com.alok.justrack.data.model.MediaType.TV) "Created by" else "Directed by"
            RichDirectorText(label = directedLabel, names = movie.director)
        }
    }
}

@Composable
private fun RichDirectorText(label: String, names: List<String>) {
    val joinedNames = names.joinToString(" & ")
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label ",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = joinedNames,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ActionButtons(
    isInWatchlist: Boolean,
    isWatched: Boolean,
    releaseDate: String = "",
    onWatchlistToggle: () -> Unit,
    onWatchedToggle: () -> Unit
) {
    val isReleased = try {
        val today = java.time.LocalDate.now()
        java.time.LocalDate.parse(releaseDate).isBefore(today) || java.time.LocalDate.parse(releaseDate).isEqual(today)
    } catch (_: Exception) { true }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val watchlistColor by animateColorAsState(
            if (isInWatchlist) AccentPrimary else Color.Transparent,
            label = "watchlistColor"
        )
        val watchlistContent by animateColorAsState(
            if (isInWatchlist) Background else TextSecondary,
            label = "watchlistContent"
        )
        val watchedColor by animateColorAsState(
            if (isWatched) AccentSecondary else Color.Transparent,
            label = "watchedColor"
        )
        val watchedContent by animateColorAsState(
            if (isWatched) Background else TextSecondary,
            label = "watchedContent"
        )

        Button(
            onClick = onWatchlistToggle,
            modifier = Modifier
                .weight(1f)
                .height(42.dp),
            shape = RoundedCornerShape(21.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = watchlistColor,
                contentColor = watchlistContent
            ),
            border = if (isInWatchlist) null else BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                if (isInWatchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (isInWatchlist) "In Watchlist" else "Watchlist",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }

        Button(
            onClick = { if (isReleased) onWatchedToggle() },
            enabled = isReleased,
            modifier = Modifier
                .weight(1f)
                .height(42.dp),
            shape = RoundedCornerShape(21.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isReleased) watchedColor else SurfaceColor,
                contentColor = if (isReleased) watchedContent else TextSecondary.copy(alpha = 0.5f),
                disabledContainerColor = SurfaceColor,
                disabledContentColor = TextSecondary.copy(alpha = 0.5f)
            ),
            border = if (isWatched) null else BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                if (isWatched) Icons.Filled.CheckCircle else Icons.Outlined.Visibility,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (!isReleased) "Upcoming" else if (isWatched) "Watched" else "Mark Watched",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun CollapsibleDescription(description: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceVariant)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { isExpanded = !isExpanded }
            .padding(16.dp)
            .animateContentSize(animationSpec = tween(300))
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 20.sp,
                color = TextSecondary
            ),
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis
        )

        if (description.length > 100) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isExpanded) "Show less" else "Read more",
                color = AccentPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun CastSection(cast: List<CastMember>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cast",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextPrimary
            )
            Text(
                text = "See all",
                style = MaterialTheme.typography.bodySmall,
                color = AccentPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 14.dp)
        ) {
            items(cast) { member ->
                CastItem(member)
            }
        }
    }
}

@Composable
fun CastItem(member: CastMember) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        AsyncImage(
            model = member.profilePath,
            contentDescription = member.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(SurfaceColor)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = TextPrimary,
            maxLines = 2,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
        Text(
            text = member.character,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecommendationsSection(recommendations: List<MediaItem>, onRecommendationClick: (MediaItem) -> Unit = {}) {
    Column {
        Text(
            text = "Recommendations",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 10.dp)
        ) {
            items(recommendations) { item ->
                RecommendationItem(item, onClick = { onRecommendationClick(item) })
            }
        }
    }
}

@Composable
fun RecommendationItem(item: MediaItem, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = item.posterPath,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceColor)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(600)) + scaleIn(tween(600, easing = EaseOutBack), initialScale = 0.9f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
        ) {
            Box {
                AsyncImage(
                    model = item.posterPath,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(190.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceColor)
                )
                
                // Rating Badge
                if (item.rating > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", item.rating),
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.releaseDate.split("-").firstOrNull() ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun PosterOnlyCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(600)) + scaleIn(tween(600, easing = EaseOutBack), initialScale = 0.9f),
        modifier = modifier
    ) {
        AsyncImage(
            model = item.posterPath,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .background(SurfaceColor)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = TextPrimary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewAllClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = "View All",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun HorizontalSection(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = TextPrimary,
    emptyMessage: String = "No data yet"
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = title,
            onViewAllClick = onViewAllClick,
            modifier = Modifier.padding(horizontal = 16.dp),
            icon = icon,
            iconTint = iconTint
        )
        
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                state = rememberLazyListState()
            ) {
                items(items.take(7), key = { it.id }) { item ->
                    PosterCard(
                        item = item, 
                        onClick = { onItemClick(item) },
                        modifier = Modifier.width(130.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    userName: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = avatarUrl ?: "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y",
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(2.dp, AccentPrimary, CircleShape)
                .background(SurfaceColor)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
fun DualStatsRow(
    movieCount: Int,
    showCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            label = "Movies Watched",
            value = movieCount.toString(),
            icon = Icons.Outlined.Movie,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Shows Watched",
            value = showCount.toString(),
            icon = Icons.Outlined.Tv,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SurfaceColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun SocialStatsRow(
    following: Int,
    followers: Int,
    comments: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SocialStatItem(label = "following", value = following)
        VerticalDivider(color = TextSecondary.copy(alpha = 0.2f), modifier = Modifier.fillMaxHeight().width(1.dp))
        SocialStatItem(label = "followers", value = followers)
        VerticalDivider(color = TextSecondary.copy(alpha = 0.2f), modifier = Modifier.fillMaxHeight().width(1.dp))
        SocialStatItem(label = "comments", value = comments)
    }
}

@Composable
private fun SocialStatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (value > 0) value.toString() else "...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun PremiumEmptyState(
    title: String,
    subtitle: String,
    buttonLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    illustration: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (illustration != null) {
            illustration()
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = AccentPrimary.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(42.dp),
            shape = RoundedCornerShape(21.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Text(
                text = buttonLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun SkeletonSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(120.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceColor)
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(5) {
                 Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceColor)
                )
            }
        }
    }
}

@Composable
fun MoreOptionsBottomSheet(
    isFavourite: Boolean,
    onDismiss: () -> Unit,
    onFavouriteClick: () -> Unit,
    onChangePosterClick: () -> Unit,
    onChangeBackdropClick: () -> Unit,
    onAddToListClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TextSecondary.copy(alpha = 0.3f))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Options",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Favourite card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onFavouriteClick()
                    onDismiss()
                },
            color = SurfaceColor,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isFavourite) HeartRed.copy(alpha = 0.15f) else SurfaceColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = if (isFavourite) HeartRed else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFavourite) "Remove from Favourite" else "Add to Favourite",
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isFavourite) "Remove this from your favourites" else "Mark as your favourite",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Change Poster card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onChangePosterClick()
                    onDismiss()
                },
            color = SurfaceColor,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Change Poster", color = TextPrimary, fontSize = 15.sp)
                    Text("Choose from available posters", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Change Backdrop card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onChangeBackdropClick()
                    onDismiss()
                },
            color = SurfaceColor,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentSecondary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = AccentSecondary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Change Backdrop", color = TextPrimary, fontSize = 15.sp)
                    Text("Choose from available backdrops", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Add to List card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onAddToListClick()
                    onDismiss()
                },
            color = SurfaceColor,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(WatchlistBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = null, tint = WatchlistBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Add to List", color = TextPrimary, fontSize = 15.sp)
                    Text("Add to a custom list", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ListPickerDialog(
    lists: List<Pair<String, String>>,
    mediaListIds: List<String>,
    onDismiss: () -> Unit,
    onListSelected: (String) -> Unit,
    onCreateList: (String) -> Unit
) {
    var showCreateInput by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = { Text("Add to List", color = TextPrimary) },
        text = {
            Column {
                lists.forEach { (id, name) ->
                    val isInList = id in mediaListIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isInList) {
                                    onListSelected(id)
                                } else {
                                    onListSelected(id)
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isInList) Icons.Filled.CheckCircle else Icons.Outlined.AddCircle,
                            contentDescription = null,
                            tint = if (isInList) AccentPrimary else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, color = TextPrimary, fontSize = 16.sp)
                    }
                }
                if (lists.isEmpty() || showCreateInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("List name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newListName.isNotBlank()) {
                                onCreateList(newListName.trim())
                                newListName = ""
                                showCreateInput = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Text("Create", color = TextPrimary)
                    }
                }
                if (!showCreateInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreateInput = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Create new list", color = AccentPrimary, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun FullScreenImagePicker(
    title: String,
    images: List<String>,
    onDismiss: () -> Unit,
    onImageSelected: (String) -> Unit
) {
    var selectedUrl by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Image grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(images) { url ->
                    val isSelected = url == selectedUrl
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.67f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) AccentPrimary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedUrl = url }
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AccentPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Apply button at bottom
        if (selectedUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Background),
                            startY = 0f,
                            endY = 80f
                        )
                    )
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 24.dp)
            ) {
                Button(
                    onClick = {
                        selectedUrl?.let { onImageSelected(it) }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "Apply",
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
