package com.example.data

import android.content.Context
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.model.Course
import com.example.model.CourseEnrollment
import com.example.model.FavoriteCourse
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TargetBootcampRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    /**
     * Real-time stream of active courses from Firestore merged with DefaultCoursesCatalog
     * so every batch always has complete data and a guaranteed course-specific thumbnail,
     * while any Admin update in Firestore propagates automatically to all students.
     */
    fun observeCourses(): Flow<List<Course>> = flow {
        val uid = requireUserId()
        val path = "courses"
        emitAll(
            db.collection(path)
                .whereEqualTo("catalogActive", true)
                .snapshots()
                .map { snapshot ->
                    val remoteCourses = snapshot.toObjects(
                        Course::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    mergeWithDefaultCatalog(remoteCourses)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    private fun mergeWithDefaultCatalog(remoteCourses: List<Course>): List<Course> {
        val remoteById = remoteCourses.associateBy { it.id }
        val mergedDefaults = DefaultCoursesCatalog.courses.map { defaultCourse ->
            val remote = remoteById[defaultCourse.id]
            if (remote != null) {
                remote.copy(
                    fallbackKey = remote.fallbackKey.ifBlank { defaultCourse.fallbackKey }
                )
            } else {
                defaultCourse
            }
        }
        val defaultIds = DefaultCoursesCatalog.courses.map { it.id }.toSet()
        val extraAdminCourses = remoteCourses.filter { it.id !in defaultIds }
        return (mergedDefaults + extraAdminCourses).sortedBy { it.sortOrder }
    }

    fun observeFavoriteIds(userId: String): Flow<Set<String>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$userId/favorites"
        emitAll(
            db.collection("users").document(userId).collection("favorites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FavoriteCourse::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.courseId.ifBlank { it.id } }.toSet()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeEnrollments(userId: String): Flow<Map<String, CourseEnrollment>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$userId/enrollments"
        emitAll(
            db.collection("users").document(userId).collection("enrollments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CourseEnrollment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).associateBy { it.courseId.ifBlank { it.id } }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun checkAdminAccess(userId: String): Boolean {
        val user = auth.currentUser ?: return false
        if (GoogleAuthHelper.isBootstrappedAdmin(user)) return true
        return try {
            val doc = db.collection("admins").document(userId).get().await()
            doc.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        phoneNumber: String?,
        photoUrl: String?
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(userId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "Student" }.take(80)
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "preferredBoard" to "All",
                    "preferredClass" to "All",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!phoneNumber.isNullOrBlank() && phoneNumber.length <= 20) {
                    payload["phoneNumber"] = phoneNumber.trim()
                }
                if (!photoUrl.isNullOrBlank() && photoUrl.length <= 500) {
                    payload["photoUrl"] = photoUrl
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(userId: String, courseId: String, currentlyFavorite: Boolean): Result<Unit> {
        val path = "users/$userId/favorites/$courseId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            require(courseId.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) { "Invalid course ID" }
            val docRef = db.collection("users").document(uid)
                .collection("favorites").document(courseId)
            if (currentlyFavorite) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "courseId" to courseId,
                        "addedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (currentlyFavorite) OperationType.DELETE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun enrollInCourse(userId: String, course: Course): Result<Unit> {
        val path = "users/$userId/enrollments/${course.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
                .collection("enrollments").document(course.id)
            docRef.set(
                mapOf(
                    "courseId" to course.id,
                    "courseTitle" to course.title.take(100),
                    "progressPercent" to 10L,
                    "enrolledAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateEnrollmentProgress(userId: String, courseId: String, progressPercent: Long): Result<Unit> {
        val path = "users/$userId/enrollments/$courseId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val bounded = progressPercent.coerceIn(0L, 100L)
            db.collection("users").document(uid)
                .collection("enrollments").document(courseId)
                .update(
                    mapOf(
                        "progressPercent" to bounded,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Seeds default courses into Firestore if the signed-in user is an admin and any default course
     * does not yet exist in Firestore.
     */
    suspend fun seedDefaultCoursesIfAdmin(): Result<Int> {
        val path = "courses"
        return try {
            val uid = requireUserId()
            if (!checkAdminAccess(uid)) return Result.success(0)

            val existingSnapshot = db.collection(path)
                .whereEqualTo("catalogActive", true)
                .get()
                .await()
            val existingIds = existingSnapshot.documents.map { it.id }.toSet()
            val missing = DefaultCoursesCatalog.courses.filter { it.id !in existingIds }
            if (missing.isEmpty()) return Result.success(0)

            val batch = db.batch()
            missing.forEach { course ->
                val docRef = db.collection(path).document(course.id)
                batch.set(docRef, buildCourseCreateMap(course, uid))
            }
            batch.commit().await()
            Result.success(missing.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Admin operation: replaces or updates a course's thumbnailUrl in Firestore.
     * Creates the Firestore document from DefaultCoursesCatalog if it didn't exist remotely yet.
     */
    suspend fun updateCourseThumbnail(course: Course, newThumbnailUrl: String): Result<Unit> {
        val courseId = course.id
        val path = "courses/$courseId"
        return try {
            val uid = requireUserId()
            require(courseId.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) { "Invalid course ID" }
            val trimmedUrl = newThumbnailUrl.trim()
            require(trimmedUrl.isNotEmpty() && trimmedUrl.length <= 200_000) {
                "Thumbnail URL must be 1 to 200,000 characters"
            }
            val docRef = db.collection("courses").document(courseId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "thumbnailUrl" to trimmedUrl,
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(buildCourseCreateMap(course.copy(thumbnailUrl = trimmedUrl), uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Admin operation: creates or updates a full course record in Firestore.
     */
    suspend fun upsertCourse(course: Course): Result<Unit> {
        val courseId = course.id.ifBlank {
            course.title.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "course_${System.currentTimeMillis()}" }
                .take(64)
        }
        val path = "courses/$courseId"
        return try {
            val uid = requireUserId()
            require(courseId.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) {
                "Course ID must contain only letters, digits, underscores, or hyphens"
            }
            require(course.title.trim().length in 2..100) { "Title must be 2-100 characters" }
            require(course.subtitle.trim().length in 2..160) { "Subtitle must be 2-160 characters" }
            require(course.thumbnailUrl.trim().length <= 200_000) { "Thumbnail URL exceeds limit" }

            val normalized = course.copy(
                id = courseId,
                title = course.title.trim().take(100),
                subtitle = course.subtitle.trim().take(160),
                board = course.board.trim().ifBlank { "UP Board" }.take(40),
                className = course.className.trim().ifBlank { "Class 10" }.take(40),
                batchYear = course.batchYear.trim().ifBlank { "2026-27" }.take(20),
                category = course.category.trim().ifBlank { course.board.trim().ifBlank { "Other Courses" } }.take(40),
                priceInr = if (course.freeBatch) 0L else course.priceInr.coerceIn(0L, 100_000L),
                originalPriceInr = course.originalPriceInr.coerceIn(0L, 100_000L),
                thumbnailUrl = course.thumbnailUrl.trim(),
                fallbackKey = course.fallbackKey.trim().ifBlank { "cuet_batch" }.take(60),
                instructorName = course.instructorName.trim().ifBlank { "Target Bootcamp Faculty" }.take(80),
                subjectsSummary = course.subjectsSummary.trim().ifBlank { "All Core Subjects" }.take(140),
                lessonCount = course.lessonCount.coerceIn(0L, 2000L),
                durationLabel = course.durationLabel.trim().ifBlank { "12 Months" }.take(40),
                sortOrder = course.sortOrder.coerceIn(0L, 1000L)
            )

            val docRef = db.collection("courses").document(courseId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "title" to normalized.title,
                        "subtitle" to normalized.subtitle,
                        "board" to normalized.board,
                        "className" to normalized.className,
                        "batchYear" to normalized.batchYear,
                        "category" to normalized.category,
                        "priceInr" to normalized.priceInr,
                        "originalPriceInr" to normalized.originalPriceInr,
                        "freeBatch" to normalized.freeBatch,
                        "thumbnailUrl" to normalized.thumbnailUrl,
                        "fallbackKey" to normalized.fallbackKey,
                        "instructorName" to normalized.instructorName,
                        "subjectsSummary" to normalized.subjectsSummary,
                        "lessonCount" to normalized.lessonCount,
                        "durationLabel" to normalized.durationLabel,
                        "sortOrder" to normalized.sortOrder,
                        "catalogActive" to normalized.catalogActive,
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(buildCourseCreateMap(normalized, uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCourse(courseId: String): Result<Unit> {
        val path = "courses/$courseId"
        return try {
            requireUserId()
            require(courseId.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) { "Invalid course ID" }
            db.collection("courses").document(courseId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun buildCourseCreateMap(course: Course, uid: String): Map<String, Any> = mapOf(
        "title" to course.title,
        "subtitle" to course.subtitle,
        "board" to course.board,
        "className" to course.className,
        "batchYear" to course.batchYear,
        "category" to course.category,
        "priceInr" to course.priceInr,
        "originalPriceInr" to course.originalPriceInr,
        "freeBatch" to course.freeBatch,
        "thumbnailUrl" to course.thumbnailUrl,
        "fallbackKey" to course.fallbackKey,
        "instructorName" to course.instructorName,
        "subjectsSummary" to course.subjectsSummary,
        "lessonCount" to course.lessonCount,
        "durationLabel" to course.durationLabel,
        "sortOrder" to course.sortOrder,
        "catalogActive" to course.catalogActive,
        "updatedBy" to uid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
