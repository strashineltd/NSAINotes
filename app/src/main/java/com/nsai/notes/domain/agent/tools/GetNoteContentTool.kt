package com.nsai.notes.domain.agent.tools

import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.ToolParameter
import com.nsai.notes.domain.agent.ToolResult
import com.nsai.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetNoteContentTool @Inject constructor(
    private val noteRepository: NoteRepository
) : AgentTool {
    override val name = "get_note_content"
    override val displayName = "获取笔记内容"
    override val description = "通过笔记ID获取笔记的完整标题和内容，用于阅读或后续编辑"
    override val parameters = listOf(
        ToolParameter("note_id", "string", "笔记的ID（数字）")
    )
    override val isDestructive = false
    override suspend fun execute(params: Map<String, String>): ToolResult {
        val idStr = params["note_id"] ?: return ToolResult(false, "", "缺少note_id参数")
        val noteId = idStr.toLongOrNull() ?: return ToolResult(false, "", "note_id必须是数字")
        val note = noteRepository.getNoteById(noteId).first()
        return if (note != null) {
            ToolResult(true, "标题: ${note.title}\n内容: ${note.content.take(2000)}${if (note.content.length > 2000) "..." else ""}")
        } else {
            ToolResult(false, "", "笔记ID=$noteId 不存在")
        }
    }
}
