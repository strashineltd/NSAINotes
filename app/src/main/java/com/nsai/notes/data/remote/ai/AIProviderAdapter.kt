package com.nsai.notes.data.remote.ai

import com.nsai.notes.domain.model.AIMode
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.repository.AIOptions
import com.nsai.notes.domain.repository.AIResponse

interface AIProviderAdapter {
    val provider: AIProvider

    suspend fun chat(
        messages: List<ChatMessage>,
        options: AIOptions = AIOptions(),
        mode: AIMode = AIMode.QUICK
    ): AIResponse

    suspend fun summarize(
        text: String,
        options: AIOptions = AIOptions()
    ): String

    suspend fun generateImage(
        prompt: String,
        options: AIOptions = AIOptions()
    ): AIResponse
}
