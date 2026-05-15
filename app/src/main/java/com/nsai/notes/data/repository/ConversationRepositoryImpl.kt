package com.nsai.notes.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nsai.notes.data.local.db.dao.ConversationDao
import com.nsai.notes.data.local.db.entity.ConversationEntity
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.model.Conversation
import com.nsai.notes.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val dao: ConversationDao,
    private val gson: Gson
) : ConversationRepository {

    private val type = object : TypeToken<List<ChatMessage>>() {}.type

    override fun getAll(): Flow<List<Conversation>> = dao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getById(id: Long): Conversation? =
        dao.getById(id)?.toDomain()

    override suspend fun save(conversation: Conversation): Long {
        val messagesJson = gson.toJson(conversation.messages)
        val entity = ConversationEntity(
            id = conversation.id,
            title = conversation.title,
            messagesJson = messagesJson,
            createdAt = conversation.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        return if (conversation.id == 0L) {
            dao.insert(entity)
        } else {
            val rowsUpdated = dao.update(entity)
            if (rowsUpdated > 0) conversation.id else dao.insert(entity.copy(id = 0))
        }
    }

    override suspend fun delete(id: Long) {
        dao.getById(id)?.let { dao.delete(it) }
    }

    private fun ConversationEntity.toDomain(): Conversation {
        val messages: List<ChatMessage> = try {
            gson.fromJson(messagesJson, type)
        } catch (_: Exception) {
            emptyList()
        }
        return Conversation(
            id = id, title = title, messages = messages,
            createdAt = createdAt, updatedAt = updatedAt
        )
    }
}
