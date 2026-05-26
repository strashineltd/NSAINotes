package com.nsai.notes.performance

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class DeviceTier { HIGH, MID, LOW }

data class MemorySnapshot(
    val usedMemoryMB: Int,
    val maxMemoryMB: Int,
    val memoryClassMB: Int,
    val isLowMemory: Boolean,
    val deviceTier: DeviceTier
)

@Singleton
class AppPerformanceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resourceManager: ResourceManager,
    private val fluidityManager: FluidityManager
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    val deviceTier: DeviceTier = detectDeviceTier()

    private val _memorySnapshot = MutableStateFlow(captureMemorySnapshot())
    val memorySnapshot: StateFlow<MemorySnapshot> = _memorySnapshot.asStateFlow()

    private fun detectDeviceTier(): DeviceTier {
        val memClass = activityManager.memoryClass
        val totalRamMB = runCatching {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.totalMem / (1024 * 1024)
        }.getOrDefault(memClass.toLong())

        return when {
            totalRamMB >= 6000 && memClass >= 256 -> DeviceTier.HIGH
            totalRamMB >= 3000 && memClass >= 128 -> DeviceTier.MID
            else -> DeviceTier.LOW
        }
    }

    fun onTrimMemory(level: Int) {
        resourceManager.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            fluidityManager.notifyMemoryLow()
        }
        refreshMemorySnapshot()
    }

    fun onLowMemory() {
        resourceManager.onLowMemory()
        fluidityManager.notifyMemoryLow()
        refreshMemorySnapshot()
    }

    fun startThermalMonitoring() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            powerManager.addThermalStatusListener {
                if (it >= PowerManager.THERMAL_STATUS_SEVERE) {
                    fluidityManager.notifyThermalThrottled()
                } else if (it == PowerManager.THERMAL_STATUS_NONE) {
                    fluidityManager.notifyMemoryRecovered()
                }
            }
        }
    }

    fun prepareForNavigation() {
        resourceManager.prepareForNavigation()
        refreshMemorySnapshot()
    }

    fun refreshMemorySnapshot() {
        _memorySnapshot.value = captureMemorySnapshot()
    }

    private fun captureMemorySnapshot(): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val max = runtime.maxMemory() / (1024 * 1024)

        return MemorySnapshot(
            usedMemoryMB = used.toInt(),
            maxMemoryMB = max.toInt(),
            memoryClassMB = activityManager.memoryClass,
            isLowMemory = resourceManager.memoryState == ResourceManager.MemoryState.LOW,
            deviceTier = deviceTier
        )
    }
}
