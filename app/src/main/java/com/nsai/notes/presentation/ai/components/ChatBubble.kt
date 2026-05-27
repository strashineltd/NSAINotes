package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.presentation.ai.buildUrlAnnotatedText
import com.nsai.notes.presentation.ai.getUrlAt
import com.nsai.notes.presentation.theme.LocalAnimationConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── AI 简洁专业风配色 ───
private val UserBubbleColor = Color(0xFF10A37F)
private val UserTextColor = Color.White
private val AIBgColor = Color.White
private val AIBorderColor = Color(0xFFE5E5E5)
private val AITextPrimary = Color(0xFF1A1A1A)

/**
 * 聊天消息气泡 - 简洁专业风格
 * AI消息：白色背景 + 左侧青绿标识线
 * 用户消息：深青绿背景 + 白色文字
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    isLastAIMessage: Boolean = false,
    onUrlClick: (String) -> Unit = {},
    onSaveAsNote: (String) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val tokens = LocalAnimationConfig.current
    val isUser = message.role == ChatMessage.Role.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    // 打字机效果
    val initialChars = if (isUser || !isLastAIMessage) message.content.length else 1
    var displayedChars by remember(message.content) { mutableStateOf(initialChars) }
    if (!isUser && isLastAIMessage) {
        LaunchedEffect(message.content) {
            val targetLength = message.content.length
            val charsPerSecond = 120
            var lastFrameTime = 0L
            while (displayedChars < targetLength) {
                val frameTime = withFrameNanos { it }
                if (lastFrameTime > 0) {
                    val deltaSecs = (frameTime - lastFrameTime) / 1_000_000_000f
                    val charsToAdd = (deltaSecs * charsPerSecond).toInt().coerceAtLeast(1)
                    displayedChars = (displayedChars + charsToAdd).coerceAtMost(targetLength)
                }
                lastFrameTime = frameTime
            }
        }
    }
    val typewriterDone = displayedChars >= message.content.length

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        // AI 头像 + 名称 + 时间
        if (!isUser) {
            Row(
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "AI 助手",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }

        // 消息气泡
        if (isUser) {
            // 用户消息 - 深青绿背景，白色文字
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                    .background(UserBubbleColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val urlText = remember(message.content) { buildUrlAnnotatedText(message.content) }
                androidx.compose.foundation.text.ClickableText(
                    text = urlText,
                    style = MaterialTheme.typography.bodyMedium.copy(color = UserTextColor),
                    onClick = { offset -> getUrlAt(message.content, offset)?.let { onUrlClick(it) } }
                )
            }
        } else {
            // AI消息 - 白色背景 + 左侧青绿竖线
            Row(
                modifier = Modifier
                    .padding(start = 8.dp, end = 48.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                // 左侧标识线
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                )
                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // 思考过程
                    message.reasoningContent?.let { reasoning ->
                        var expanded by remember { mutableStateOf(false) }
                        Row(
                            Modifier
                                .clickable { expanded = !expanded }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "思考过程",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (expanded) "收起" else "展开",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically(tween(tokens.fastDuration)) + fadeIn(tween(tokens.fastDuration)),
                            exit = shrinkVertically(tween(tokens.fastDuration)) + fadeOut(tween(tokens.fastDuration))
                        ) {
                            Text(
                                text = reasoning,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // 消息内容
                    if (typewriterDone) {
                        val urlText = remember(message.content) { buildUrlAnnotatedText(message.content) }
                        androidx.compose.foundation.text.ClickableText(
                            text = urlText,
                            style = MaterialTheme.typography.bodyMedium.copy(color = AITextPrimary),
                            modifier = Modifier.padding(vertical = 8.dp),
                            onClick = { offset -> getUrlAt(message.content, offset)?.let { onUrlClick(it) } }
                        )
                    } else {
                        Text(
                            text = message.content.take(displayedChars),
                            style = MaterialTheme.typography.bodyMedium.copy(color = AITextPrimary),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 回复操作（仅AI消息）
        if (!isUser && message.content.isNotBlank()) {
            AIReplyActions(
                content = message.content,
                onSaveAsNote = { onSaveAsNote(message.content) },
                onRetry = onRetry,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}
