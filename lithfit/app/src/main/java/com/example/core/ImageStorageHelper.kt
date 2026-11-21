package com.example.core

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class GarmentCropAspect(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    SQUARE("1:1", 1f),
    PORTRAIT("3:4", 0.75f),
    EDITORIAL("4:5", 0.8f)
}

data class BackgroundRemovalResult(
    val studioBitmap: Bitmap,
    val foregroundFraction: Float,
    val usedFallback: Boolean
)

data class ProcessedGarmentImage(
    val localFilePath: String,
    val thumbnailBase64: String,
    val bitmapForAi: Bitmap
)

object ImageStorageHelper {

    fun createCameraCaptureUri(context: Context): Pair<Uri, File> {
        val cacheDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(cacheDir, "capture_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return uri to file
    }

    fun getPersonalFullBodyPhotoFile(context: Context): File {
        val dir = File(context.filesDir, "personal_preview").apply { mkdirs() }
        return File(dir, "full_body.jpg")
    }

    fun hasPersonalFullBodyPhoto(context: Context): Boolean {
        val file = getPersonalFullBodyPhotoFile(context)
        return file.exists() && file.length() > 0L
    }

    suspend fun savePersonalFullBodyPhoto(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(IllegalArgumentException("Could not read selected photo."))
                val decoded = decodeSampledBitmap(rawBytes, maxDim = 1280)
                    ?: return@withContext Result.failure(IllegalArgumentException("Unsupported image format."))
                val scaled = scaleDownIfNeeded(decoded, 1080)
                val outFile = getPersonalFullBodyPhotoFile(context)
                FileOutputStream(outFile).use { stream ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                }
                Result.success(outFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun deletePersonalFullBodyPhoto(context: Context): Boolean {
        val file = getPersonalFullBodyPhotoFile(context)
        return if (file.exists()) file.delete() else true
    }

    suspend fun saveProfilePhoto(context: Context, uri: Uri, userId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(IllegalArgumentException("Could not read selected photo."))
                val decoded = decodeSampledBitmap(rawBytes, maxDim = 960)
                    ?: return@withContext Result.failure(IllegalArgumentException("Unsupported image format."))
                val scaled = scaleDownIfNeeded(decoded, 640)
                val dir = File(context.filesDir, "profile_photos").apply { mkdirs() }
                val safeId = userId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32)
                val outFile = File(dir, "avatar_${safeId}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { stream ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                }
                Result.success(outFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun deleteProfilePhoto(path: String?): Boolean {
        if (path.isNullOrBlank()) return true
        return runCatching {
            val f = File(path)
            if (f.exists()) f.delete() else true
        }.getOrDefault(false)
    }

    suspend fun loadPersonalFullBodyPhotoBitmap(context: Context): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val file = getPersonalFullBodyPhotoFile(context)
                if (!file.exists() || file.length() == 0L) return@withContext null
                val bytes = file.readBytes()
                decodeSampledBitmap(bytes, maxDim = 1024)
            } catch (_: Exception) {
                null
            }
        }

    suspend fun processUriForCommunity(context: Context, uri: Uri): Result<ProcessedGarmentImage> =
        withContext(Dispatchers.IO) {
            try {
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(IllegalArgumentException("Could not open selected photo."))
                val decoded = decodeSampledBitmap(rawBytes, maxDim = 1280)
                    ?: return@withContext Result.failure(IllegalArgumentException("Unsupported image format."))
                saveBitmapBundle(context, decoded, folder = "community")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun processUriForWardrobe(context: Context, uri: Uri): Result<ProcessedGarmentImage> =
        withContext(Dispatchers.IO) {
            try {
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(IllegalArgumentException("Could not open selected image."))
                val decoded = decodeSampledBitmap(rawBytes, maxDim = 1280)
                    ?: return@withContext Result.failure(IllegalArgumentException("Unsupported image format."))
                saveBitmapBundle(context, decoded, folder = "wardrobe")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun processBitmapForWardrobe(context: Context, bitmap: Bitmap): Result<ProcessedGarmentImage> =
        withContext(Dispatchers.IO) {
            try {
                saveBitmapBundle(context, bitmap, folder = "wardrobe")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun loadRemoteBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val trimmed = imageUrl.trim()
            if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return@withContext null
            val bytes = URL(trimmed).openStream().use { it.readBytes() }
            decodeSampledBitmap(bytes, maxDim = 1080)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun processRemoteUrlForWardrobe(context: Context, imageUrl: String): Result<ProcessedGarmentImage> =
        withContext(Dispatchers.IO) {
            try {
                val trimmed = imageUrl.trim()
                require(trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
                    "Image URL must start with http:// or https://"
                }
                val bytes = URL(trimmed).openStream().use { it.readBytes() }
                val decoded = decodeSampledBitmap(bytes, maxDim = 1280)
                    ?: return@withContext Result.failure(IllegalArgumentException("Could not decode remote image."))
                saveBitmapBundle(context, decoded, folder = "wardrobe")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun cropBitmap(
        source: Bitmap,
        zoom: Float = 1f,
        offsetXFrac: Float = 0f,
        offsetYFrac: Float = 0f,
        aspectRatio: Float? = null
    ): Bitmap {
        val srcW = source.width.coerceAtLeast(1)
        val srcH = source.height.coerceAtLeast(1)
        val safeZoom = zoom.coerceIn(1f, 3f)

        val baseW: Float
        val baseH: Float
        if (aspectRatio == null || aspectRatio <= 0f) {
            baseW = srcW.toFloat()
            baseH = srcH.toFloat()
        } else {
            val currentRatio = srcW.toFloat() / srcH.toFloat()
            if (currentRatio > aspectRatio) {
                baseH = srcH.toFloat()
                baseW = baseH * aspectRatio
            } else {
                baseW = srcW.toFloat()
                baseH = baseW / aspectRatio
            }
        }

        val cropW = (baseW / safeZoom).roundToInt().coerceIn(1, srcW)
        val cropH = (baseH / safeZoom).roundToInt().coerceIn(1, srcH)
        val maxShiftX = (srcW - cropW) / 2f
        val maxShiftY = (srcH - cropH) / 2f

        val centerX = srcW / 2f + offsetXFrac.coerceIn(-1f, 1f) * maxShiftX
        val centerY = srcH / 2f + offsetYFrac.coerceIn(-1f, 1f) * maxShiftY

        val left = (centerX - cropW / 2f).roundToInt().coerceIn(0, srcW - cropW)
        val top = (centerY - cropH / 2f).roundToInt().coerceIn(0, srcH - cropH)

        return Bitmap.createBitmap(source, left, top, cropW, cropH)
    }

    suspend fun removeBackgroundFromGarmentBitmap(
        source: Bitmap,
        tolerance: Int = 48,
        backdropColor: Int = Color.rgb(244, 240, 232) // Official LITHFIT Warm Cream #F4F0E8
    ): BackgroundRemovalResult = withContext(Dispatchers.Default) {
        val work = scaleDownIfNeeded(source, 560)
        val w = work.width.coerceAtLeast(1)
        val h = work.height.coerceAtLeast(1)
        val total = w * h
        if (total < 16) {
            return@withContext BackgroundRemovalResult(work, 1f, usedFallback = true)
        }

        val pixels = IntArray(total)
        work.getPixels(pixels, 0, w, 0, 0, w, h)

        // Collect representative border pixels (perimeter band)
        val borderColors = ArrayList<Int>(2 * (w + h))
        val stepX = max(1, w / 48)
        val stepY = max(1, h / 48)
        for (x in 0 until w step stepX) {
            borderColors.add(pixels[x])
            borderColors.add(pixels[(h - 1) * w + x])
        }
        for (y in 0 until h step stepY) {
            borderColors.add(pixels[y * w])
            borderColors.add(pixels[y * w + (w - 1)])
        }

        var avgR = 0L
        var avgG = 0L
        var avgB = 0L
        borderColors.forEach { c ->
            avgR += Color.red(c)
            avgG += Color.green(c)
            avgB += Color.blue(c)
        }
        val bCount = borderColors.size.coerceAtLeast(1)
        val refR = (avgR / bCount).toInt()
        val refG = (avgG / bCount).toInt()
        val refB = (avgB / bCount).toInt()

        val tolSq = tolerance.coerceIn(18, 95).let { it * it * 3 }
        val visited = BooleanArray(total)
        val isBg = BooleanArray(total)
        val queue = IntArray(total)
        var head = 0
        var tail = 0

        fun colorDistSq(c1: Int, r2: Int, g2: Int, b2: Int): Int {
            val dr = Color.red(c1) - r2
            val dg = Color.green(c1) - g2
            val db = Color.blue(c1) - b2
            return dr * dr + dg * dg + db * db
        }

        fun tryEnqueue(idx: Int, parentColor: Int) {
            if (visited[idx]) return
            val px = pixels[idx]
            val distMean = colorDistSq(px, refR, refG, refB)
            val distLocal = colorDistSq(
                px,
                Color.red(parentColor),
                Color.green(parentColor),
                Color.blue(parentColor)
            )
            // Matches border backdrop OR smooth local backdrop gradient within strict envelope
            if (distMean <= tolSq || (distLocal <= (tolSq / 3) && distMean <= tolSq * 2)) {
                visited[idx] = true
                isBg[idx] = true
                queue[tail++] = idx
            }
        }

        // Seed flood fill from all 4 image borders
        for (x in 0 until w) {
            val topIdx = x
            val botIdx = (h - 1) * w + x
            tryEnqueue(topIdx, pixels[topIdx])
            tryEnqueue(botIdx, pixels[botIdx])
        }
        for (y in 0 until h) {
            val leftIdx = y * w
            val rightIdx = y * w + (w - 1)
            tryEnqueue(leftIdx, pixels[leftIdx])
            tryEnqueue(rightIdx, pixels[rightIdx])
        }

        while (head < tail) {
            val cur = queue[head++]
            val curColor = pixels[cur]
            val cx = cur % w
            val cy = cur / w
            if (cx > 0) tryEnqueue(cur - 1, curColor)
            if (cx + 1 < w) tryEnqueue(cur + 1, curColor)
            if (cy > 0) tryEnqueue(cur - w, curColor)
            if (cy + 1 < h) tryEnqueue(cur + w, curColor)
        }

        var fgCount = 0
        for (i in 0 until total) {
            if (!isBg[i]) fgCount++
        }
        val fgFrac = fgCount.toFloat() / total.toFloat()
        // Graceful fallback if segmentation isolated too little or almost nothing
        if (fgFrac < 0.08f || fgFrac > 0.96f) {
            return@withContext BackgroundRemovalResult(
                studioBitmap = work,
                foregroundFraction = fgFrac,
                usedFallback = true
            )
        }

        // Compose soft feathered alpha mask onto neutral studio backdrop
        val outPixels = IntArray(total)
        val bgR = Color.red(backdropColor)
        val bgG = Color.green(backdropColor)
        val bgB = Color.blue(backdropColor)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                if (isBg[idx]) {
                    outPixels[idx] = backdropColor
                } else {
                    // Check 4-neighbor background count for subtle anti-aliased edge blend
                    var bgNeighbors = 0
                    if (x > 0 && isBg[idx - 1]) bgNeighbors++
                    if (x + 1 < w && isBg[idx + 1]) bgNeighbors++
                    if (y > 0 && isBg[idx - w]) bgNeighbors++
                    if (y + 1 < h && isBg[idx + w]) bgNeighbors++

                    val px = pixels[idx]
                    if (bgNeighbors == 0) {
                        outPixels[idx] = Color.rgb(Color.red(px), Color.green(px), Color.blue(px))
                    } else {
                        val alpha = (1f - bgNeighbors * 0.14f).coerceIn(0.42f, 1f)
                        val nr = (Color.red(px) * alpha + bgR * (1f - alpha)).roundToInt().coerceIn(0, 255)
                        val ng = (Color.green(px) * alpha + bgG * (1f - alpha)).roundToInt().coerceIn(0, 255)
                        val nb = (Color.blue(px) * alpha + bgB * (1f - alpha)).roundToInt().coerceIn(0, 255)
                        outPixels[idx] = Color.rgb(nr, ng, nb)
                    }
                }
            }
        }

        val studio = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        studio.setPixels(outPixels, 0, w, 0, 0, w, h)
        BackgroundRemovalResult(
            studioBitmap = studio,
            foregroundFraction = fgFrac,
            usedFallback = false
        )
    }

    suspend fun saveGeneratedPreviewBitmap(context: Context, bitmap: Bitmap): Result<ProcessedGarmentImage> =
        withContext(Dispatchers.IO) {
            try {
                saveBitmapBundle(context, bitmap, folder = "previews")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun saveBitmapToDeviceGallery(
        context: Context,
        bitmap: Bitmap,
        title: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val safeTitle = title.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32).ifBlank { "lithfit_look" }
            val filename = "LITHFIT_${safeTitle}_${System.currentTimeMillis()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/LITHFIT")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(IllegalStateException("Could not access Photos gallery."))

                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)) {
                            throw IllegalStateException("Failed to write image bytes.")
                        }
                    } ?: throw IllegalStateException("Unable to open gallery output stream.")

                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    Result.success("Saved to Pictures/LITHFIT/$filename")
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    Result.failure(e)
                }
            } else {
                // On pre-Android 10 devices, save inside app-private Pictures/LITHFIT without broad storage permissions
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let { File(it, "LITHFIT") }
                    ?: File(context.filesDir, "previews")
                dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                }
                Result.success("Saved to ${file.name}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareBitmapViaChooser(context: Context, bitmap: Bitmap, title: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val shareDir = File(context.cacheDir, "camera").apply { mkdirs() }
                val file = File(shareDir, "lithfit_share_${UUID.randomUUID()}.jpg")
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "LITHFIT Look: $title")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                withContext(Dispatchers.Main) {
                    val chooser = Intent.createChooser(shareIntent, "Save or Share LITHFIT Outfit").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            decodeSampledBitmap(rawBytes, maxDim = 1024)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64Thumbnail(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveBitmapBundle(
        context: Context,
        source: Bitmap,
        folder: String
    ): Result<ProcessedGarmentImage> {
        val scaledFull = scaleDownIfNeeded(source, 1080)
        val outDir = File(context.filesDir, folder).apply { mkdirs() }
        val outFile = File(outDir, "lithfit_${UUID.randomUUID()}.jpg")
        FileOutputStream(outFile).use { stream ->
            scaledFull.compress(Bitmap.CompressFormat.JPEG, 86, stream)
        }

        val thumb = scaleDownIfNeeded(scaledFull, 240)
        val thumbStream = ByteArrayOutputStream()
        thumb.compress(Bitmap.CompressFormat.JPEG, 72, thumbStream)
        val base64 = Base64.encodeToString(thumbStream.toByteArray(), Base64.NO_WRAP)
        val safeBase64 = if (base64.length <= 150_000) base64 else ""

        val aiBitmap = scaleDownIfNeeded(scaledFull, 768)
        return Result.success(
            ProcessedGarmentImage(
                localFilePath = outFile.absolutePath,
                thumbnailBase64 = safeBase64,
                bitmapForAi = aiBitmap
            )
        )
    }

    private fun decodeSampledBitmap(bytes: ByteArray, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val maxOrig = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (maxOrig / sample > maxDim * 2) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
        return scaleDownIfNeeded(decoded, maxDim)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val matrix = Matrix().apply { postScale(ratio, ratio) }
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
    }
}
