package com.nsai.notes.domain.rag

import com.nsai.notes.data.local.db.dao.NoteDao
import com.nsai.notes.data.local.embedding.EmbeddingEngine
import com.nsai.notes.data.local.embedding.TextChunker
import com.nsai.notes.data.local.vector.ChunkEntity
import com.nsai.notes.data.local.vector.VectorStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndexNotesUseCase @Inject constructor(
    private val noteDao: NoteDao,
    private val embeddingEngine: EmbeddingEngine,
    private val vectorStore: VectorStore
) {
    suspend fun indexNote(noteId: Long) {
        val note = noteDao.getNoteByIdAny(noteId) ?: return
        if (!embeddingEngine.isInitialized) return
        vectorStore.deleteByNoteId(noteId)
        val chunks = TextChunker.chunk(note.content, noteId)
        if (chunks.isEmpty()) return
        val entities = chunks.map { chunk ->
            val emb = embeddingEngine.embed(chunk.text)
            ChunkEntity(
                noteId = chunk.noteId,
                chunkIndex = chunk.index,
                content = chunk.text,
                embedding = VectorStore.toByteArray(emb)
            )
        }
        vectorStore.insertBatch(entities)
    }

    suspend fun removeNote(noteId: Long) {
        vectorStore.deleteByNoteId(noteId)
    }

    suspend fun rebuildAll() {
        vectorStore.deleteAll()
        val allNotes = noteDao.getAllNotesRaw()
        for (note in allNotes) {
            if (note.content.isNotBlank()) indexNote(note.id)
        }
    }
}
