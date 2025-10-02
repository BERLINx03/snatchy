package com.berlin.snatchy.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.berlin.snatchy.domain.model.StorageResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject

/**
 * @author Abdallah Elsokkary
 */
const val CACHE_DIR_NAME = ".cached_statuses"
class WhatsappStatusRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WhatsappRepository"
    }

    private val mDir: File? by lazy {
        context.getExternalFilesDirs(null)
            .filterNotNull()
            .firstOrNull()
            ?.let { File(it, CACHE_DIR_NAME) }
            ?.apply {
                if (!exists()) mkdirs()
            }
    }

    fun fetchWhatsappStatuses(): Flow<StorageResponse> {
        return flow {
            emit(StorageResponse.Loading)

            val possiblePaths = listOf(
                File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/.Statuses"),
                File(
                    Environment.getExternalStorageDirectory(),
                    "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
                ),
                File(
                    Environment.getExternalStorageDirectory(),
                    "WhatsApp Business/Media/.Statuses"
                ),
                File(
                    Environment.getExternalStorageDirectory(),
                    "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
                ),
                File(
                    Environment.getExternalStorageDirectory(),
                    "Android/media/com.whatsapp/WhatsApp/.Statuses"
                ),
                File("/storage/sdcard1/WhatsApp/Media/.Statuses"),
                File("/storage/sdcard1/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            )

            val statusFiles = mutableListOf<File>()

            for (directory in possiblePaths) {
                if (directory.exists() && directory.isDirectory) {
                    directory.listFiles()?.filter { file ->
                        file.isFile && isSupportedStatusFile(file)
                    }?.let { files ->
                        statusFiles.addAll(files)
                    }
                }
            }

            if (statusFiles.isNotEmpty()) {
                val newFiles = filterNewFiles(statusFiles)
                if (newFiles.isNotEmpty()) {
                    cacheStatuses(newFiles)
                    Log.d(TAG, "Cached ${newFiles.size} new statuses")
                }

                val cachedFiles = getCachedStatuses()
                if (cachedFiles.isNotEmpty()) {
                    emit(StorageResponse.Success(statusList = cachedFiles))
                } else {
                    Log.w(TAG, "No cached files available, returning original status files")
                    emit(StorageResponse.Success(statusList = statusFiles))
                }
            } else {
                val cachedFiles = getCachedStatuses()
                if (cachedFiles.isNotEmpty()) {
                    emit(StorageResponse.Success(statusList = cachedFiles))
                } else {
                    emit(StorageResponse.Failure("No supported statuses found"))
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    fun getCachedStatuses(): List<File> {
        val dir = mDir ?: return emptyList()

        return if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.filter { file ->
                file.isFile && isSupportedStatusFile(file)
            }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun filterNewFiles(files: List<File>): List<File> {
        val dir = mDir ?: return files

        if (!dir.exists()) return files

        val cachedNames = dir.listFiles()?.map { it.name }?.toSet() ?: emptySet()

        return files.filter { file ->
            file.name !in cachedNames
        }
    }

    private fun cacheStatuses(files: List<File>) {
        val dir = mDir ?: run {
            Log.e(TAG, "No external storage available for caching")
            return
        }

        try {
            if (!dir.exists()) {
                Log.e(TAG, "Failed to create cache directory")
                return
            }

            files.forEach { sourceFile ->
                val targetFile = File(dir, sourceFile.name)
                sourceFile.copyTo(targetFile, overwrite = true)
                Log.d(TAG, "Cached: ${sourceFile.name} (${sourceFile.length()} bytes)")
            }

            Log.d(TAG, "Successfully cached ${files.size} files to ${dir.absolutePath}")

        } catch (e: IOException) {
            Log.e(TAG, "Error caching files", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied", e)
        }
    }

    fun downloadWhatsappStatus(
        statuses: List<File>,
    ): Flow<StorageResponse> = flow {
        emit(StorageResponse.Loading)
        val downloadedCount = mutableListOf<String>()

        try {
            statuses.forEach { status ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val isVideo = status.name.endsWith(".mp4", ignoreCase = true)
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, status.name)
                            put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(status))
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                            put(
                                MediaStore.MediaColumns.RELATIVE_PATH,
                                if (isVideo) Environment.DIRECTORY_MOVIES + "/Snatchy"
                                else Environment.DIRECTORY_PICTURES + "/Snatchy"
                            )
                        }

                        val collection = if (isVideo) {
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        } else {
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }

                        val uri = context.contentResolver.insert(collection, contentValues)

                        if (uri != null) {
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                FileInputStream(status).use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }

                            contentValues.clear()
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            context.contentResolver.update(uri, contentValues, null, null)

                            downloadedCount.add(status.name)
                            Log.d("WhatsappStatusRepository", "Saved: ${status.name}")
                        } else {
                            Log.e(
                                "WhatsappStatusRepository",
                                "Failed to create MediaStore entry for: ${status.name}"
                            )
                        }
                    } else {
                        val isVideo = status.name.endsWith(".mp4", ignoreCase = true)
                        val desDir = File(
                            Environment.getExternalStoragePublicDirectory(
                                if (isVideo) Environment.DIRECTORY_MOVIES
                                else Environment.DIRECTORY_PICTURES
                            ), "Snatchy"
                        )

                        if (!desDir.exists()) desDir.mkdirs()

                        val statusFile = File(desDir, status.name)
                        FileInputStream(status).use { input ->
                            statusFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(statusFile.absolutePath),
                            arrayOf(getMimeType(status)),
                            null
                        )

                        downloadedCount.add(status.name)
                        Log.d("WhatsappStatusRepository", "Saved: ${status.name}")
                    }
                } catch (e: Exception) {
                    Log.e(
                        "WhatsappStatusRepository",
                        "Failed to save ${status.name}: ${e.message}",
                        e
                    )
                }
            }

            if (downloadedCount.isNotEmpty()) {
                emit(
                    StorageResponse.Success(
                        statusList = statuses,
                        message = "${downloadedCount.size} status${if (downloadedCount.size != 1) "es" else ""} saved to gallery."
                    )
                )
            } else {
                emit(StorageResponse.Failure("No statuses were downloaded."))
            }
        } catch (e: Exception) {
            Log.e("WhatsappStatusRepository", "Download failed: ${e.message}", e)
            emit(StorageResponse.Failure("Failed to download: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp4", "mkv" -> "video/mp4"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }

    private fun isSupportedStatusFile(file: File): Boolean {
        val supportedExtensions = listOf("jpg", "jpeg", "png", "mp4", "gif")
        return file.extension.lowercase() in supportedExtensions
    }
}