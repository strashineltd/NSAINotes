package com.nsai.notes.performance

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FluidityManager @Inject constructor(
    private val frameMonitor: FrameMonitor,
    private val resourceManager: ResourceManager
) {
    private var lastJankTime = 0L
    private var consecutiveJankFrames = 0
    private var animationBudget = AnimationBudget.FULL

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

        // Require >10% drop rate (30/300 frames) to count as a janky sample,
        // up from the original 1% threshold (>3/300) which was too aggressive.
        if (metrics.droppedFrames > 30) {
            consecutiveJankFrames++
            // Require 4 consecutive janky samples (~20 seconds) before downgrading
            // animations, up from the original 2 samples (~10 seconds).
            if (consecutiveJankFrames >= 4) {
                downgradeAnimations(_config.value)
            }
        } else {
            consecutiveJankFrames = 0
            // Require 8 seconds of sustained smooth frames before upgrading back,
            // up from the original 2 seconds, to prevent rapid oscillation.
            if (animationBudget < AnimationBudget.FULL &&
                now - lastJankTime > 8000L
            ) {
                upgradeAnimations()
            }
        }

        // Record when the last janky sample occurred (mirrors the >30 threshold above)
        if (metrics.droppedFrames > 30) {
            lastJankTime = now
        }
    }

    private fun prepareForTransition() {
        if (resourceManager.memoryState == ResourceManager.MemoryState.LOW) {
            resourceManager.releaseMemory()
        }
    }

    fun notifyMemoryLow() {
        downgradeAnimations(
            _config.value.copy(
                animationBudget = AnimationBudget.MINIMAL,
                skippableContent = true
            )
        )
    }

    fun notifyMemoryRecovered() {
        if (animationBudget < AnimationBudget.REDUCED) {
            upgradeAnimations()
        }
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
