package com.nsai.notes.domain.memory

enum class MemoryType { PREFERENCE, FACT, CONVERSATION, DATE, RELATIONSHIP }

data class Memory(
    val id: Long = 0,
    val type: MemoryType,
    val key: String,
    val content: String,
    val importance: Float = 0.5f,
    val embedding: FloatArray = FloatArray(0),
    val sourceConversationId: Long? = null,
    val accessCount: Int = 0,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
