package com.nsai.notes.presentation.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

@Immutable
data class HeroBounds(
    val left: Float = 0f,
    val top: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

sealed class HeroState {
    data object Idle : HeroState()
    data class Entering(val origin: HeroBounds) : HeroState()
    data class Exiting(val target: HeroBounds) : HeroState()
}

private val HeroEasing = CubicBezierEasing(0.33f, 0.0f, 0.67f, 1.0f)

@Composable
fun HeroOverlay(
    state: HeroState,
    screenWidthPx: Float,
    screenHeightPx: Float,
    animationSpeedMultiplier: Float,
    onAnimationEnd: () -> Unit
) {
    if (state is HeroState.Idle) return

    val density = LocalDensity.current
    val isEntering = state is HeroState.Entering
    val durationMs = (450 / animationSpeedMultiplier.coerceAtLeast(0.25f)).toInt()
    val densityInv = remember(density) { 1f / density.density }

    val anim = remember { Animatable(if (isEntering) 0f else 1f) }

    LaunchedEffect(state) {
        anim.animateTo(
            targetValue = if (isEntering) 1f else 0f,
            animationSpec = tween(durationMs, easing = HeroEasing)
        )
        onAnimationEnd()
    }

    val p = anim.value
    val origin = when (state) {
        is HeroState.Entering -> state.origin
        is HeroState.Exiting -> state.target
        else -> HeroBounds()
    }

    // --- Size & Position: center-anchored scale ---
    // Enter: 0%→50% scales to fullscreen, Exit: 100%→40% shrinks
    val expandP = if (isEntering)
        (p / 0.50f).coerceIn(0f, 1f)
    else
        (p / 0.60f).coerceIn(0f, 1f)

    val iconCenterX = origin.left + origin.width / 2f
    val iconCenterY = origin.top + origin.height / 2f
    val screenCenterX = screenWidthPx / 2f
    val screenCenterY = screenHeightPx / 2f

    val hPx = lerp(origin.height + 16f, screenHeightPx, expandP)
    val wPx = lerp(origin.width + 16f, screenWidthPx, expandP)
    val cx = lerp(iconCenterX, screenCenterX, expandP)
    val cy = lerp(iconCenterY, screenCenterY, expandP)
    val fullscreen = expandP > 0.99f
    val ox = if (fullscreen) 0 else (cx - wPx / 2f).roundToInt()
    val oy = if (fullscreen) 0 else (cy - hPx / 2f).roundToInt()
    val w = if (fullscreen) screenWidthPx * densityInv else wPx * densityInv
    val h = if (fullscreen) screenHeightPx * densityInv else hPx * densityInv

    // Corner: fully rounded at icon size (w/2) → sharp at fullscreen (0)
    val cornerRadius = lerp(w / 2f, 0f, expandP)

    // --- Overlay alpha ---
    val overlayAlpha = if (isEntering) {
        if (p < 0.50f) 1f else 1f - ((p - 0.50f) / 0.50f)
    } else {
        1f  // Exit: always opaque, no overlap with AIHome
    }

    // --- Content alpha: bell curve ---
    val contentAlpha = if (isEntering) {
        when {
            p < 0.10f -> 0f
            p < 0.50f -> (p - 0.10f) / 0.40f
            p < 0.85f -> 1f
            else -> 1f - ((p - 0.85f) / 0.15f)
        }
    } else {
        when {
            p > 0.90f -> (p - 0.90f) / 0.10f
            p > 0.75f -> 1f
            p > 0.60f -> 1f - ((0.75f - p) / 0.15f)
            else -> 0f
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .offset { IntOffset(ox, oy) }
                .size(w.dp, h.dp)
                .alpha(overlayAlpha)
                .clip(RoundedCornerShape(cornerRadius.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                Modifier.fillMaxSize().alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.AutoAwesome, null,
                    Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "AI 助手",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
