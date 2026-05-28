package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nsai.notes.data.remote.search.SearchResult
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.presentation.theme.LocalAnimationConfig

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
    val animConfig = LocalAnimationConfig.current
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisible ->
                if (lastVisible != null && lastVisible >= messages.size - 2) {
                    listState.animateScrollToItem(messages.size + 2)
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val lastAIMessageIndex = messages.indexOfLast { it.role == ChatMessage.Role.ASSISTANT }

        items(messages, key = { it.timestamp }) { message ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(animConfig.fastDuration)) + slideInVertically(
                    tween(animConfig.fastDuration),
                    initialOffsetY = { it / 4 }
                )
            ) {
                ChatBubble(
                    message = message,
                    isLastAIMessage = messages.indexOf(message) == lastAIMessageIndex,
                    onUrlClick = onUrlClick,
                    onSaveAsNote = onSaveAsNote,
                    onRetry = onRetry
                )
            }
        }

        if (generatedImage != null) {
            item("generated_image") {
                GeneratedImagePreview(generatedImage)
            }
        }

        if (searchResults.isNotEmpty()) {
            item("search_results") {
                SearchResultCard(
                    query = messages.lastOrNull { it.role == ChatMessage.Role.USER }?.content ?: "",
                    results = searchResults,
                    onResultClick = onUrlClick
                )
            }
        }

        if (isLoading) {
            item("thinking") {
                ThinkingIndicator()
            }
        }
    }
}
