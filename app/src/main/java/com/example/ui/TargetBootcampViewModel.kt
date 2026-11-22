package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TargetBootcampRepository
import com.example.data.ThumbnailResolver
import com.example.model.Course
import com.example.model.CourseEnrollment
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudentFilters(
    val query: String = "",
    val selectedBoard: String = "All",
    val selectedClass: String = "All",
    val freeOnly: Boolean = false
)

class TargetBootcampViewModel(
    private val repository: TargetBootcampRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userPhoneNumber: String?,
    private val userPhotoUrl: String?,
    isBootstrappedAdmin: Boolean
) : ViewModel() {

    private val _filters = MutableStateFlow(StudentFilters())
    val filters: StateFlow<StudentFilters> = _filters.asStateFlow()

    private val _isAdmin = MutableStateFlow(isBootstrappedAdmin)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isAdminActionBusy = MutableStateFlow(false)
    val isAdminActionBusy: StateFlow<Boolean> = _isAdminActionBusy.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    val coursesState: StateFlow<UiState<List<Course>>> = repository.observeCourses()
        .map<List<Course>, UiState<List<Course>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing courses", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load courses"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val filteredCoursesState: StateFlow<UiState<List<Course>>> = combine(coursesState, _filters) { state, filter ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val q = filter.query.trim().lowercase()
                val filtered = state.data.filter { course ->
                    val boardMatch = when (filter.selectedBoard) {
                        "All" -> true
                        "Other" -> course.category !in listOf("UP Board", "Bihar Board", "CUET")
                        else -> course.board.equals(filter.selectedBoard, ignoreCase = true) ||
                            course.category.equals(filter.selectedBoard, ignoreCase = true)
                    }
                    val classMatch = when (filter.selectedClass) {
                        "All" -> true
                        "Entrance" -> course.className.contains("UG", ignoreCase = true) ||
                            course.category.equals("CUET", ignoreCase = true) ||
                            course.className.contains("All", ignoreCase = true)
                        else -> course.className.contains(filter.selectedClass, ignoreCase = true)
                    }
                    val freeMatch = !filter.freeOnly || course.freeBatch || course.priceInr == 0L
                    val queryMatch = q.isEmpty() ||
                        course.title.lowercase().contains(q) ||
                        course.board.lowercase().contains(q) ||
                        course.className.lowercase().contains(q) ||
                        course.subjectsSummary.lowercase().contains(q) ||
                        course.subtitle.lowercase().contains(q)
                    boardMatch && classMatch && freeMatch && queryMatch
                }
                UiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    val favoriteIds: StateFlow<Set<String>> = repository.observeFavoriteIds(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing favorites", firestoreError ?: error)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet()
        )

    val enrollments: StateFlow<Map<String, CourseEnrollment>> = repository.observeEnrollments(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing enrollments", firestoreError ?: error)
            emit(emptyMap())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyMap()
        )

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing profile", firestoreError ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, userDisplayName, userPhoneNumber, userPhotoUrl)
            val adminGranted = repository.checkAdminAccess(currentUserId)
            _isAdmin.value = adminGranted
            if (adminGranted) {
                repository.seedDefaultCoursesIfAdmin()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _filters.value = _filters.value.copy(query = query)
    }

    fun selectBoard(board: String) {
        _filters.value = _filters.value.copy(selectedBoard = board)
    }

    fun selectClass(className: String) {
        _filters.value = _filters.value.copy(selectedClass = className)
    }

    fun toggleFreeOnly() {
        _filters.value = _filters.value.copy(freeOnly = !_filters.value.freeOnly)
    }

    fun clearFilters() {
        _filters.value = StudentFilters()
    }

    fun toggleFavorite(course: Course) {
        val currentlyFav = course.id in favoriteIds.value
        viewModelScope.launch {
            val result = repository.toggleFavorite(currentUserId, course.id, currentlyFav)
            result.onSuccess {
                _snackbarEvents.tryEmit(
                    if (currentlyFav) "Removed ${course.title} from Favourites"
                    else "Added ${course.title} to Favourites"
                )
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to update favourites")
            }
        }
    }

    fun enrollInCourse(course: Course) {
        viewModelScope.launch {
            val result = repository.enrollInCourse(currentUserId, course)
            result.onSuccess {
                _snackbarEvents.tryEmit("Enrolled in ${course.title}!")
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.localizedMessage ?: "Enrollment failed")
            }
        }
    }

    fun advanceProgress(courseId: String, nextPercent: Long) {
        viewModelScope.launch {
            val result = repository.updateEnrollmentProgress(currentUserId, courseId, nextPercent)
            result.onFailure { e ->
                _snackbarEvents.tryEmit(e.localizedMessage ?: "Could not update progress")
            }
        }
    }

    fun uploadPickedThumbnail(context: Context, course: Course, uri: Uri, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isAdminActionBusy.value = true
            val encoded = ThumbnailResolver.encodePickedImageToDataUri(context, uri)
            encoded.fold(
                onSuccess = { dataUri ->
                    val res = repository.updateCourseThumbnail(course, dataUri)
                    _isAdminActionBusy.value = false
                    res.onSuccess {
                        _snackbarEvents.tryEmit("Thumbnail updated for ${course.title} & synced live!")
                        onDone(true)
                    }.onFailure { e ->
                        _snackbarEvents.tryEmit(e.localizedMessage ?: "Permission denied updating thumbnail")
                        onDone(false)
                    }
                },
                onFailure = { e ->
                    _isAdminActionBusy.value = false
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to process selected image")
                    onDone(false)
                }
            )
        }
    }

    fun updateCourseThumbnailUrl(course: Course, newUrl: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isAdminActionBusy.value = true
            val res = repository.updateCourseThumbnail(course, newUrl)
            _isAdminActionBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("Thumbnail updated for ${course.title} & synced live!")
                onDone(true)
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to update thumbnail URL")
                onDone(false)
            }
        }
    }

    fun saveCourse(course: Course, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isAdminActionBusy.value = true
            val res = repository.upsertCourse(course)
            _isAdminActionBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("Saved ${course.title} to Firestore!")
                onDone(true)
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to save course")
                onDone(false)
            }
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            _isAdminActionBusy.value = true
            val res = repository.deleteCourse(course.id)
            _isAdminActionBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("Deleted ${course.title}")
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.localizedMessage ?: "Failed to delete course")
            }
        }
    }

    fun syncDefaultCatalog() {
        viewModelScope.launch {
            _isAdminActionBusy.value = true
            val res = repository.seedDefaultCoursesIfAdmin()
            _isAdminActionBusy.value = false
            res.onSuccess { count ->
                _snackbarEvents.tryEmit(
                    if (count > 0) "Synced $count course batches to Firestore!"
                    else "All default courses are already synced in Firestore."
                )
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.localizedMessage ?: "Sync failed")
            }
        }
    }

    private companion object {
        const val TAG = "TargetBootcampVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
