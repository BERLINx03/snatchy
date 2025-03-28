package com.berlin.snatchy.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
                File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/.Statuses"),
                File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
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

    fun hasRequiredPermissions(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> { // API 34
                context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                        context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                        Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> { // API 33
                context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                        context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> { // API 30
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
    ): Flow<StorageResponse> = flow {
        emit(StorageResponse.Loading)
        val downloadedStatus = mutableListOf<File>()

        try {
            statuses.forEach { status ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, status.name)
                        put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(status))
                        put(MediaStore.MediaColumns.IS_PENDING, 1)

                        //directory based on file type
                        val isVideo = status.name.endsWith(".mp4", ignoreCase = true)
                        put(MediaStore.MediaColumns.RELATIVE_PATH,
                            if (isVideo) Environment.DIRECTORY_MOVIES + "/Snatchy"
                            else Environment.DIRECTORY_PICTURES + "/Snatchy"
                        )
                    }

                    // Choose correct MediaStore collection based on file type
                    val collection = when {
                        status.name.endsWith(".mp4", ignoreCase = true) -> {
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        }
                        else -> {
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }
                    }

                    val uri = context.contentResolver.insert(collection, contentValues)

                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            FileInputStream(status).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(it, contentValues, null, null)

                        downloadedStatus.add(status)
                        Log.d("WhatsappStatusRepository", "Saved file: ${status.name} to ${if(status.name.endsWith(".mp4", ignoreCase = true)) "Movies" else "Pictures"}")
                    }
                } else {
                    val isVideo = status.name.endsWith(".mp4", ignoreCase = true)
                    val desDir = File(Environment.getExternalStoragePublicDirectory(
                        if (isVideo) Environment.DIRECTORY_MOVIES
                        else Environment.DIRECTORY_PICTURES
                    ), "Snatchy")

                    if (!desDir.exists()) desDir.mkdirs()

                    val statusFile = File(desDir, status.name)
                    if (!statusFile.exists()) {
                        FileInputStream(status).use { input ->
                            statusFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        // Trigger media scan
                        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(statusFile)))
                        downloadedStatus.add(statusFile)
                    }
                }
            }

            if (downloadedStatus.isNotEmpty()) {
                emit(
                    StorageResponse.Success(
                        statusList = statuses,
                        message = "${downloadedStatus.size} statuses have been saved to gallery."
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