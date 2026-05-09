package com.nsai.notes.data.local.vector

import com.nsai.notes.data.local.embedding.EmbeddingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorStoreTest {

    @Test
    fun `float array to byte array round-trip`() {
        val floats = FloatArray(384) { it.toFloat() }
        val bytes = VectorStore.toByteArray(floats)
        val restored = VectorStore.toFloatArray(bytes)
        for (i in floats.indices) {
            assertEquals(floats[i], restored[i], 0.001f)
        }
    }

    @Test
    fun `search returns empty for empty list`() {
        val results = VectorStore.bruteForceSearch(
            query = FloatArray(384) { 0.1f },
            candidates = emptyList(),
            topK = 5
        )
        assertEquals(0, results.size)
    }

    @Test
    fun `search returns sorted by similarity`() {
        val query = FloatArray(384) { 0.1f }
        val candidates = listOf(
            Pair("unrelated", FloatArray(384) { 0.9f }),
            Pair("matching", FloatArray(384) { 0.1f }),
            Pair("somewhat", FloatArray(384) { 0.3f })
        )
        val results = VectorStore.bruteForceSearch(query, candidates, topK = 2)
        assertEquals(2, results.size)
        assertEquals("matching", results.first().first)
        assertTrue(results[0].second > results[1].second)
    }

    @Test
    fun `minSimilarity filters results`() {
        val query = FloatArray(384) { 0.1f }
        val candidates = listOf(
            Pair("good", FloatArray(384) { 0.11f }),
            Pair("bad", FloatArray(384) { 0.9f })
        )
        val results = VectorStore.bruteForceSearch(query, candidates, topK = 5, minSimilarity = 0.5f)
        assertEquals(1, results.size)
        assertEquals("good", results[0].first)
    }
}
