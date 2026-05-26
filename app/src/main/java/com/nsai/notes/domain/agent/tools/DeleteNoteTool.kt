package com.nsai.notes.domain.agent.tools

import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.ToolParameter
import com.nsai.notes.domain.agent.ToolResult
import com.nsai.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteNoteTool @Inject constructor(
    private val noteRepository: NoteRepository
) : AgentTool {
    override val name = "delete_note"
    override val displayName = "删除笔记"
    override val description = "软删除指定ID的笔记。注意：此操作不可撤销"
    override val parameters = listOf(
        ToolParameter("note_id", "string", "要删除的笔记ID（数字）")
    )
    override val isDestructive = true
    override suspend fun execute(params: Map<String, String>): ToolResult {
        val idStr = params["note_id"] ?: return ToolResult(false, "", "缺少note_id参数")
        val noteId = idStr.toLongOrNull() ?: return ToolResult(false, "", "note_id必须是数字")
        val note = noteRepository.getNoteById(noteId).first()
            ?: return ToolResult(false, "", "笔记ID=$noteId 不存在")
        noteRepository.softDeleteNote(noteId)
        return ToolResult(true, "已删除笔记「${note.title}」")
    }
}
