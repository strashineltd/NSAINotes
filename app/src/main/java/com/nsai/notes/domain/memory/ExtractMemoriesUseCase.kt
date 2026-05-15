package com.nsai.notes.domain.memory

import com.nsai.notes.data.local.embedding.EmbeddingEngine
import com.nsai.notes.data.local.memory.MemoryDao
import com.nsai.notes.data.local.memory.MemoryEntity
import com.nsai.notes.data.local.memory.MemoryExtractor
import com.nsai.notes.data.local.vector.VectorStore
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.repository.AIOptions
import com.nsai.notes.domain.repository.AIService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractMemoriesUseCase @Inject constructor(
    private val aiService: AIService,
    private val memoryDao: MemoryDao,
    private val embeddingEngine: EmbeddingEngine,
    private val memoryExtractor: MemoryExtractor
) {
    suspend fun extract(conversationText: String, provider: AIProvider) = withContext(Dispatchers.IO) {
        if (!embeddingEngine.isInitialized) return@withContext
        val prompt = memoryExtractor.buildExtractionPrompt(conversationText)
        val response = aiService.chat(
            provider = provider,
            messages = listOf(ChatMessage(ChatMessage.Role.USER, prompt)),
            options = AIOptions(temperature = 0.2f, maxTokens = 1024)
        )
        val memories = memoryExtractor.parseMemoriesJson(response.content)
        memories.forEach { memory ->
            val embedding = embeddingEngine.embed(memory.content)
            memoryDao.insert(MemoryEntity(
                type = memory.type.name, key = memory.key, content = memory.content,
                importance = memory.importance,
                embedding = VectorStore.toByteArray(embedding),
                sourceConversationId = memory.sourceConversationId
            ))
        }
    }
}
