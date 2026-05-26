package com.nsai.notes.domain.rag

import com.nsai.notes.data.local.db.dao.NoteDao
import com.nsai.notes.data.local.embedding.EmbeddingEngine
import com.nsai.notes.data.local.vector.VectorStore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyList
import com.nsai.notes.data.local.db.entity.NoteEntity

class IndexNotesUseCaseTest {

    @Test
    fun `indexNote deletes old chunks and inserts new ones`() = runTest {
        val noteDao = mock(NoteDao::class.java)
        val embeddingEngine = mock(EmbeddingEngine::class.java)
        val vectorStore = mock(VectorStore::class.java)

        val note = NoteEntity(id = 1L, title = "Test", content = "Sample note content for indexing")
        `when`(noteDao.getNoteByIdAny(anyLong())).thenReturn(note)
        `when`(embeddingEngine.isInitialized).thenReturn(true)
        `when`(embeddingEngine.embed(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(FloatArray(384) { 0.1f })

        val useCase = IndexNotesUseCase(noteDao, embeddingEngine, vectorStore)
        useCase.indexNote(1L)

        verify(vectorStore).deleteByNoteId(1L)
        verify(vectorStore).insertBatch(anyList())
    }
}
