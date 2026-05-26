package com.nsai.notes.performance

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.Volatile

@Singleton
class FluidityManager @Inject constructor(
    private val frameMonitor: FrameMonitor,
    private val resourceManager: ResourceManager
) {
    @Volatile private var lastJankTime = 0L
    @Volatile private var consecutiveJankFrames = 0
    @Volatile private var smoothWindowCount = 0
    @Volatile private var animationBudget = AnimationBudget.FULL

    private val _config = MutableStateFlow(FluidityConfig())
    val config: StateFlow<FluidityConfig> = _config.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val navigationEvents = _navigationEvents.asSharedFlow()

    private var currentScreen: String = "notes"

    fun onScreenChange(screen: String) {
        if (screen != currentScreen) {
            _navigationEvents.tryEmit(screen)
            currentScreen = screen
            prepareForTransition()
        }
    }

    fun onFrameMetrics(metrics: FrameMetrics) {
        val now = SystemClock.elapsedRealtime()

        if (metrics.dropRate > 0.10f) {
            consecutiveJankFrames++
            smoothWindowCount = 0
            if (consecutiveJankFrames >= 1) { // Downgrade immediately on first jank detection
                downgradeAnimations(_config.value)
            }
        } else if (metrics.dropRate < 0.03f) {
            consecutiveJankFrames = 0
            smoothWindowCount++
            if (animationBudget < AnimationBudget.FULL && smoothWindowCount >= 3) {
                upgradeAnimations()
            }
        } else {
            // Intermediate drop rate: hold steady, only reset on real jank
            consecutiveJankFrames = maxOf(0, consecutiveJankFrames - 1)
        }

        if (metrics.dropRate > 0.10f) lastJankTime = now
    }

    private fun prepareForTransition() {
        if (resourceManager.memoryState == ResourceManager.MemoryState.LOW) {
            resourceManager.releaseMemory()
        }
    }

    fun notifyMemoryLow() {
        animationBudget = AnimationBudget.MINIMAL
        _config.value = _config.value.copy(
            animationBudget = AnimationBudget.MINIMAL,
            animationSpeedMultiplier = AnimationBudget.MINIMAL.speedMultiplier,
            skippableContent = true
        )
    }

    fun notifyMemoryRecovered() {
        if (animationBudget == AnimationBudget.MINIMAL) {
            animationBudget = AnimationBudget.REDUCED
            _config.value = _config.value.copy(
                animationBudget = AnimationBudget.REDUCED,
                animationSpeedMultiplier = AnimationBudget.REDUCED.speedMultiplier,
                skippableContent = false
            )
        }
    }

    fun notifyThermalThrottled() {
        animationBudget = AnimationBudget.MINIMAL
        _config.value = _config.value.copy(
            animationBudget = AnimationBudget.MINIMAL,
            animationSpeedMultiplier = AnimationBudget.MINIMAL.speedMultiplier,
            skippableContent = true
        )
    }

    private fun downgradeAnimations(current: FluidityConfig) {
        val newBudget = when (animationBudget) {
            AnimationBudget.FULL -> AnimationBudget.REDUCED
            AnimationBudget.REDUCED -> AnimationBudget.MINIMAL
            AnimationBudget.MINIMAL -> AnimationBudget.MINIMAL
        }
        if (newBudget != animationBudget) {
            animationBudget = newBudget
            _config.value = current.copy(
                animationBudget = newBudget,
                animationSpeedMultiplier = newBudget.speedMultiplier,
                shimmerEnabled = newBudget == AnimationBudget.FULL,
                blurEnabled = newBudget != AnimationBudget.MINIMAL,
                skippableContent = newBudget == AnimationBudget.MINIMAL
            )
        }
    }

    private fun upgradeAnimations() {
        val newBudget = when (animationBudget) {
            AnimationBudget.MINIMAL -> AnimationBudget.REDUCED
            AnimationBudget.REDUCED -> AnimationBudget.FULL
            AnimationBudget.FULL -> AnimationBudget.FULL
        }
        if (newBudget != animationBudget) {
            animationBudget = newBudget
            _config.value = _config.value.copy(
                animationBudget = newBudget,
                animationSpeedMultiplier = newBudget.speedMultiplier,
                shimmerEnabled = true,
                blurEnabled = true,
                skippableContent = false
            )
        }
    }

    val isJanky: Boolean get() = animationBudget <= AnimationBudget.REDUCED
    val animationSpeedMultiplier: Float get() = animationBudget.speedMultiplier
}

data class FluidityConfig(
    val animationBudget: AnimationBudget = AnimationBudget.FULL,
    val animationSpeedMultiplier: Float = 1.0f,
    val shimmerEnabled: Boolean = true,
    val blurEnabled: Boolean = true,
    val skippableContent: Boolean = false
)

enum class AnimationBudget(val speedMultiplier: Float) {
    FULL(1.0f),
    REDUCED(2.0f),
    MINIMAL(4.0f)
}
