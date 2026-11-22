package com.example

import com.example.data.DefaultCoursesCatalog
import com.example.data.ThumbnailResolver
import com.example.model.Course
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseThumbnailUniquenessTest {

    @Test
    fun everyDefaultCourseHasUniqueThumbnailDrawable() {
        val courses = DefaultCoursesCatalog.courses
        assertTrue("Expected at least 9 core courses", courses.size >= 12)

        val fallbackDrawables = courses.map { ThumbnailResolver.fallbackDrawableFor(it) }
        assertEquals(
            "Every course must map to its own distinct fallback thumbnail drawable",
            courses.size,
            fallbackDrawables.toSet().size
        )

        val thumbnailUrls = courses.map { it.thumbnailUrl }
        assertEquals(
            "Every course must have a unique default thumbnailUrl",
            courses.size,
            thumbnailUrls.toSet().size
        )
    }

    @Test
    fun missingThumbnailFallsBackToCourseSpecificPoster() {
        val up10 = Course(
            id = "custom_up_10",
            title = "Class 10 UP Board Special",
            board = "UP Board",
            className = "Class 10",
            thumbnailUrl = ""
        )
        val bihar10 = Course(
            id = "custom_bihar_10",
            title = "Bihar Board Class 10 Ranker",
            board = "Bihar Board",
            className = "Class 10",
            thumbnailUrl = ""
        )
        val cuet = Course(
            id = "custom_cuet",
            title = "CUET UG Crash Course",
            board = "NTA CUET",
            className = "UG Entrance",
            thumbnailUrl = ""
        )

        val resUp10 = ThumbnailResolver.fallbackDrawableFor(up10)
        val resBihar10 = ThumbnailResolver.fallbackDrawableFor(bihar10)
        val resCuet = ThumbnailResolver.fallbackDrawableFor(cuet)

        assertEquals(R.drawable.thumb_up_class_10, resUp10)
        assertEquals(R.drawable.thumb_bihar_class_10, resBihar10)
        assertEquals(R.drawable.thumb_cuet_batch, resCuet)
        assertNotEquals(resUp10, resBihar10)
    }
}
