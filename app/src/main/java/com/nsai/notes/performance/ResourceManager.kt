package com.nsai.notes.performance

import android.content.ComponentCallbacks2
import android.content.Context
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoilApi::class)
@Singleton
class ResourceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: dagger.Lazy<ImageLoader>,
    private val deviceConfig: DeviceAdaptiveConfig
) {
    private var isLowMemory = false
    private var trimCount = 0

    val memoryState: MemoryState
        get() = when {
            isLowMemory -> MemoryState.LOW
            else -> MemoryState.NORMAL
        }

    fun onTrimMemory(level: Int) {
        trimCount++
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                clearMemoryCache()
                clearDiskCache()
                isLowMemory = true
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                clearMemoryCache()
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                clearMemoryCache()
                trimApplicationMemory()
            }
            else -> {}
        }
    }

    fun onLowMemory() {
        isLowMemory = true
        clearMemoryCache()
        clearDiskCache()
        trimApplicationMemory()
    }

    fun releaseMemory() {
        clearMemoryCache()
        isLowMemory = false
    }

    fun prepareForNavigation() {
        if (isLowMemory) {
            clearMemoryCache()
            clearDiskCache()
            isLowMemory = false
        }
        // Always suggest GC on navigation to reduce memory pressure
        System.gc()
    }

    fun prepareForHeavyScreen() {
        if (isLowMemory || deviceConfig.deviceClass == DeviceClass.LOW) {
            clearMemoryCache()
        }
        System.gc()
    }

    fun getMaxCacheSize(): Int = deviceConfig.memoryBudgetMB

    private fun clearMemoryCache() {
        imageLoader.get().memoryCache?.clear()
    }

    private fun clearDiskCache() {
        imageLoader.get().diskCache?.clear()
    }

    private fun trimApplicationMemory() {
        try {
            Runtime.getRuntime().gc()
        } catch (_: Exception) {}
    }

    val trimHistory: Int get() = trimCount

    enum class MemoryState { NORMAL, LOW }
}
