package com.nsai.notes.domain.repository

import com.nsai.notes.domain.model.AIMode
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage

data class AIOptions(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val stream: Boolean = false,
    val imageData: String? = null
)

data class AIResponse(
    val content: String,
    val model: String,
    val reasoning: String? = null,
    val usage: TokenUsage? = null
)

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

interface AIService {
    suspend fun chat(
        provider: AIProvider,
        messages: List<ChatMessage>,
        options: AIOptions = AIOptions(),
        mode: AIMode = AIMode.QUICK
    ): AIResponse

    suspend fun summarize(
        provider: AIProvider,
        text: String,
        options: AIOptions = AIOptions()
    ): String

    suspend fun generateImage(
        provider: AIProvider,
        prompt: String,
        options: AIOptions = AIOptions()
    ): AIResponse
}
