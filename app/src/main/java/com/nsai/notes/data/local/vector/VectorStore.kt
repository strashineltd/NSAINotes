package com.nsai.notes.data.local.vector

import com.nsai.notes.data.local.db.dao.ChunkDao
import com.nsai.notes.data.local.embedding.EmbeddingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VectorStore @Inject constructor(
    private val chunkDao: ChunkDao
) {
    suspend fun insert(chunk: ChunkEntity) = withContext(Dispatchers.IO) {
        chunkDao.insert(chunk)
    }

    suspend fun insertBatch(chunks: List<ChunkEntity>) = withContext(Dispatchers.IO) {
        chunkDao.insertBatch(chunks)
    }

    suspend fun deleteByNoteId(noteId: Long) = withContext(Dispatchers.IO) {
        chunkDao.deleteByNoteId(noteId)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        chunkDao.deleteAll()
    }

    suspend fun getAll(): List<ChunkEntity> = withContext(Dispatchers.IO) {
        chunkDao.getRecent(limit = MAX_SEARCH_CANDIDATES)
    }

    /**
     * Search chunks by cosine similarity against query embedding.
     * Uses brute-force search capped at MAX_SEARCH_CANDIDATES chunks to avoid OOM.
     */
    suspend fun search(
        queryEmbedding: FloatArray,
        topK: Int = 5,
        minSimilarity: Float = 0.6f
    ): List<Triple<Long, String, Float>> = withContext(Dispatchers.IO) {
        val all = chunkDao.getRecent(limit = MAX_SEARCH_CANDIDATES)
        val candidates = all.map { Pair("${it.noteId}:${it.content}", toFloatArray(it.embedding)) }
        bruteForceSearch(queryEmbedding, candidates, topK, minSimilarity).map { (key, score) ->
            val colonIndex = key.indexOf(':')
            val noteId = key.substring(0, colonIndex).toLong()
            val content = key.substring(colonIndex + 1)
            Triple(noteId, content, score)
        }
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) {
        chunkDao.count()
    }

    companion object {
        /** Maximum number of chunks loaded for search to control memory usage. */
        const val MAX_SEARCH_CANDIDATES = 2000

        fun bruteForceSearch(
            query: FloatArray,
            candidates: List<Pair<String, FloatArray>>,
            topK: Int,
            minSimilarity: Float = 0.6f
        ): List<Pair<String, Float>> {
            return candidates.map { (key, emb) ->
                key to EmbeddingEngine.cosineSimilarity(query, emb)
            }
            .filter { it.second >= minSimilarity }
            .sortedByDescending { it.second }
            .take(topK)
        }

        fun toByteArray(floats: FloatArray): ByteArray {
            val buffer = ByteBuffer.allocate(floats.size * 4)
            floats.forEach { buffer.putFloat(it) }
            return buffer.array()
        }

        fun toFloatArray(bytes: ByteArray): FloatArray {
            val buffer = ByteBuffer.wrap(bytes)
            val floats = FloatArray(bytes.size / 4)
            for (i in floats.indices) {
                floats[i] = buffer.float
            }
            return floats
        }
    }
}
