package com.nsai.notes.presentation.ai.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nsai.notes.domain.model.AIMode

/**
 * 底部工作区栏 - 整合分段切换器、更多菜单和输入框
 */
@Composable
fun WorkspaceBar(
    currentTab: WorkspaceTab,
    onSelectTab: (WorkspaceTab) -> Unit,
    isImageMode: Boolean,
    inputText: String,
    imagePrompt: String,
    onTextChange: (String) -> Unit,
    onImagePromptChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    isWebSearchEnabled: Boolean,
    onToggleWebSearch: () -> Unit,
    contextLabel: String,
    onMoreClick: () -> Unit,
    onExitAI: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 分段切换器
        WorkspaceSegmentedSwitch(
            currentTab = currentTab,
            onSelectTab = onSelectTab
        )

        // 上下文标签 + 更多按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (contextLabel.isNotEmpty()) {
                Text(
                    text = contextLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
            TextButton(
                onClick = onMoreClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "更多",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // 退出按钮
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onExitAI,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "退出 AI 模式",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        // 输入框
        val text = if (isImageMode) imagePrompt else inputText
        val onInputChange: (String) -> Unit = if (isImageMode) onImagePromptChange else onTextChange
        val placeholder = when (currentTab) {
            WorkspaceTab.AGENT -> "输入任务指令..."
            WorkspaceTab.RAG -> "输入问题，从笔记中检索..."
            else -> if (isImageMode) "输入图片描述..." else "输入消息..."
        }

        ElevatedInputIsland(
            text = text,
            onTextChange = onInputChange,
            onSend = onSend,
            isLoading = isLoading,
            isWebSearchEnabled = isWebSearchEnabled,
            onToggleWebSearch = onToggleWebSearch,
            placeholder = placeholder
        )
    }
}
