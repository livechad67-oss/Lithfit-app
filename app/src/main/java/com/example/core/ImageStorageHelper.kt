package com.example.core

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
