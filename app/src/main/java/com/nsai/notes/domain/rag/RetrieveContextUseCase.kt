package com.nsai.notes.domain.rag

import com.nsai.notes.data.local.embedding.EmbeddingEngine
import com.nsai.notes.data.local.vector.VectorStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrieveContextUseCase @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val vectorStore: VectorStore
) {
    suspend fun retrieve(query: String, topK: Int = 5, minSimilarity: Float = 0.6f): String {
        if (!embeddingEngine.isInitialized) return ""
        val queryEmbedding = embeddingEngine.embed(query)
        val results = vectorStore.search(queryEmbedding, topK, minSimilarity)
        if (results.isEmpty()) return ""
        return buildString {
            appendLine("以下来自你的笔记的相关内容：")
            results.forEachIndexed { index, (noteId, content, score) ->
                appendLine("---")
                appendLine("[笔记#$noteId] (相关度: ${"%.1f".format(score * 100)}%)")
                appendLine(content.take(800))
            }
        }
    }
}
