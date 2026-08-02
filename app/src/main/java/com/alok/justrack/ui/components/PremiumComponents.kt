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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
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
import com.alok.justrack.data.model.RatingSource
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

        Spacer(modifier = Modifier.width(16.dp))

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
                        border = BorderStroke(1.dp, TextSecondary)
                    ) {
                        Text(
                            text = movie.certification,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "${movie.releaseDate}  •  ${movie.runtime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
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
    isWatchlisted: Boolean,
    isWatched: Boolean,
    onWatchlistToggle: () -> Unit,
    onWatchedToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val watchlistBg by animateColorAsState(
            if (isWatchlisted) AccentPrimary else Color.Transparent,
            label = "watchlistBg"
        )
        val watchlistContent by animateColorAsState(
            if (isWatchlisted) Color.White else TextSecondary,
            label = "watchlistContent"
        )
        val watchedBg by animateColorAsState(
            if (isWatched) AccentSecondary else Color.Transparent,
            label = "watchedBg"
        )
        val watchedContent by animateColorAsState(
            if (isWatched) Color.White else TextSecondary,
            label = "watchedContent"
        )

        Button(
            onClick = onWatchlistToggle,
            modifier = Modifier
                .weight(1f)
                .height(42.dp),
            shape = RoundedCornerShape(21.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = watchlistBg,
                contentColor = watchlistContent
            ),
            border = if (isWatchlisted) null else BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                if (isWatchlisted) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (isWatchlisted) "In Watchlist" else "Watchlist",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }

        Button(
            onClick = onWatchedToggle,
            modifier = Modifier
                .weight(1f)
                .height(42.dp),
            shape = RoundedCornerShape(21.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = watchedBg,
                contentColor = watchedContent
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
                if (isWatched) "Watched" else "Mark Watched",
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
            .background(SurfaceColor)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { isExpanded = !isExpanded }
            .padding(12.dp)
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
fun MinimalRatingsRow(ratings: List<RatingSource>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ratings.forEach { rating ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = rating.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rating.value,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
            }
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
fun MovieDetailsBottomNavigation() {
    NavigationBar(
        containerColor = Background,
        tonalElevation = 0.dp,
        modifier = Modifier.height(80.dp)
    ) {
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
            label = { Text("Lists") },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
            label = { Text("Reviews") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextPrimary,
                selectedTextColor = TextPrimary,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Outlined.Link, contentDescription = null) },
            label = { Text("Links") },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            label = { Text("Me") },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
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
                .width(130.dp)
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
    iconTint: Color = TextPrimary
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
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceColor),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { /* RELOAD */ },
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, TextPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("RELOAD", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                state = rememberLazyListState()
            ) {
                items(items.take(10), key = { it.id }) { item ->
                    PosterCard(item = item, onClick = { onItemClick(item) })
                }
            }
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
    icon: ImageVector? = null,
    illustration: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
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
