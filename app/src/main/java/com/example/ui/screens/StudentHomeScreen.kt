package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Course
import com.example.model.CourseEnrollment
import com.example.model.UiState
import com.example.ui.StudentFilters
import com.example.ui.components.CourseCatalogCard
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.SaffronAccent

@Composable
fun StudentHomeScreen(
    coursesState: UiState<List<Course>>,
    filters: StudentFilters,
    favoriteIds: Set<String>,
    enrollments: Map<String, CourseEnrollment>,
    userName: String,
    isAdmin: Boolean,
    onQueryChange: (String) -> Unit,
    onBoardSelect: (String) -> Unit,
    onClassSelect: (String) -> Unit,
    onToggleFreeOnly: () -> Unit,
    onClearFilters: () -> Unit,
    onToggleFavorite: (Course) -> Unit,
    onCourseClick: (Course) -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boards = listOf("All", "UP Board", "Bihar Board", "CUET", "Other")
    val classes = listOf("All", "Class 9", "Class 10", "Class 11", "Class 12", "Entrance")

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 165.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1100.dp)
        ) {
            // Top Greeting & Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Target Bootcamp",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isAdmin) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SaffronAccent
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Namaste, $userName • UP, Bihar & CUET 2026-27",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isAdmin) {
                            IconButton(
                                onClick = onOpenAdmin,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("top_admin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = "Open Admin Thumbnail Panel",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Hero Banner
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Target Bootcamp Admissions Open 2026-27",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xE60F172A),
                                        Color(0x991E1B4B),
                                        Color(0x330F172A)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "BOARD TOPPERS & CUET 2026-27",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFDE047)
                                )
                            }
                            Text(
                                text = "Class 9 to 12 & CUET Batches",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Live classes, bilingual notes, PYQs & model papers",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            // Search bar
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = filters.query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search UP Board, Bihar Board, Class 10, CUET...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search courses"
                        )
                    },
                    trailingIcon = {
                        if (filters.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_courses_input")
                )
            }

            // Board Filter Chips + Free Toggle
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        boards.forEach { board ->
                            val selected = filters.selectedBoard == board
                            FilterChip(
                                selected = selected,
                                onClick = { onBoardSelect(board) },
                                label = { Text(board) },
                                modifier = Modifier.testTag("board_chip_$board")
                            )
                        }
                        FilterChip(
                            selected = filters.freeOnly,
                            onClick = onToggleFreeOnly,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.LocalOffer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("FREE Batches") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldFree,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("free_only_chip")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        classes.forEach { cls ->
                            FilterChip(
                                selected = filters.selectedClass == cls,
                                onClick = { onClassSelect(cls) },
                                label = { Text(cls) },
                                modifier = Modifier.testTag("class_chip_$cls")
                            )
                        }
                    }
                }
            }

            // Course Grid Content
            when (coursesState) {
                is UiState.Loading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = coursesState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val courses = coursesState.data
                    if (courses.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No matching batches found",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Try clearing your board, class, or search filters.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = onClearFilters) {
                                    Text("Reset Filters")
                                }
                            }
                        }
                    } else {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "Available Batches (${courses.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(courses, key = { it.id }) { course ->
                            CourseCatalogCard(
                                course = course,
                                isFavorite = course.id in favoriteIds,
                                isEnrolled = course.id in enrollments,
                                onToggleFavorite = { onToggleFavorite(course) },
                                onClick = { onCourseClick(course) }
                            )
                        }
                    }
                }
            }
        }
    }
}
