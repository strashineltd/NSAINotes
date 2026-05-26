package com.nsai.notes.performance

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class DeviceClass { LOW, MID, HIGH }

data class RenderConfig(
    val imageQuality: Float,        // 0.5-1.0
    val maxImageSize: Int,          // pixels
    val enableShadows: Boolean,
    val enableBlur: Boolean,
    val maxListItems: Int,          // preload limit
    val gifAutoplay: Boolean,
    val animationComplexity: Float   // 0.0-1.0
) {
    companion object {
        val HIGH = RenderConfig(1.0f, 2048, true, true, 50, true, 1.0f)
        val MID  = RenderConfig(0.8f, 1024, true, false, 30, false, 0.6f)
        val LOW  = RenderConfig(0.5f, 768, false, false, 15, false, 0.3f)
    }
}

@Singleton
class DeviceAdaptiveConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val deviceClass: DeviceClass
    val renderConfig: RenderConfig
    val memoryBudgetMB: Int

    init {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalRamMB = memInfo.totalMem / (1024 * 1024)
        val memClass = am.memoryClass

        deviceClass = when {
            totalRamMB >= 8000 && memClass >= 256 -> DeviceClass.HIGH
            totalRamMB >= 4000 && memClass >= 192 -> DeviceClass.MID
            else -> DeviceClass.LOW
        }

        renderConfig = when (deviceClass) {
            DeviceClass.HIGH -> RenderConfig.HIGH
            DeviceClass.MID  -> RenderConfig.MID
            DeviceClass.LOW  -> RenderConfig.LOW
        }

        memoryBudgetMB = when (deviceClass) {
            DeviceClass.HIGH -> 128
            DeviceClass.MID  -> 64
            DeviceClass.LOW  -> 32
        }
    }

    fun shouldSkipHeavyRendering(): Boolean = deviceClass == DeviceClass.LOW

    fun maxRecompositionBudget(): Int = when (deviceClass) {
        DeviceClass.HIGH -> 16
        DeviceClass.MID  -> 8
        DeviceClass.LOW  -> 4
    }
}
