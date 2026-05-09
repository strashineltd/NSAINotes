package com.nsai.notes.data.repository

import com.nsai.notes.data.remote.ai.AIProviderAdapter
import com.nsai.notes.domain.model.AIMode
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.repository.AIOptions
import com.nsai.notes.domain.repository.AIResponse
import com.nsai.notes.domain.repository.AIService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIServiceImpl @Inject constructor(
    private val adapters: Set<@JvmSuppressWildcards AIProviderAdapter>
) : AIService {

    private val adapterMap: Map<AIProvider, AIProviderAdapter> by lazy {
        adapters.associateBy { it.provider }
    }

    override suspend fun chat(
        provider: AIProvider, messages: List<ChatMessage>, options: AIOptions, mode: AIMode
    ): AIResponse {
        return adapterMap[provider]?.chat(messages, options, mode) ?:
            throw IllegalArgumentException("不支持的AI Provider: $provider")
    }

    override suspend fun summarize(provider: AIProvider, text: String, options: AIOptions): String {
        return adapterMap[provider]?.summarize(text, options) ?:
            throw IllegalArgumentException("不支持的AI Provider: $provider")
    }

    override suspend fun generateImage(provider: AIProvider, prompt: String, options: AIOptions): AIResponse {
        return adapterMap[provider]?.generateImage(prompt, options) ?:
            throw IllegalArgumentException("不支持的AI Provider: $provider")
    }
}
