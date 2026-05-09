package com.nsai.notes.domain.memory

import com.nsai.notes.data.local.embedding.EmbeddingEngine
import com.nsai.notes.data.local.memory.MemoryDao
import com.nsai.notes.data.local.vector.VectorStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrieveMemoriesUseCase @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val memoryDao: MemoryDao
) {
    suspend fun retrieve(query: String, topK: Int = 3): String {
        if (!embeddingEngine.isInitialized) return ""
        val queryEmbedding = embeddingEngine.embed(query)
        val all = memoryDao.getAllWithEmbedding()
        if (all.isEmpty()) return ""
        val candidates = all.map { entity -> Pair(entity, VectorStore.toFloatArray(entity.embedding)) }
        val results = VectorStore.bruteForceSearch(
            queryEmbedding, candidates.map { (e, emb) -> "${e.id}" to emb }, topK, minSimilarity = 0.5f
        )
        if (results.isEmpty()) return ""
        val memoryMap = all.associateBy { it.id }
        return buildString {
            appendLine("关于用户的已知信息：")
            results.forEach { (idStr, _) ->
                memoryMap[idStr.toLong()]?.let { mem ->
                    appendLine("- [${mem.type}] ${mem.key}: ${mem.content}")
                    memoryDao.incrementAccess(mem.id)
                }
            }
        }
    }
}
