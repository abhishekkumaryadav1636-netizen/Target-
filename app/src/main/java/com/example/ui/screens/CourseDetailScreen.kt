package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Course
import com.example.model.CourseEnrollment
import com.example.ui.components.CourseThumbnailBox
import com.example.ui.theme.CrimsonFavorite
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.SaffronAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    course: Course,
    isFavorite: Boolean,
    enrollment: CourseEnrollment?,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEnroll: () -> Unit,
    onAdvanceProgress: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = course.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("detail_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Toggle Favourite",
                            tint = if (isFavorite) CrimsonFavorite else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CourseThumbnailBox(
                        course = course,
                        isFavorite = isFavorite,
                        onToggleFavorite = onToggleFavorite,
                        modifier = Modifier.widthIn(max = 300.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = course.board,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${course.className} • Batch ${course.batchYear}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = course.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Batch Highlights",
                            style = MaterialTheme.typography.titleMedium
                        )
                        DetailInfoRow(Icons.Filled.Person, "Faculty", course.instructorName)
                        DetailInfoRow(Icons.AutoMirrored.Filled.MenuBook, "Subjects", course.subjectsSummary)
                        DetailInfoRow(Icons.Filled.PlayArrow, "Content", "${course.lessonCount} Video Lectures, PDFs & Tests")
                        DetailInfoRow(Icons.Filled.Schedule, "Validity", course.durationLabel)
                        DetailInfoRow(Icons.Filled.CalendarMonth, "Batch Year", course.batchYear)
                    }
                }

                if (enrollment != null) {
                    val progress = enrollment.progressPercent.coerceIn(0L, 100L)
                    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldFree
                                )
                                Text(
                                    text = "Enrolled • $progress% Complete",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EmeraldFree
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (progress < 100L) {
                                OutlinedButton(
                                    onClick = { onAdvanceProgress((progress + 20L).coerceAtMost(100L)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("complete_next_module_btn")
                                ) {
                                    Text("Mark Next Module Complete (+20%)")
                                }
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = onEnroll,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (course.freeBatch || course.priceInr == 0L) EmeraldFree else SaffronAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("enroll_course_button")
                    ) {
                        val cta = if (course.freeBatch || course.priceInr == 0L) {
                            "Join FREE Batch Now"
                        } else {
                            "Enroll Now • ₹${course.priceInr}"
                        }
                        Text(
                            text = cta,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DetailInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
