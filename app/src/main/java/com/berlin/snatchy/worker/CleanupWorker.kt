package com.berlin.snatchy.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.berlin.snatchy.data.CACHE_DIR_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class CleanupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO){
            try {
                val dir = context.applicationContext.getExternalFilesDirs(null)
                    .filterNotNull()
                    .firstOrNull()
                    ?.let { File(it, CACHE_DIR_NAME) }

                if (dir == null) {
                    return@withContext Result.success()
                }
                val currentTIme = System.currentTimeMillis()
                val expirationTime = 1000 * 60 * 60 * 24 //24 hours

                var hasFailed = false
                dir.listFiles()?.forEach { file ->
                    val age = currentTIme - file.lastModified()
                    if (age > expirationTime) {
                        if (!file.delete()) {
                            hasFailed = true
                        }
                    }
                }
                if (hasFailed) {
                    return@withContext Result.retry()
                } else {
                    return@withContext Result.success()
                }
            } catch (_: SecurityException){
                return@withContext Result.retry()
            } catch (_: IOException){
                return@withContext Result.retry()

            } catch (_: Exception){
                return@withContext Result.retry()
            }
        }
    }
}