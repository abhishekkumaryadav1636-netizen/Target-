package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ThumbnailResolver
import com.example.model.Course
import com.example.ui.theme.CrimsonFavorite
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.SaffronAccent

/**
 * Displays a 1:1.2 portrait course thumbnail with rounded corners, Coil caching,
 * guaranteed course-specific fallback poster, Price/FREE badges, and Favourite Heart icon.
 */
@Composable
fun CourseThumbnailBox(
    course: Course,
    isFavorite: Boolean,
    onToggleFavorite: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showOverlayMetaOnCustomImage: Boolean = true
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { ThumbnailResolver.getImageLoader(context) }
    val fallbackRes = remember(course.id, course.fallbackKey, course.board, course.className, course.title) {
        ThumbnailResolver.fallbackDrawableFor(course)
    }
    val model = remember(course.thumbnailUrl, fallbackRes) {
        ThumbnailResolver.resolveModel(course)
    }
    val isCustomImage = remember(course.thumbnailUrl) {
        ThumbnailResolver.isCustomExternalThumbnail(course)
    }

    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "heartScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / 1.2f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = "${course.title} ${course.board} ${course.batchYear} poster thumbnail",
            placeholder = painterResource(id = fallbackRes),
            error = painterResource(id = fallbackRes),
            fallback = painterResource(id = fallbackRes),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // When an admin uploads a custom photo/URL, ensure course name, class, board & batch year
        // remain prominently displayed in a clean coaching poster footer over the bottom edge.
        if (isCustomImage && showOverlayMetaOnCustomImage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xCC0A1026),
                                Color(0xF20A1026)
                            )
                        )
                    )
                    .padding(start = 10.dp, end = 10.dp, top = 28.dp, bottom = 44.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PosterMiniBadge(text = course.board.uppercase(), color = SaffronAccent)
                        PosterMiniBadge(text = "BATCH ${course.batchYear}", color = Color(0xFF2563EB))
                    }
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${course.className} • ${course.subjectsSummary}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Top-right Favourite Heart Icon (48dp touch target)
        if (onToggleFavorite != null) {
            Surface(
                shape = CircleShape,
                color = Color(0x990F172A),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(48.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("favorite_button_${course.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove ${course.title} from favourites" else "Add ${course.title} to favourites",
                        tint = if (isFavorite) CrimsonFavorite else Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .scale(heartScale)
                    )
                }
            }
        }

        // Bottom Price / FREE badge strip over thumbnail
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (course.freeBatch || course.priceInr == 0L) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldFree,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "FREE",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (course.originalPriceInr > 0L) {
                            Text(
                                text = "₹${course.originalPriceInr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD1FAE5),
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SaffronAccent,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "₹${course.priceInr}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (course.originalPriceInr > course.priceInr) {
                            Text(
                                text = "₹${course.originalPriceInr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFEDD5),
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                }
            }

            if (course.discountPercent > 0 && !course.freeBatch && course.priceInr > 0L) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC0F172A)
                ) {
                    Text(
                        text = "${course.discountPercent}% OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE047),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PosterMiniBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CourseCatalogCard(
    course: Course,
    isFavorite: Boolean,
    isEnrolled: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("course_card_${course.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            CourseThumbnailBox(
                course = course,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${course.board} • ${course.className}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isEnrolled) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "ENROLLED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = course.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${course.lessonCount} Lessons",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    color = Color.Transparent
                ) {
                    Text(
                        text = course.batchYear,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
