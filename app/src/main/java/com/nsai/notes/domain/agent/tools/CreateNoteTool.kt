package com.nsai.notes.domain.agent.tools

import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.ToolParameter
import com.nsai.notes.domain.agent.ToolResult
import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateNoteTool @Inject constructor(private val noteRepository: NoteRepository) : AgentTool {
    override val name = "create_note"; override val displayName = "创建笔记"
    override val description = "创建一篇新笔记"
    override val parameters = listOf(ToolParameter("title", "string", "笔记标题"), ToolParameter("content", "string", "笔记内容（Markdown格式）"))
    override val isDestructive = false
    override suspend fun execute(params: Map<String, String>): ToolResult {
        val title = params["title"] ?: return ToolResult(false, "", "缺少title参数")
        val content = params["content"] ?: return ToolResult(false, "", "缺少content参数")
        val id = noteRepository.createNote(Note(title = title, content = content))
        return ToolResult(true, "已创建笔记「$title」(ID: $id)")
    }
}
