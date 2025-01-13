package com.berlin.snatchy.data

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * @author Abdallah Elsokkary
 */
class WhatsappStatusRepository {

    fun fetchWhatsAppStatuses(): List<File> {
        val statusDirectory = File(
            Environment.getExternalStorageDirectory(),
            "WhatsApp/Media/.Statuses"
        )

        if (statusDirectory.exists() && statusDirectory.isDirectory) {
            return statusDirectory.listFiles()?.filter { file ->
                file.isFile && isSupportedStatusFile(file)
            } ?: emptyList()
        }

        return emptyList()
    }

    private fun isSupportedStatusFile(file: File): Boolean {
        val supportedExtensions = listOf("jpg", "jpeg", "png", "mp4", "gif")
        return file.extension.lowercase() in supportedExtensions
    }


    fun downloadWhatsAppStatus(statuses: List<File>, destinationPath: String): Flow<Pair<File,Boolean>>{
        return flow {
            val destinationDirectory = File(destinationPath)
            if(!destinationDirectory.exists()) destinationDirectory.mkdirs()
            for (status in statuses ){
                val destinationFile = File(destinationDirectory, status.name)
                if(destinationFile.exists()) continue
                try {

                } catch ()
            }
        }.flowOn(Dispatchers.IO)
    }
}