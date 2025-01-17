package com.berlin.snatchy.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
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
class WhatsappStatusRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun fetchWhatsappStatuses(): Flow<StorageResponse> {
        return flow {
            emit(StorageResponse.Loading)

            val hasPermissions = hasRequiredPermissions(context)
            Log.d("WhatsappRepo", "Has permissions: $hasPermissions")

            if (!hasPermissions) {
                emit(StorageResponse.Failure("Required permissions not granted"))
                return@flow
            }

            val possiblePaths = listOf(
                // Original path
                File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/.Statuses"),
                // New path for Android 10+
                File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                // Business WhatsApp path
                File(Environment.getExternalStorageDirectory(), "WhatsApp Business/Media/.Statuses")
            )

            val statusFiles = mutableListOf<File>()

            for (directory in possiblePaths) {
                Log.d("WhatsappRepo", "Checking directory: ${directory.absolutePath}")
                if (directory.exists() && directory.isDirectory) {
                    directory.listFiles()?.filter { file ->
                        file.isFile && isSupportedStatusFile(file)
                    }?.let { files ->
                        statusFiles.addAll(files)
                    }
                }
            }

            if (statusFiles.isNotEmpty())
                emit(StorageResponse.Success(statusList = statusFiles))
            else
                emit(StorageResponse.Failure("No supported statuses found"))
        }.flowOn(Dispatchers.IO)
    }

    private fun hasRequiredPermissions(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                        context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }
            else -> {
                context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                        context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun downloadWhatsappStatus(
        statuses: List<File>,
        destinationPath: String
    ): Flow<StorageResponse> = flow {
        emit(StorageResponse.Loading)
        val downloadedStatus = mutableListOf<File>()

        try {
            statuses.forEach { status ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ : Use MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, status.name)
                        put(MediaStore.Downloads.MIME_TYPE, getMimeType(status))
                        put(MediaStore.Downloads.RELATIVE_PATH, "Download/Snatchy")
                    }

                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            FileInputStream(status).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        downloadedStatus.add(status)
                    }
                } else {
                    // Below Android 10: Direct file copy
                    val desDir = File(destinationPath)
                    if (!desDir.exists()) desDir.mkdirs()

                    val statusFile = File(desDir, status.name)
                    if (!statusFile.exists()) {
                        FileInputStream(status).use { input ->
                            statusFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        downloadedStatus.add(statusFile)
                    }
                }
            }

            if (downloadedStatus.isNotEmpty()) {
                emit(
                    StorageResponse.Success(
                        statusList = statuses,
                        message = "${downloadedStatus.size} statuses have been downloaded successfully."
                    )
                )
            } else {
                emit(StorageResponse.Failure("No statuses were downloaded."))
            }
        } catch (e: IOException) {
            Log.e("WhatsappStatusRepository", "Download failed: ${e.message}")
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