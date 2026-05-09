package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@Composable
fun AIThinkingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thinking")

    val iconScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "iconPulse"
    )

    val dot1Scale by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(400, delayMillis = 0), RepeatMode.Reverse
        ), label = "dot1"
    )
    val dot2Scale by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(400, delayMillis = 150), RepeatMode.Reverse
        ), label = "dot2"
    )
    val dot3Scale by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(400, delayMillis = 300), RepeatMode.Reverse
        ), label = "dot3"
    )

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).scale(iconScale).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.width(10.dp))

        Box(Modifier.size(6.dp).scale(dot1Scale).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)))
        Spacer(Modifier.width(5.dp))
        Box(Modifier.size(6.dp).scale(dot2Scale).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)))
        Spacer(Modifier.width(5.dp))
        Box(Modifier.size(6.dp).scale(dot3Scale).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)))
    }
}
