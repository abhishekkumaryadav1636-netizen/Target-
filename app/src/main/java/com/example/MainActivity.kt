package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.GoogleAuthHelper
import com.example.data.TargetBootcampRepository
import com.example.model.UiState
import com.example.ui.TargetBootcampViewModel
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CourseDetailScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.MyBatchesScreen
import com.example.ui.screens.StudentHomeScreen
import com.example.ui.theme.TargetBootcampTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TargetBootcampTheme {
                AppContent()
            }
        }
    }
}

private enum class AppTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    COURSES("courses", "Courses", Icons.Filled.School, Icons.Outlined.School),
    FAVORITES("favorites", "Favourites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    BATCHES("batches", "My Batches", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    ADMIN("admin", "Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings)
}

@Composable
fun AppContent() {
    val auth = remember { Firebase.auth }
    val currentUser by GoogleAuthHelper.authStateFlow(auth)
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedTargetBootcampApp(user = user)
        }
    }
}

@Composable
private fun AuthenticatedTargetBootcampApp(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: TargetBootcampViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repo = TargetBootcampRepository(db)
                TargetBootcampViewModel(
                    repository = repo,
                    currentUserId = user.uid,
                    userDisplayName = user.displayName?.takeIf { it.isNotBlank() }
                        ?: user.phoneNumber?.takeIf { it.isNotBlank() }
                        ?: user.email?.substringBefore("@")
                        ?: "Student",
                    userPhoneNumber = user.phoneNumber,
                    userPhotoUrl = user.photoUrl?.toString(),
                    isBootstrappedAdmin = GoogleAuthHelper.isBootstrappedAdmin(user)
                )
            }
        }
    )

    val coursesState by viewModel.coursesState.collectAsStateWithLifecycle()
    val filteredCoursesState by viewModel.filteredCoursesState.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val enrollments by viewModel.enrollments.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isAdminActionBusy by viewModel.isAdminActionBusy.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(AppTab.COURSES) }
    var selectedCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val selectedCourse = remember(coursesState, selectedCourseId) {
        val id = selectedCourseId ?: return@remember null
        (coursesState as? UiState.Success)?.data?.firstOrNull { it.id == id }
    }

    if (selectedCourse != null) {
        CourseDetailScreen(
            course = selectedCourse,
            isFavorite = selectedCourse.id in favoriteIds,
            enrollment = enrollments[selectedCourse.id],
            onBack = { selectedCourseId = null },
            onToggleFavorite = { viewModel.toggleFavorite(selectedCourse) },
            onEnroll = { viewModel.enrollInCourse(selectedCourse) },
            onAdvanceProgress = { next -> viewModel.advanceProgress(selectedCourse.id, next) }
        )
        return
    }

    val displayName = user.displayName?.takeIf { it.isNotBlank() }
        ?: user.phoneNumber?.takeIf { it.isNotBlank() }
        ?: user.email?.substringBefore("@")
        ?: "Student"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = isSelected,
                                        favoritesCount = favoriteIds.size
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.statusBarsPadding()) {
                        AppTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = isSelected,
                                        favoritesCount = favoriteIds.size
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTab) {
                        AppTab.COURSES -> StudentHomeScreen(
                            coursesState = filteredCoursesState,
                            filters = filters,
                            favoriteIds = favoriteIds,
                            enrollments = enrollments,
                            userName = displayName,
                            isAdmin = isAdmin,
                            onQueryChange = viewModel::updateSearchQuery,
                            onBoardSelect = viewModel::selectBoard,
                            onClassSelect = viewModel::selectClass,
                            onToggleFreeOnly = viewModel::toggleFreeOnly,
                            onClearFilters = viewModel::clearFilters,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onCourseClick = { course -> selectedCourseId = course.id },
                            onOpenAdmin = { selectedTab = AppTab.ADMIN },
                            onSignOut = {
                                GoogleAuthHelper.signOut(credentialManager, scope)
                            }
                        )

                        AppTab.FAVORITES -> FavoritesScreen(
                            coursesState = coursesState,
                            favoriteIds = favoriteIds,
                            enrollments = enrollments,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onCourseClick = { course -> selectedCourseId = course.id },
                            onExploreCourses = { selectedTab = AppTab.COURSES }
                        )

                        AppTab.BATCHES -> MyBatchesScreen(
                            coursesState = coursesState,
                            enrollments = enrollments,
                            favoriteIds = favoriteIds,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onCourseClick = { course -> selectedCourseId = course.id },
                            onExploreCourses = { selectedTab = AppTab.COURSES }
                        )

                        AppTab.ADMIN -> AdminPanelScreen(
                            coursesState = coursesState,
                            isAdmin = isAdmin,
                            isBusy = isAdminActionBusy,
                            userEmail = user.email?.takeIf { it.isNotBlank() }
                                ?: user.phoneNumber?.takeIf { it.isNotBlank() }
                                ?: "Signed-in account",
                            onUploadPickedImage = { course, uri, onDone ->
                                viewModel.uploadPickedThumbnail(context, course, uri) { ok ->
                                    if (ok) onDone()
                                }
                            },
                            onUpdateThumbnailUrl = { course, url, onDone ->
                                viewModel.updateCourseThumbnailUrl(course, url) { ok ->
                                    if (ok) onDone()
                                }
                            },
                            onSaveCourse = { course, onDone ->
                                viewModel.saveCourse(course) { ok ->
                                    if (ok) onDone()
                                }
                            },
                            onSyncCatalog = viewModel::syncDefaultCatalog
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: AppTab,
    selected: Boolean,
    favoritesCount: Int
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (tab == AppTab.FAVORITES && favoritesCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(favoritesCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = tab.label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = tab.label)
    }
}
