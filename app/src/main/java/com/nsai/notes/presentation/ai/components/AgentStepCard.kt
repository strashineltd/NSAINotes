package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nsai.notes.presentation.theme.LocalAnimationConfig

@Composable
fun AgentStepCard(
    stepNumber: Int,
    thought: String,
    action: String?,
    observation: String?,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAnimationConfig.current
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Step $stepNumber", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(if (expanded) "收起" else "展开", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }

            AnimatedVisibility(expanded,
                enter = expandVertically(tween(tokens.fastDuration)) + fadeIn(tween(tokens.fastDuration)),
                exit = shrinkVertically(tween(tokens.fastDuration)) + fadeOut(tween(tokens.fastDuration))
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Thought
                    if (thought.isNotBlank()) {
                        StepRow(Icons.Default.Lightbulb, "思考", thought, Color(0xFF42A5F5))
                        Spacer(Modifier.height(6.dp))
                    }
                    // Action
                    if (action != null) {
                        StepRow(Icons.Default.PlayArrow, "执行", action, Color(0xFF66BB6A))
                        Spacer(Modifier.height(6.dp))
                    }
                    // Observation
                    if (observation != null) {
                        val isError = observation.startsWith("错误") || observation.startsWith("工具 '")
                        StepRow(
                            if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                            "结果", observation,
                            if (isError) Color(0xFFEF5350) else Color(0xFF8D6E63)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(icon: ImageVector, label: String, content: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(16.dp).padding(top = 2.dp), tint = color)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color,
                fontWeight = FontWeight.SemiBold)
            Text(content, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}
