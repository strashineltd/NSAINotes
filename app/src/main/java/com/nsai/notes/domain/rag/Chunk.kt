package com.nsai.notes.domain.rag

data class Chunk(
    val noteId: Long,
    val index: Int,
    val content: String,
    val embedding: FloatArray
)
