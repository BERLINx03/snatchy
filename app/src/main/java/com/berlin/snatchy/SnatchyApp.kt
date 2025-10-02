package com.berlin.snatchy

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.berlin.snatchy.worker.CleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

/**
 * @author Abdallah Elsokkary
 */
private const val CACHE_CLEANUP = "cache_cleanup"
@HiltAndroidApp
class SnatchyApp: Application(){
    override fun onCreate() {
        super.onCreate()
        scheduleCleanup()
    }

    private fun scheduleCleanup() {
        val cleanupReq = PeriodicWorkRequestBuilder<CleanupWorker>(
            1, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            CACHE_CLEANUP,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupReq
        )
    }
}