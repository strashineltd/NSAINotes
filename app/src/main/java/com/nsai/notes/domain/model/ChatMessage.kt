package com.nsai.notes.domain.model

import androidx.compose.runtime.Stable

@Stable
data class ChatMessage(
    val role: Role,
    val content: String,
    val reasoningContent: String? = null,
    val isThinking: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Role { SYSTEM, USER, ASSISTANT }
}
