package com.nsai.notes

import android.app.Application
import android.content.ComponentCallbacks2
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import com.nsai.notes.data.local.db.worker.DataCleanupWorker
import com.nsai.notes.data.local.db.worker.NSAIWorkers
import com.nsai.notes.data.local.db.worker.SearchHistoryWorker
import com.nsai.notes.performance.FluidityManager
import com.nsai.notes.performance.FrameMonitor
import com.nsai.notes.performance.ResourceManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@OptIn(ExperimentalCoilApi::class)
@HiltAndroidApp
class NSAIApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var resourceManager: ResourceManager

    @Inject
    lateinit var frameMonitor: FrameMonitor

    @Inject
    lateinit var fluidityManager: FluidityManager

    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(imageLoader)

        frameMonitor.setOnMetricsReport { metrics ->
            fluidityManager.onFrameMetrics(metrics)
        }

        registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                resourceManager.onTrimMemory(level)
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                    fluidityManager.notifyMemoryLow()
                }
            }

            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}

            override fun onLowMemory() {
                resourceManager.onLowMemory()
                fluidityManager.notifyMemoryLow()
            }
        })

        // Collect navigation events to enable animation budget downgrades
        appScope.launch {
            fluidityManager.navigationEvents.collect { /* Navigation events consumed by FluidityManager internally */ }
        }

        enqueueWorkers()
    }

    private fun enqueueWorkers() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<DataCleanupWorker>(
            NSAIWorkers.DEFAULT_CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS
        ).setConstraints(constraints).build()

        val historyRequest = PeriodicWorkRequestBuilder<SearchHistoryWorker>(
            NSAIWorkers.DEFAULT_HISTORY_CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).apply {
            enqueueUniquePeriodicWork(
                NSAIWorkers.DATA_CLEANUP,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
            enqueueUniquePeriodicWork(
                NSAIWorkers.SEARCH_HISTORY_CLEANUP,
                ExistingPeriodicWorkPolicy.KEEP,
                historyRequest
            )
        }
    }
}
