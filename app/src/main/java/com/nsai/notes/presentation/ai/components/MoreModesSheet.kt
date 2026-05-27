package com.nsai.notes.presentation.ai.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 更多AI模式选择底部Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreModesSheet(
    onDismiss: () -> Unit,
    onSelectAgent: () -> Unit,
    onSelectRag: () -> Unit,
    onSelectImage: () -> Unit,
    onSelectDocGen: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            ListItem(
                headlineContent = { Text("Agent") },
                supportingContent = { Text("AI 多步推理，调用工具完成任务") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    onSelectAgent()
                }
            )
            ListItem(
                headlineContent = { Text("知识库搜索") },
                supportingContent = { Text("从你的笔记中检索相关内容辅助回答") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF10A37F)
                    )
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    onSelectRag()
                }
            )
            ListItem(
                headlineContent = { Text("图片生成") },
                supportingContent = { Text("输入描述，AI 生成图片") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color(0xFFE91E63)
                    )
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    onSelectImage()
                }
            )
            ListItem(
                headlineContent = { Text("文档生成") },
                supportingContent = { Text("AI 生成完整文档并自动保存为笔记") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF0070F3)
                    )
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    onSelectDocGen()
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
