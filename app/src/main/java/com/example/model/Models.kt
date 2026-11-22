package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class Course(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val board: String = "",
    val className: String = "",
    val batchYear: String = "2026-27",
    val category: String = "",
    val priceInr: Long = 0L,
    val originalPriceInr: Long = 0L,
    val freeBatch: Boolean = false,
    val thumbnailUrl: String = "",
    val fallbackKey: String = "",
    val instructorName: String = "",
    val subjectsSummary: String = "",
    val lessonCount: Long = 0L,
    val durationLabel: String = "12 Months",
    val sortOrder: Long = 0L,
    val catalogActive: Boolean = true,
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val discountPercent: Int
        get() = if (!freeBatch && originalPriceInr > priceInr && originalPriceInr > 0L) {
            (((originalPriceInr - priceInr) * 100L) / originalPriceInr).toInt()
        } else {
            0
        }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val preferredBoard: String = "All",
    val preferredClass: String = "All",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteCourse(
    @DocumentId val id: String = "",
    val courseId: String = "",
    @ServerTimestamp val addedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CourseEnrollment(
    @DocumentId val id: String = "",
    val courseId: String = "",
    val courseTitle: String = "",
    val progressPercent: Long = 0L,
    @ServerTimestamp val enrolledAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
