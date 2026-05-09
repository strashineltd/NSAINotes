package com.nsai.notes.presentation.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import com.nsai.notes.performance.AnimationBudget

@Stable
data class AnimationTokens(
    val normalDuration: Int,
    val fastDuration: Int,
    val springDamping: Float,
    val springStiffness: Float,
    val staggeredDelay: Int,
    val enableContentTransitions: Boolean
) {
    companion object {
        val FULL = AnimationTokens(
            normalDuration = 300, fastDuration = 150,
            springDamping = 0.55f, springStiffness = 250f,
            staggeredDelay = 50, enableContentTransitions = true
        )
        val REDUCED = AnimationTokens(
            normalDuration = 180, fastDuration = 100,
            springDamping = 0.7f, springStiffness = 400f,
            staggeredDelay = 0, enableContentTransitions = true
        )
        val MINIMAL = AnimationTokens(
            normalDuration = 0, fastDuration = 0,
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
