package com.nsai.notes.domain.agent.tools

import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.ToolParameter
import com.nsai.notes.domain.agent.ToolResult
import com.nsai.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateNoteTool @Inject constructor(
    private val noteRepository: NoteRepository
) : AgentTool {
    override val name = "update_note"
    override val displayName = "更新笔记"
    override val description = "更新指定笔记的标题或内容。至少提供title或content之一"
    override val parameters = listOf(
        ToolParameter("note_id", "string", "笔记的ID（数字）"),
        ToolParameter("title", "string", "新标题（可选，不修改则留空）", required = false),
        ToolParameter("content", "string", "新内容（可选，不修改则留空）", required = false)
    )
    override val isDestructive = true
    override suspend fun execute(params: Map<String, String>): ToolResult {
        val idStr = params["note_id"] ?: return ToolResult(false, "", "缺少note_id参数")
        val noteId = idStr.toLongOrNull() ?: return ToolResult(false, "", "note_id必须是数字")
        val note = noteRepository.getNoteById(noteId).first()
            ?: return ToolResult(false, "", "笔记ID=$noteId 不存在")
        val newTitle = params["title"]?.takeIf { it.isNotBlank() } ?: note.title
        val newContent = params["content"]?.takeIf { it.isNotBlank() } ?: note.content
        noteRepository.updateNote(note.copy(title = newTitle, content = newContent))
        return ToolResult(true, "已更新笔记「$newTitle」")
    }
}
