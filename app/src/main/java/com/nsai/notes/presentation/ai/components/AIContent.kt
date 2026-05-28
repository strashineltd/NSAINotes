package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nsai.notes.data.remote.search.SearchResult
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.presentation.theme.LocalAnimationConfig

@Composable
fun AIContent(
    showChat: Boolean,
    messages: List<ChatMessage>,
    isLoading: Boolean,
    searchResults: List<SearchResult>,
    generatedImage: String?,
    onUrlClick: (String) -> Unit,
    onSaveAsNote: (String) -> Unit,
    onRetry: () -> Unit,
    onSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val animConfig = LocalAnimationConfig.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Crossfade(
            targetState = showChat,
            animationSpec = tween(animConfig.normalDuration),
            label = "content"
        ) { isChat ->
            Box(
                modifier = Modifier.widthIn(max = 640.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (isChat) {
                    ChatContainer(
                        messages = messages,
                        isLoading = isLoading,
                        searchResults = searchResults,
                        generatedImage = generatedImage,
                        onUrlClick = onUrlClick,
                        onSaveAsNote = onSaveAsNote,
                        onRetry = onRetry
                    )
                } else {
                    WelcomeView(onSuggestion = onSuggestion)
                }
            }
        }
    }
}
