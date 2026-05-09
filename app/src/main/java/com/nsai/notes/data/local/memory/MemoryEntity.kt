package com.nsai.notes.data.local.memory

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val key: String,
    val content: String,
    val importance: Float = 0.5f,
    val embedding: ByteArray,
    @ColumnInfo(name = "source_conversation_id") val sourceConversationId: Long? = null,
    @ColumnInfo(name = "access_count") val accessCount: Int = 0,
    @ColumnInfo(name = "last_accessed_at") val lastAccessedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
