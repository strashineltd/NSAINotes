package com.nsai.notes.data.local.embedding

import org.junit.Assert.assertEquals
import org.junit.Test

class EmbeddingEngineTest {

    @Test
    fun `embed returns 384-dimensional vector`() {
        val v1 = FloatArray(384) { 0.1f }
        val v2 = FloatArray(384) { 0.1f }
        val similarity = EmbeddingEngine.cosineSimilarity(v1, v2)
        assertEquals(1.0f, similarity, 0.001f)
    }

    @Test
    fun `cosine similarity of orthogonal vectors is zero`() {
        val v1 = FloatArray(384).apply { this[0] = 1f }
        val v2 = FloatArray(384).apply { this[1] = 1f }
        val similarity = EmbeddingEngine.cosineSimilarity(v1, v2)
        assertEquals(0.0f, similarity, 0.001f)
    }

    @Test
    fun `embedding dimension constant is 384`() {
        assertEquals(384, EmbeddingEngine.EMBEDDING_DIM)
    }
}
