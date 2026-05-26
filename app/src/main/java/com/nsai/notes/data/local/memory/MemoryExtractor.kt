package com.nsai.notes.data.local.memory

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.nsai.notes.domain.memory.Memory
import com.nsai.notes.domain.memory.MemoryType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryExtractor @Inject constructor(
    private val gson: Gson
) {
    private data class MemoryJson(@SerializedName("memories") val memories: List<MemoryItem>? = null)
    private data class MemoryItem(val type: String = "", val key: String = "", val content: String = "")

    fun parseMemoriesJson(json: String, sourceConversationId: Long? = null): List<Memory> {
        if (json.isBlank()) return emptyList()
        return try {
            val parsed = gson.fromJson(json, MemoryJson::class.java)
            parsed.memories?.mapNotNull { item ->
                val type = try { MemoryType.valueOf(item.type.uppercase()) } catch (_: Exception) { null } ?: return@mapNotNull null
                Memory(type = type, key = item.key, content = item.content, sourceConversationId = sourceConversationId)
            } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun buildExtractionPrompt(conversationText: String): String = buildString {
        appendLine("分析以下对话，提取用户的关键信息。返回JSON格式：")
        appendLine("{ \"memories\": [")
        appendLine("  { \"type\": \"PREFERENCE|FACT|DATE\", \"key\": \"简短标题\", \"content\": \"具体内容\" }")
        appendLine("]}")
        appendLine()
        appendLine("对话内容：")
        appendLine(conversationText.takeLast(3000))
    }
}
