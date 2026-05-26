package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nsai.notes.presentation.theme.LocalAnimationConfig

enum class WorkspaceTab(val label: String, val icon: ImageVector) {
    CHAT("对话", Icons.Default.Chat),
    AGENT("Agent", Icons.Default.AutoAwesome),
    RAG("知识库", Icons.Default.LibraryBooks)
}

@Composable
fun WorkspaceTabs(
    currentTab: WorkspaceTab,
    onSelectTab: (WorkspaceTab) -> Unit
) {
    val tokens = LocalAnimationConfig.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WorkspaceTab.entries.forEach { tab ->
            val selected = currentTab == tab
            val bgColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                animationSpec = tween(tokens.fastDuration), label = "tabBg"
            )
            val contentColor = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            Card(
                onClick = { onSelectTab(tab) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(tab.icon, null, Modifier.size(16.dp), tint = contentColor)
                    Spacer(Modifier.width(6.dp))
                    Text(tab.label, style = MaterialTheme.typography.labelLarge, color = contentColor)
                }
            }
        }
    }
}
