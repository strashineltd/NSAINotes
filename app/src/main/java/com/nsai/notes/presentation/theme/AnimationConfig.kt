package com.nsai.notes.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import com.nsai.notes.performance.AnimationBudget

val StandardEasing: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
val DecelerateEasing: Easing = CubicBezierEasing(0.0f, 0f, 0.2f, 1f)
val AccelerateEasing: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

@Immutable
data class AnimationTokens(
    val normalDuration: Int,
    val fastDuration: Int,
    val slowDuration: Int,
    val springDamping: Float,
    val springStiffness: Float,
    val staggeredDelay: Int,
    val enableContentTransitions: Boolean
) {
    companion object {
        val FULL = AnimationTokens(
            normalDuration = 300, fastDuration = 150, slowDuration = 500,
            springDamping = 0.55f, springStiffness = 250f,
            staggeredDelay = 50, enableContentTransitions = true
        )
        val REDUCED = AnimationTokens(
            normalDuration = 180, fastDuration = 100, slowDuration = 300,
            springDamping = 0.7f, springStiffness = 400f,
            staggeredDelay = 0, enableContentTransitions = true
        )
        val MINIMAL = AnimationTokens(
            normalDuration = 0, fastDuration = 0, slowDuration = 0,
            springDamping = 0.9f, springStiffness = 800f,
            staggeredDelay = 0, enableContentTransitions = false
        )

        fun fromBudget(budget: AnimationBudget): AnimationTokens = when (budget) {
            AnimationBudget.FULL -> FULL
            AnimationBudget.REDUCED -> REDUCED
            AnimationBudget.MINIMAL -> MINIMAL
        }
    }
}

val LocalAnimationConfig = compositionLocalOf { AnimationTokens.FULL }
