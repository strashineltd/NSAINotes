package com.nsai.notes.domain.rag

import com.nsai.notes.data.local.embedding.EmbeddingEngine
import com.nsai.notes.data.local.vector.VectorStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyFloat
import org.mockito.ArgumentMatchers.anyInt

class RetrieveContextUseCaseTest {

    @Test
    fun `retrieve returns concatenated context`() = runTest {
        val embeddingEngine = mock(EmbeddingEngine::class.java)
        val vectorStore = mock(VectorStore::class.java)
        `when`(embeddingEngine.isInitialized).thenReturn(true)
        `when`(embeddingEngine.embed("test query")).thenReturn(FloatArray(384) { 0.1f })
        `when`(vectorStore.search(org.mockito.ArgumentMatchers.any(), anyInt(), anyFloat()))
            .thenReturn(listOf(
                Triple(1L, "relevant chunk 1", 0.85f),
                Triple(2L, "relevant chunk 2", 0.72f)
            ))

        val useCase = RetrieveContextUseCase(embeddingEngine, vectorStore)
        val context = useCase.retrieve("test query", topK = 3)

        assertEquals(true, context.contains("relevant chunk 1"))
        assertEquals(true, context.contains("relevant chunk 2"))
    }

    @Test
    fun `retrieve returns empty when engine not initialized`() = runTest {
        val embeddingEngine = mock(EmbeddingEngine::class.java)
        val vectorStore = mock(VectorStore::class.java)
        `when`(embeddingEngine.isInitialized).thenReturn(false)

        val useCase = RetrieveContextUseCase(embeddingEngine, vectorStore)
        val context = useCase.retrieve("test query")
        assertEquals("", context)
    }
}
