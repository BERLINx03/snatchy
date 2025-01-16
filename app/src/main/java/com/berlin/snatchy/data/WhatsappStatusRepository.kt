package com.berlin.snatchy.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.berlin.snatchy.domain.model.StorageResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * @author Abdallah Elsokkary
 */
class WhatsappStatusRepository(private val context: Context) {

    fun fetchWhatsappStatuses(): Flow<StorageResponse> {
        return flow {
            emit(StorageResponse.Loading)

            val statusDirectory = File(
                //wrong path for testing only. -> add /.Statuses for the actual functionality
                Environment.getExternalStorageDirectory(), "WhatsApp/Media/.Statuses"
            )
            if (statusDirectory.exists() && statusDirectory.isDirectory) {
                val statuses = statusDirectory.listFiles()?.filter { file ->
                    file.isFile && isSupportedStatusFile(file)
                } ?: emptyList()

                if (statuses.isNotEmpty())
                    emit(StorageResponse.Success(statusList = statuses))
                else
                    emit(StorageResponse.Failure("No supported statuses found"))
            } else {
                emit(StorageResponse.Failure("WhatsApp statuses folder not found"))
            }
        }.flowOn(Dispatchers.IO)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun downloadWhatsappStatus(
        statuses: List<File>,
        destinationPath: String
    ): Flow<StorageResponse> = flow {
        emit(StorageResponse.Loading)
        val desDir = File(destinationPath)
        if (!desDir.exists()) desDir.mkdirs()

        val downloadedStatus = mutableListOf<File>()
        statuses.forEach { status ->
            try {
                val statusFile = File(desDir, status.name)
                if (!statusFile.exists()) {
                    // Determine MIME type based on file extension
                    val mimeType = getMimeType(status)

                    // Save file using MediaStore for public directories
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, status.name)
                        put(MediaStore.Downloads.MIME_TYPE, mimeType)
                        put(MediaStore.Downloads.RELATIVE_PATH, "Download/Snatchy")
                    }

                    val uri: Uri? = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            FileInputStream(status).use { inputStream ->
                                copyStream(inputStream, outputStream)
                            }
                        }
                        downloadedStatus.add(statusFile)
                    } ?: throw IOException("Failed to create URI for file ${status.name}")
                }
            } catch (e: IOException) {
                Log.e("WhatsappStatusRepository", "Failed to download ${status.name}: ${e.message}")
                emit(StorageResponse.Failure("Failed to download ${status.name}: ${e.message}"))
                return@flow // Return early on failure
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
            emit(StorageResponse.Failure("No supported statuses were found."))
        }
    }.flowOn(Dispatchers.IO)

    private fun copyStream(inputStream: InputStream, outputStream: OutputStream) {
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp4", "mkv" -> "video/mp4"
            "pdf" -> "application/pdf"
            // Add more MIME types as needed
            else -> "application/octet-stream"
        }
    }

    private fun isSupportedStatusFile(file: File): Boolean {
        val supportedExtensions = listOf("jpg", "jpeg", "png", "mp4", "gif")
        return file.extension.lowercase() in supportedExtensions
    }
}