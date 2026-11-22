package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.Course
import com.example.model.CourseEnrollment
import com.example.model.UiState
import com.example.ui.components.CourseCatalogCard

@Composable
fun FavoritesScreen(
    coursesState: UiState<List<Course>>,
    favoriteIds: Set<String>,
    enrollments: Map<String, CourseEnrollment>,
    onToggleFavorite: (Course) -> Unit,
    onCourseClick: (Course) -> Unit,
    onExploreCourses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteCourses = when (coursesState) {
        is UiState.Success -> coursesState.data.filter { it.id in favoriteIds }
        else -> emptyList()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (favoriteCourses.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Favourite Batches Yet",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap the heart icon on any course poster to save your UP Board, Bihar Board, or CUET batches here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onExploreCourses,
                    modifier = Modifier.testTag("explore_courses_from_favorites_btn")
                ) {
                    Text("Explore Courses")
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 165.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1100.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            text = "Saved Batches (${favoriteCourses.size})",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Your favourited courses synced across devices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(favoriteCourses, key = { it.id }) { course ->
                    CourseCatalogCard(
                        course = course,
                        isFavorite = true,
                        isEnrolled = course.id in enrollments,
                        onToggleFavorite = { onToggleFavorite(course) },
                        onClick = { onCourseClick(course) }
                    )
                }
            }
        }
    }
}
