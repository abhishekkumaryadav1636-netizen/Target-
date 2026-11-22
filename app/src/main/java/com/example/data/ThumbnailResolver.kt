package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Base64
import androidx.annotation.DrawableRes
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.memory.MemoryCache
import coil.request.Options
import com.example.R
import com.example.model.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ThumbnailPreset(
    val key: String,
    val label: String,
    @DrawableRes val drawableRes: Int
) {
    val presetUri: String get() = "preset://$key"
}

object ThumbnailResolver {

    val presets: List<ThumbnailPreset> = listOf(
        ThumbnailPreset("thumb_up_class_9", "UP Board • Class 9", R.drawable.thumb_up_class_9),
        ThumbnailPreset("thumb_up_class_10", "UP Board • Class 10", R.drawable.thumb_up_class_10),
        ThumbnailPreset("thumb_up_class_11", "UP Board • Class 11", R.drawable.thumb_up_class_11),
        ThumbnailPreset("thumb_up_class_12", "UP Board • Class 12", R.drawable.thumb_up_class_12),
        ThumbnailPreset("thumb_bihar_class_9", "Bihar Board • Class 9", R.drawable.thumb_bihar_class_9),
        ThumbnailPreset("thumb_bihar_class_10", "Bihar Board • Class 10", R.drawable.thumb_bihar_class_10),
        ThumbnailPreset("thumb_bihar_class_11", "Bihar Board • Class 11", R.drawable.thumb_bihar_class_11),
        ThumbnailPreset("thumb_bihar_class_12", "Bihar Board • Class 12", R.drawable.thumb_bihar_class_12),
        ThumbnailPreset("thumb_cuet_batch", "CUET UG Batch", R.drawable.thumb_cuet_batch),
        ThumbnailPreset("thumb_olympiad_foundation", "Science & Maths Olympiad", R.drawable.thumb_olympiad_foundation),
        ThumbnailPreset("thumb_spoken_english", "Spoken English Pro", R.drawable.thumb_spoken_english),
        ThumbnailPreset("thumb_computer_coding", "Computer & Coding Pro", R.drawable.thumb_computer_coding)
    )

    private val presetMap: Map<String, Int> = presets.associate { it.key to it.drawableRes }

    @Volatile
    private var cachedImageLoader: ImageLoader? = null

    fun getImageLoader(context: Context): ImageLoader {
        return cachedImageLoader ?: synchronized(this) {
            cachedImageLoader ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    add(Base64DataUriFetcher.Factory())
                }
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("thumbnail_image_cache"))
                        .maxSizeBytes(50L * 1024L * 1024L)
                        .build()
                }
                .crossfade(true)
                .respectCacheHeaders(false)
                .build()
                .also { cachedImageLoader = it }
        }
    }

    @DrawableRes
    fun fallbackDrawableFor(course: Course): Int {
        val byFallbackKey = presetMap["thumb_${course.fallbackKey}"] ?: presetMap[course.fallbackKey]
        if (byFallbackKey != null) return byFallbackKey

        val byId = presetMap["thumb_${course.id}"] ?: presetMap[course.id]
        if (byId != null) return byId

        val normalized = "${course.board} ${course.className} ${course.title}".lowercase()
        return when {
            "up" in normalized && "9" in normalized && "10" !in normalized && "11" !in normalized && "12" !in normalized ->
                R.drawable.thumb_up_class_9
            "up" in normalized && "10" in normalized -> R.drawable.thumb_up_class_10
            "up" in normalized && "11" in normalized -> R.drawable.thumb_up_class_11
            "up" in normalized && "12" in normalized -> R.drawable.thumb_up_class_12
            "bihar" in normalized && "9" in normalized && "10" !in normalized && "11" !in normalized && "12" !in normalized ->
                R.drawable.thumb_bihar_class_9
            "bihar" in normalized && "10" in normalized -> R.drawable.thumb_bihar_class_10
            "bihar" in normalized && "11" in normalized -> R.drawable.thumb_bihar_class_11
            "bihar" in normalized && "12" in normalized -> R.drawable.thumb_bihar_class_12
            "cuet" in normalized -> R.drawable.thumb_cuet_batch
            "olympiad" in normalized || "nso" in normalized || "imo" in normalized -> R.drawable.thumb_olympiad_foundation
            "english" in normalized || "spoken" in normalized -> R.drawable.thumb_spoken_english
            "coding" in normalized || "computer" in normalized || "python" in normalized -> R.drawable.thumb_computer_coding
            else -> presets[(course.id.hashCode().let { if (it < 0) -it else it }) % presets.size].drawableRes
        }
    }

    /**
     * Resolves a course's thumbnailUrl to either a drawable resource ID (for preset:// or blank)
     * or a remote HTTPS / base64 data URI string for Coil AsyncImage.
     */
    fun resolveModel(course: Course): Any {
        val url = course.thumbnailUrl.trim()
        if (url.isBlank()) return fallbackDrawableFor(course)
        if (url.startsWith("preset://")) {
            val key = url.removePrefix("preset://").trim()
            return presetMap[key] ?: fallbackDrawableFor(course)
        }
        return url
    }

    fun isCustomExternalThumbnail(course: Course): Boolean {
        val url = course.thumbnailUrl.trim()
        return url.isNotBlank() && !url.startsWith("preset://")
    }

    /**
     * Center-crops and compresses an image picked from Photo Picker into a 1:1.2 portrait JPEG
     * data URI capped under 145,000 characters so it safely fits in Firestore documents.
     */
    suspend fun encodePickedImageToDataUri(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.applicationContext.contentResolver
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }
                if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
                    return@withContext Result.failure(IllegalArgumentException("Unsupported or empty image file"))
                }

                val targetW = 450
                val targetH = 540 // exact 1:1.2 portrait ratio
                var sampleSize = 1
                while ((boundsOpts.outWidth / sampleSize) > targetW * 2 &&
                    (boundsOpts.outHeight / sampleSize) > targetH * 2
                ) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val rawBitmap = resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                } ?: return@withContext Result.failure(IllegalStateException("Could not decode selected image"))

                val cropped = centerCropPortrait1To12(rawBitmap, targetW, targetH)
                if (cropped !== rawBitmap) rawBitmap.recycle()

                var quality = 80
                var encodedDataUri = ""
                while (quality >= 35) {
                    val out = ByteArrayOutputStream()
                    cropped.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    val bytes = out.toByteArray()
                    val candidate = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                    if (candidate.length <= 145_000) {
                        encodedDataUri = candidate
                        break
                    }
                    quality -= 10
                }
                cropped.recycle()

                if (encodedDataUri.isBlank()) {
                    Result.failure(IllegalArgumentException("Image is too large even after compression. Try a simpler image or HTTPS URL."))
                } else {
                    Result.success(encodedDataUri)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun centerCropPortrait1To12(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val srcW = source.width.toFloat()
        val srcH = source.height.toFloat()
        val targetRatio = 1f / 1.2f
        val srcRatio = srcW / srcH

        val cropW: Int
        val cropH: Int
        val startX: Int
        val startY: Int
        if (srcRatio > targetRatio) {
            cropH = source.height
            cropW = min(source.width, (srcH * targetRatio).roundToInt())
            startX = max(0, (source.width - cropW) / 2)
            startY = 0
        } else {
            cropW = source.width
            cropH = min(source.height, (srcW / targetRatio).roundToInt())
            startX = 0
            startY = max(0, (source.height - cropH) / 2)
        }

        val cropped = Bitmap.createBitmap(source, startX, startY, cropW, cropH)
        val scaled = Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
        if (scaled !== cropped) cropped.recycle()
        return scaled
    }
}

private class Base64DataUriFetcher(
    private val dataUri: String,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val commaIndex = dataUri.indexOf(',')
        val base64Part = if (commaIndex >= 0) dataUri.substring(commaIndex + 1) else dataUri
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Failed to decode thumbnail bitmap")
        return DrawableResult(
            drawable = BitmapDrawable(options.context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val raw = data.toString()
            return if (raw.startsWith("data:image/")) Base64DataUriFetcher(raw, options) else null
        }
    }
}
