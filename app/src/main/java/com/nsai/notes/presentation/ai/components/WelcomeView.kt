package com.nsai.notes.presentation.ai.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nsai.notes.domain.model.Note

private val suggestions = listOf(
    Suggestion(Icons.Default.AutoAwesome, "创意灵感", "给我一些创意写作的灵感", listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))),
    Suggestion(Icons.Default.AutoAwesome, "知识问答", "解释一下量子计算的基本原理", listOf(Color(0xFF4ECDC4), Color(0xFF44B09E))),
    Suggestion(Icons.Default.AutoAwesome, "代码助手", "用Kotlin写一个快速排序算法", listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE))),
    Suggestion(Icons.Default.AutoAwesome, "翻译专家", "把这段文字翻译成英文", listOf(Color(0xFF0984E3), Color(0xFF74B9FF))),
    Suggestion(Icons.Default.AutoAwesome, "文档生成", "帮我写一份项目周报模板", listOf(Color(0xFF00B894), Color(0xFF55EFC4))),
    Suggestion(Icons.Default.AutoAwesome, "内容润色", "帮我润色这段文字使其更流畅", listOf(Color(0xFFFD79A8), Color(0xFFFDCB6E)))
)

/**
 * AI欢迎页面 - 简洁专业风
 * 包含问候语、快捷建议卡片和最近笔记列表
 */
@Composable
fun WelcomeView(
    onSuggestion: (String) -> Unit,
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 问候语
        item {
            Text(
                text = "你好，今天需要什么帮助？",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(16.dp))
        }

        // 建议卡片网格
        item {
            SuggestionGrid(
                suggestions = suggestions,
                onSuggestionClick = onSuggestion
            )
        }

        // 最近笔记
        if (notes.isNotEmpty()) {
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }
            item {
                Text(
                    text = "最近的笔记",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
            }
            items(notes, key = { it.id }) { note ->
                NoteItem(
                    note = note,
                    onClick = { onNoteClick(note.id) }
                )
            }
        }
    }
}

/**
 * 单个笔记项 - 简洁列表样式
 */
@Composable
private fun NoteItem(
    note: Note,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.padding(end = 12.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title.ifBlank { "无标题" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = note.content.take(60).replace("\n", " "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
