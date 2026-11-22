package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Course
import com.example.model.CourseEnrollment
import com.example.model.UiState
import com.example.ui.components.CourseThumbnailBox

@Composable
fun MyBatchesScreen(
    coursesState: UiState<List<Course>>,
    enrollments: Map<String, CourseEnrollment>,
    favoriteIds: Set<String>,
    onToggleFavorite: (Course) -> Unit,
    onCourseClick: (Course) -> Unit,
    onExploreCourses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enrolledCourses = when (coursesState) {
        is UiState.Success -> coursesState.data.filter { it.id in enrollments }
        else -> emptyList()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (enrolledCourses.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Enrolled Batches Yet",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Join any FREE or paid Target Bootcamp batch to track lectures, notes, and revision progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onExploreCourses,
                    modifier = Modifier.testTag("browse_batches_btn")
                ) {
                    Text("Browse Batches")
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 820.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "My Study Batches (${enrolledCourses.size})",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Continue learning where you left off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(enrolledCourses, key = { it.id }) { course ->
                    val enrollment = enrollments[course.id]
                    val progress = (enrollment?.progressPercent ?: 0L).coerceIn(0L, 100L)
                    ElevatedCard(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enrolled_course_${course.id}")
                            .clickable { onCourseClick(course) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CourseThumbnailBox(
                                course = course,
                                isFavorite = course.id in favoriteIds,
                                onToggleFavorite = { onToggleFavorite(course) },
                                modifier = Modifier.width(116.dp)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${course.board} • ${course.batchYear}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = course.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = course.subjectsSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = { progress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "$progress% Completed • ${course.lessonCount} Lessons",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
