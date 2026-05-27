package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nsai.notes.presentation.theme.LocalAnimationConfig

enum class WorkspaceTab(val label: String, val icon: ImageVector) {
    CHAT("对话", Icons.AutoMirrored.Filled.Chat),
    AGENT("Agent", Icons.Default.AutoAwesome),
    RAG("知识库", Icons.AutoMirrored.Filled.LibraryBooks)
}

@Composable
fun WorkspaceSegmentedSwitch(
    currentTab: WorkspaceTab,
    onSelectTab: (WorkspaceTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAnimationConfig.current

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        WorkspaceTab.entries.forEachIndexed { index, tab ->
            val selected = currentTab == tab

            SegmentedButton(
                selected = selected,
                onClick = { onSelectTab(tab) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = WorkspaceTab.entries.size
                ),
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.primary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}