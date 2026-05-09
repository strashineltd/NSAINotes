package com.nsai.notes.presentation.theme

import com.nsai.notes.performance.AnimationBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationTokensTest {

    @Test
    fun `FULL budget maps to full animation values`() {
        val tokens = AnimationTokens.fromBudget(AnimationBudget.FULL)
        assertEquals(300, tokens.normalDuration)
        assertEquals(150, tokens.fastDuration)
        assertTrue(tokens.enableContentTransitions)
        assertEquals(50, tokens.staggeredDelay)
    }

    @Test
    fun `REDUCED budget cuts durations and disables stagger`() {
        val tokens = AnimationTokens.fromBudget(AnimationBudget.REDUCED)
        assertEquals(180, tokens.normalDuration)
        assertEquals(100, tokens.fastDuration)
        assertEquals(0, tokens.staggeredDelay)
        assertTrue(tokens.enableContentTransitions)
    }

    @Test
    fun `MINIMAL budget disables animations`() {
        val tokens = AnimationTokens.fromBudget(AnimationBudget.MINIMAL)
        assertEquals(0, tokens.normalDuration)
        assertEquals(0, tokens.fastDuration)
        assertEquals(0, tokens.staggeredDelay)
        assertTrue(!tokens.enableContentTransitions)
    }

    @Test
    fun `spring stiffness increases as budget decreases`() {
        val full = AnimationTokens.fromBudget(AnimationBudget.FULL)
        val reduced = AnimationTokens.fromBudget(AnimationBudget.REDUCED)
        val minimal = AnimationTokens.fromBudget(AnimationBudget.MINIMAL)
        assertTrue(full.springStiffness <= reduced.springStiffness)
        assertTrue(reduced.springStiffness <= minimal.springStiffness)
    }

    @Test
    fun `AnimationTokens is Stable data class`() {
        val tokens1 = AnimationTokens.FULL
        val tokens2 = AnimationTokens.FULL
        assertEquals(tokens1, tokens2)
        assertEquals(tokens1.hashCode(), tokens2.hashCode())
    }
}
