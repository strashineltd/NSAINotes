package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nsai.notes.data.remote.search.SearchResult
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.presentation.theme.LocalAnimationConfig

/**
 * 聊天内容容器 - 管理消息列表、加载状态、搜索结果和图片预览
 */
@Composable
fun ChatContainer(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    searchResults: List<SearchResult>,
    generatedImage: String?,
    onUrlClick: (String) -> Unit,
    onSaveAsNote: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAnimationConfig.current
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 消息列表
        items(
            items = messages,
            key = { it.timestamp }
        ) { msg ->
            val lastAiMsg = messages.lastOrNull { it.role == ChatMessage.Role.ASSISTANT }
            val isLastAiMsg = msg == lastAiMsg

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(tokens.normalDuration)) +
                        slideInVertically(tween(tokens.normalDuration)) { it / 4 }
            ) {
                ChatBubble(
                    message = msg,
                    isLastAIMessage = isLastAiMsg,
                    onUrlClick = onUrlClick,
                    onSaveAsNote = onSaveAsNote,
                    onRetry = onRetry
                )
            }
        }

        // 生成的图片
        generatedImage?.let { url ->
            item(key = "generated_image") {
                GeneratedImagePreview(imageUrl = url)
            }
        }

        // 搜索结果
        if (searchResults.isNotEmpty()) {
            item(key = "search_results") {
                val query = messages.lastOrNull { it.role == ChatMessage.Role.USER }?.content ?: ""
                SearchResultCard(
                    query = query,
                    results = searchResults,
                    onResultClick = onUrlClick
                )
            }
        }

        // 思考中指示器
        if (isLoading) {
            item(key = "thinking") {
                ThinkingIndicator()
            }
        }
    }
}
