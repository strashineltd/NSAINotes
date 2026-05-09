package com.nsai.notes.domain.agent.tools

import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.ToolParameter
import com.nsai.notes.domain.agent.ToolResult
import com.nsai.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchNotesTool @Inject constructor(private val noteRepository: NoteRepository) : AgentTool {
    override val name = "search_notes"; override val displayName = "搜索笔记"
    override val description = "在笔记中搜索指定关键词，返回匹配的笔记列表"
    override val parameters = listOf(ToolParameter("keyword", "string", "搜索关键词"))
    override val isDestructive = false
    override suspend fun execute(params: Map<String, String>): ToolResult {
        val keyword = params["keyword"] ?: return ToolResult(false, "", "缺少keyword参数")
        val notes = noteRepository.searchNotes(keyword).first()
        return ToolResult(true, if (notes.isEmpty()) "未找到匹配笔记" else notes.take(10).joinToString("\n") { "- ${it.title} (ID:${it.id})" })
    }
}
