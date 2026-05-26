package com.nsai.notes.domain.model

import androidx.compose.runtime.Stable

@Stable
data class Conversation(
    val id: Long = 0,
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
