package com.nsai.notes.domain.repository

import com.nsai.notes.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun getAll(): Flow<List<Conversation>>
    suspend fun getById(id: Long): Conversation?
    suspend fun save(conversation: Conversation): Long
    suspend fun delete(id: Long)
}
