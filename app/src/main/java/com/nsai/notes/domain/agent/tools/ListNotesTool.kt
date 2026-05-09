package com.nsai.notes.domain.agent.tools

import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.ToolParameter
import com.nsai.notes.domain.agent.ToolResult
import com.nsai.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListNotesTool @Inject constructor(private val noteRepository: NoteRepository) : AgentTool {
    override val name = "list_notes"; override val displayName = "列出笔记"
    override val description = "列出最近的笔记"
    override val parameters = emptyList<ToolParameter>(); override val isDestructive = false
    override suspend fun execute(params: Map<String, String>): ToolResult {
        val notes = noteRepository.getAllNotes().first()
        return ToolResult(true, if (notes.isEmpty()) "暂无笔记" else notes.take(20).joinToString("\n") { "- ${it.title} (ID:${it.id})" })
    }
}
