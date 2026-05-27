package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nsai.notes.data.remote.search.SearchResult
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.model.Note
import com.nsai.notes.presentation.theme.LocalAnimationConfig

/**
 * AI主内容区 - 在欢迎页和聊天界面之间切换
 */
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
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAnimationConfig.current

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = showChat,
            animationSpec = tween(tokens.normalDuration),
            label = "ai_content"
        ) { isChatMode ->
            if (isChatMode) {
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
                WelcomeView(
                    onSuggestion = onSuggestion,
                    notes = notes,
                    onNoteClick = onNoteClick
                )
            }
        }
    }
}
