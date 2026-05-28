package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.presentation.ai.buildUrlAnnotatedText
import com.nsai.notes.presentation.ai.getUrlAt
import com.nsai.notes.presentation.theme.AnimationTokens
import com.nsai.notes.presentation.theme.LocalAnimationConfig

@Composable
fun ChatBubble(
    message: ChatMessage,
    isLastAIMessage: Boolean = false,
    onUrlClick: (String) -> Unit = {},
    onSaveAsNote: (String) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val animConfig = LocalAnimationConfig.current
    val isUser = message.role == ChatMessage.Role.USER

    if (isUser) {
        UserMessage(message)
    } else {
        AIMessage(
            message = message,
            isLastAIMessage = isLastAIMessage,
            onUrlClick = onUrlClick,
            onSaveAsNote = onSaveAsNote,
            animConfig = animConfig
        )
    }
}

@Composable
private fun UserMessage(message: ChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

@Composable
private fun AIMessage(
    message: ChatMessage,
    isLastAIMessage: Boolean,
    onUrlClick: (String) -> Unit,
    onSaveAsNote: (String) -> Unit,
    animConfig: AnimationTokens
) {
    var visibleChars by remember(message.content) { mutableStateOf(if (!isLastAIMessage) message.content.length else 0) }
    var typewriterDone by remember(message.content) { mutableStateOf(!isLastAIMessage) }

    if (isLastAIMessage && !typewriterDone) {
        LaunchedEffect(message.content) {
            val totalChars = message.content.length
            val nanosPerChar = 1_000_000_000L / 120
            var lastTime = 0L
            var charIndex = 0
            while (charIndex < totalChars) {
                withFrameNanos { frameTime ->
                    if (lastTime == 0L) lastTime = frameTime
                    val elapsed = frameTime - lastTime
                    val charsToAdd = (elapsed / nanosPerChar).toInt()
                    if (charsToAdd > 0) {
                        charIndex = (charIndex + charsToAdd).coerceAtMost(totalChars)
                        visibleChars = charIndex
                        lastTime = frameTime
                    }
                }
            }
            typewriterDone = true
        }
    }

    val displayText = if (typewriterDone) message.content else message.content.take(visibleChars)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(0.9f).height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(1.dp)
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!message.reasoningContent.isNullOrBlank()) {
                    ReasoningBlock(message.reasoningContent, animConfig)
                    Spacer(Modifier.height(8.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp)
                ) {
                    if (typewriterDone) {
                        val annotated = remember(displayText) { buildUrlAnnotatedText(displayText) }
                        ClickableText(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.7f
                            ),
                            onClick = { offset ->
                                getUrlAt(displayText, offset)?.let(onUrlClick)
                            }
                        )
                    } else {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.7f
                        )
                    }
                }

                if (typewriterDone) {
                    Spacer(Modifier.height(4.dp))
                    AIReplyActions(message.content, onSaveAsNote)
                }
            }
        }
    }
}

@Composable
private fun ReasoningBlock(
    reasoning: String,
    animConfig: AnimationTokens
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.height(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (expanded) "收起思考过程" else "查看思考过程",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(animConfig.normalDuration)) + expandVertically(tween(animConfig.normalDuration)),
            exit = fadeOut(tween(animConfig.fastDuration)) + shrinkVertically(tween(animConfig.fastDuration))
        ) {
            Text(
                text = reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun AIReplyActions(content: String, onSaveAsNote: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = { /* copy to clipboard handled by parent */ }) {
            Text("复制", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { onSaveAsNote(content) }) {
            Text("插入笔记", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
