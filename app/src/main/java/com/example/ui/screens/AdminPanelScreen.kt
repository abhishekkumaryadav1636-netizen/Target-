package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.auth.GoogleAuthHelper
import com.example.data.ThumbnailResolver
import com.example.model.Course
import com.example.model.UiState
import com.example.ui.components.CourseThumbnailBox
import com.example.ui.theme.SaffronAccent

@Composable
fun AdminPanelScreen(
    coursesState: UiState<List<Course>>,
    isAdmin: Boolean,
    isBusy: Boolean,
    userEmail: String,
    onUploadPickedImage: (Course, Uri, () -> Unit) -> Unit,
    onUpdateThumbnailUrl: (Course, String, () -> Unit) -> Unit,
    onSaveCourse: (Course, () -> Unit) -> Unit,
    onSyncCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var thumbnailEditingCourse by remember { mutableStateOf<Course?>(null) }
    var courseEditingTarget by remember { mutableStateOf<Course?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    // Android Photo Picker (zero storage permission required)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val target = thumbnailEditingCourse
        if (uri != null && target != null) {
            onUploadPickedImage(target, uri) {
                thumbnailEditingCourse = null
            }
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = {
                        isCreatingNew = true
                        courseEditingTarget = Course(
                            id = "",
                            title = "",
                            subtitle = "",
                            board = "UP Board",
                            className = "Class 10",
                            batchYear = "2026-27",
                            category = "UP Board",
                            priceInr = 499L,
                            originalPriceInr = 1999L,
                            freeBatch = false,
                            thumbnailUrl = "preset://thumb_up_class_10",
                            fallbackKey = "up_class_10",
                            instructorName = "Target Bootcamp Faculty",
                            subjectsSummary = "Science, Maths, English, Hindi",
                            lessonCount = 120L,
                            durationLabel = "12 Months",
                            sortOrder = 20L,
                            catalogActive = true
                        )
                    },
                    modifier = Modifier.testTag("admin_add_course_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add new course")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 900.dp)
            ) {
                item {
                    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAdmin) Icons.Filled.CloudDone else Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = if (isAdmin) MaterialTheme.colorScheme.tertiary else SaffronAccent
                                )
                                Text(
                                    text = "Admin Thumbnail & Course Panel",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isAdmin) {
                                Text(
                                    text = "Signed in as Admin ($userEmail). Upload or replace any course's 1:1.2 thumbnail from Photo Picker, hosted URL, or poster preset — changes sync live to all students via Firestore.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = onSyncCatalog,
                                        enabled = !isBusy,
                                        modifier = Modifier.testTag("admin_sync_catalog_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CloudSync,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sync All Default Courses")
                                    }
                                    if (isBusy) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                    }
                                }
                            } else {
                                Text(
                                    text = "Read-only preview ($userEmail). Firestore security rules allow course & thumbnail writes only for the verified admin account (${GoogleAuthHelper.BOOTSTRAPPED_ADMIN_EMAIL}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                when (coursesState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    is UiState.Error -> {
                        item {
                            Text(
                                text = coursesState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    is UiState.Success -> {
                        items(coursesState.data, key = { it.id }) { course ->
                            AdminCourseThumbnailRow(
                                course = course,
                                isAdmin = isAdmin,
                                isBusy = isBusy,
                                onReplaceThumbnail = { thumbnailEditingCourse = course },
                                onEditCourse = {
                                    isCreatingNew = false
                                    courseEditingTarget = course
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }

    // Dialog 1: Replace / Upload Thumbnail for a Course
    val activeThumbCourse = thumbnailEditingCourse
    if (activeThumbCourse != null) {
        ThumbnailEditorDialog(
            course = activeThumbCourse,
            isBusy = isBusy,
            onDismiss = { thumbnailEditingCourse = null },
            onPickFromGallery = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSaveUrlOrPreset = { newUrl ->
                onUpdateThumbnailUrl(activeThumbCourse, newUrl) {
                    thumbnailEditingCourse = null
                }
            }
        )
    }

    // Dialog 2: Create / Edit Full Course Details + Thumbnail
    val activeEditCourse = courseEditingTarget
    if (activeEditCourse != null) {
        CourseDetailsEditorDialog(
            initialCourse = activeEditCourse,
            isNew = isCreatingNew,
            isBusy = isBusy,
            onDismiss = { courseEditingTarget = null },
            onSave = { updated ->
                onSaveCourse(updated) {
                    courseEditingTarget = null
                }
            }
        )
    }
}

@Composable
private fun AdminCourseThumbnailRow(
    course: Course,
    isAdmin: Boolean,
    isBusy: Boolean,
    onReplaceThumbnail: () -> Unit,
    onEditCourse: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_course_row_${course.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CourseThumbnailBox(
                course = course,
                isFavorite = false,
                onToggleFavorite = null,
                modifier = Modifier.width(104.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = course.batchYear,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val sourceLabel = when {
                    course.thumbnailUrl.startsWith("data:image/") -> "Uploaded Photo (Synced in Firestore)"
                    course.thumbnailUrl.startsWith("http") -> "Hosted URL: ${course.thumbnailUrl}"
                    course.thumbnailUrl.startsWith("preset://") -> "Poster Preset: ${course.thumbnailUrl.removePrefix("preset://")}"
                    else -> "Course Fallback Poster (${course.fallbackKey})"
                }
                Text(
                    text = sourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onReplaceThumbnail,
                        enabled = isAdmin && !isBusy,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("admin_change_thumb_${course.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Thumbnail", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onEditCourse,
                        enabled = isAdmin && !isBusy,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("admin_edit_course_${course.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailEditorDialog(
    course: Course,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onPickFromGallery: () -> Unit,
    onSaveUrlOrPreset: (String) -> Unit
) {
    var draftUrl by remember(course.id) { mutableStateOf(course.thumbnailUrl) }
    val previewCourse = remember(course, draftUrl) { course.copy(thumbnailUrl = draftUrl) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text("Update Thumbnail • ${course.title}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CourseThumbnailBox(
                        course = previewCourse,
                        isFavorite = false,
                        onToggleFavorite = null,
                        modifier = Modifier.width(150.dp)
                    )
                }

                Button(
                    onClick = onPickFromGallery,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pick_gallery_btn")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Photo from Device")
                }

                OutlinedTextField(
                    value = if (draftUrl.startsWith("data:image/")) "" else draftUrl,
                    onValueChange = { draftUrl = it },
                    label = { Text("Image URL (https://...) or preset://") },
                    placeholder = { Text("https://firebasestorage.googleapis.com/...") },
                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_thumbnail_url_input")
                )

                Text(
                    text = "Or pick a Target Bootcamp Poster Preset:",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThumbnailResolver.presets.forEach { preset ->
                        FilterChip(
                            selected = draftUrl == preset.presetUri,
                            onClick = { draftUrl = preset.presetUri },
                            label = { Text(preset.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalUrl = draftUrl.trim().ifBlank { "preset://thumb_${course.fallbackKey}" }
                    onSaveUrlOrPreset(finalUrl)
                },
                enabled = !isBusy && draftUrl.isNotBlank(),
                modifier = Modifier.testTag("admin_save_thumbnail_btn")
            ) {
                Text(if (isBusy) "Saving..." else "Save & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CourseDetailsEditorDialog(
    initialCourse: Course,
    isNew: Boolean,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit
) {
    var title by remember { mutableStateOf(initialCourse.title) }
    var subtitle by remember { mutableStateOf(initialCourse.subtitle) }
    var board by remember { mutableStateOf(initialCourse.board) }
    var className by remember { mutableStateOf(initialCourse.className) }
    var batchYear by remember { mutableStateOf(initialCourse.batchYear) }
    var subjects by remember { mutableStateOf(initialCourse.subjectsSummary) }
    var priceText by remember { mutableStateOf(initialCourse.priceInr.toString()) }
    var originalPriceText by remember { mutableStateOf(initialCourse.originalPriceInr.toString()) }
    var freeBatch by remember { mutableStateOf(initialCourse.freeBatch) }
    var selectedPresetKey by remember { mutableStateOf(initialCourse.fallbackKey.ifBlank { "up_class_10" }) }
    var thumbnailUrl by remember { mutableStateOf(initialCourse.thumbnailUrl) }

    val boardChoices = listOf("UP Board", "Bihar Board", "NTA CUET", "Olympiad", "Skill Pro", "Tech Batch")

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text(if (isNew) "Add New Course Batch" else "Edit ${initialCourse.title}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Name (e.g. Class 10 UP Board)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Batch Tagline / Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Board", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    boardChoices.forEach { option ->
                        FilterChip(
                            selected = board == option,
                            onClick = { board = option },
                            label = { Text(option) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text("Class") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = batchYear,
                        onValueChange = { batchYear = it },
                        label = { Text("Batch Year") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = subjects,
                    onValueChange = { subjects = it },
                    label = { Text("Subjects") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FREE Batch Badge", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = freeBatch, onCheckedChange = { freeBatch = it })
                }
                if (!freeBatch) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it.filter(Char::isDigit) },
                            label = { Text("Price (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = originalPriceText,
                            onValueChange = { originalPriceText = it.filter(Char::isDigit) },
                            label = { Text("MRP (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = thumbnailUrl,
                    onValueChange = { thumbnailUrl = it },
                    label = { Text("Thumbnail URL / preset://") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThumbnailResolver.presets.forEach { preset ->
                        FilterChip(
                            selected = thumbnailUrl == preset.presetUri,
                            onClick = {
                                thumbnailUrl = preset.presetUri
                                selectedPresetKey = preset.key.removePrefix("thumb_")
                            },
                            label = { Text(preset.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val category = when {
                        board.contains("UP", ignoreCase = true) -> "UP Board"
                        board.contains("Bihar", ignoreCase = true) -> "Bihar Board"
                        board.contains("CUET", ignoreCase = true) -> "CUET"
                        else -> "Other Courses"
                    }
                    val price = if (freeBatch) 0L else (priceText.toLongOrNull() ?: 499L)
                    val mrp = originalPriceText.toLongOrNull() ?: 1999L
                    onSave(
                        initialCourse.copy(
                            title = title,
                            subtitle = subtitle.ifBlank { "$board • $className Batch $batchYear" },
                            board = board,
                            className = className,
                            batchYear = batchYear,
                            category = category,
                            priceInr = price,
                            originalPriceInr = mrp,
                            freeBatch = freeBatch,
                            thumbnailUrl = thumbnailUrl.ifBlank { "preset://thumb_$selectedPresetKey" },
                            fallbackKey = selectedPresetKey,
                            subjectsSummary = subjects
                        )
                    )
                },
                enabled = !isBusy && title.trim().length >= 2
            ) {
                Text(if (isBusy) "Saving..." else "Save Course")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Cancel")
            }
        }
    )
}
