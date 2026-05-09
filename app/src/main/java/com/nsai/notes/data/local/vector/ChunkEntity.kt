package com.nsai.notes.data.local.vector

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chunks")
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "note_id")
    val noteId: Long,
    @ColumnInfo(name = "chunk_index")
    val chunkIndex: Int,
    val content: String,
    val embedding: ByteArray,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
