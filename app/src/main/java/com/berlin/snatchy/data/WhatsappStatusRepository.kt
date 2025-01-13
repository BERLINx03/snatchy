package com.berlin.snatchy.data

import android.os.Environment
import android.util.Log
import com.berlin.snatchy.domain.model.StorageResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException

/**
 * @author Abdallah Elsokkary
 */
class WhatsappStatusRepository {

    fun fetchWhatsappStatuses(): Flow<StorageResponse> {
        return flow {
            emit(StorageResponse.Loading)

            val statusDirectory = File(
                Environment.getExternalStorageDirectory(), "WhatsApp/Media"
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

    fun downloadWhatsappStatus(
        statuses: List<File>,
        destinationPath: String
    ): Flow<StorageResponse> {
        return flow {
            emit(StorageResponse.Loading)

            val desDir = File(destinationPath)
            if (!desDir.exists()) desDir.mkdir()

            val downloadedStatus = mutableListOf<File>()
            statuses.forEach { status ->
                try {
                    val statusFile = File(desDir, status.name)
                    if (!statusFile.exists()) {
                        status.copyTo(statusFile, overwrite = false)
                        downloadedStatus.add(statusFile)
                    }
                } catch (e: IOException) {
                    Log.e("WhatsappStatusRepository", "Failed to download ${status.name}: ${e.message}")
                    emit(StorageResponse.Failure("Failed to download ${status.name}: ${e.message}"))
                }
            }

            if (downloadedStatus.isNotEmpty())
                emit(
                    StorageResponse.Success(
                        statusList = statuses,
                        message = "${downloadedStatus.size} statuses have been downloaded successfully."
                    )
                )
            else
                emit(StorageResponse.Failure("No supported statuses were found."))
        }.flowOn(Dispatchers.IO)
    }


    private fun isSupportedStatusFile(file: File): Boolean {
        val supportedExtensions = listOf("jpg", "jpeg", "png", "mp4", "gif")
        return file.extension.lowercase() in supportedExtensions
    }
}