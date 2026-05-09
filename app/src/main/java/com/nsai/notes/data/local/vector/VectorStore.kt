package com.nsai.notes.data.local.vector

import android.content.ContentValues
import com.nsai.notes.data.local.db.AppDatabase
import com.nsai.notes.data.local.embedding.EmbeddingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VectorStore @Inject constructor(
    private val db: AppDatabase
) {

    suspend fun insert(chunk: ChunkEntity) = withContext(Dispatchers.IO) {
        db.openHelper.writableDatabase.insert("chunks", 0, chunk.toContentValues())
    }

    suspend fun insertBatch(chunks: List<ChunkEntity>) = withContext(Dispatchers.IO) {
        val writableDb = db.openHelper.writableDatabase
        writableDb.beginTransaction()
        try {
            chunks.forEach { writableDb.insert("chunks", 0, it.toContentValues()) }
            writableDb.setTransactionSuccessful()
        } finally {
            writableDb.endTransaction()
        }
    }

    suspend fun deleteByNoteId(noteId: Long) = withContext(Dispatchers.IO) {
        db.openHelper.writableDatabase.delete("chunks", "note_id = ?", arrayOf(noteId.toString()))
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        db.openHelper.writableDatabase.delete("chunks", null, null)
    }

    suspend fun getAll(): List<ChunkEntity> = withContext(Dispatchers.IO) {
        val cursor = db.openHelper.readableDatabase.query("SELECT * FROM chunks")
        val results = mutableListOf<ChunkEntity>()
        try {
            while (cursor.moveToNext()) {
                results.add(ChunkEntity(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    noteId = cursor.getLong(cursor.getColumnIndexOrThrow("note_id")),
                    chunkIndex = cursor.getInt(cursor.getColumnIndexOrThrow("chunk_index")),
                    content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                    embedding = cursor.getBlob(cursor.getColumnIndexOrThrow("embedding")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                ))
            }
        } finally {
            cursor.close()
        }
        results
    }

    suspend fun search(queryEmbedding: FloatArray, topK: Int = 5, minSimilarity: Float = 0.6f): List<Triple<Long, String, Float>> = withContext(Dispatchers.IO) {
        val all = getAll()
        val candidates = all.map { Pair("${it.noteId}:${it.content}", toFloatArray(it.embedding)) }
        bruteForceSearch(queryEmbedding, candidates, topK, minSimilarity).map { (key, score) ->
            val colonIndex = key.indexOf(':')
            val noteId = key.substring(0, colonIndex).toLong()
            val content = key.substring(colonIndex + 1)
            Triple(noteId, content, score)
        }
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) {
        val cursor = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM chunks")
        try {
            cursor.moveToFirst()
            cursor.getInt(0)
        } finally {
            cursor.close()
        }
    }

    private fun ChunkEntity.toContentValues(): ContentValues {
        return ContentValues().apply {
            put("note_id", noteId)
            put("chunk_index", chunkIndex)
            put("content", content)
            put("embedding", embedding)
            put("created_at", createdAt)
        }
    }

    companion object {
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
