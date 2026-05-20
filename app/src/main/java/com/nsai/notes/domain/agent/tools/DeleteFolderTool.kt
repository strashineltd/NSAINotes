package com.nsai.notes.domain.agent.tools

import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.ToolParameter
import com.nsai.notes.domain.agent.ToolResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteFolderTool @Inject constructor() : AgentTool {
    override val name = "delete_folder"; override val displayName = "删除文件夹"
    override val description = "删除一个空文件夹"
    override val parameters = listOf(ToolParameter("path", "string", "文件夹完整路径"))
    override val isDestructive = true
    override suspend fun execute(params: Map<String, String>): ToolResult {
        val path = params["path"] ?: return ToolResult(false, "", "缺少path参数")
        val dir = File(path)
        return try {
            when {
                !dir.exists() -> ToolResult(false, "", "文件夹不存在: $path")
                !dir.isDirectory -> ToolResult(false, "", "不是文件夹: $path")
                dir.listFiles()?.isNotEmpty() == true -> ToolResult(false, "", "文件夹不为空，无法删除: $path")
                dir.delete() -> ToolResult(true, "文件夹已删除: $path")
                else -> ToolResult(false, "", "删除失败: $path")
            }
        } catch (e: SecurityException) {
            ToolResult(false, "", "权限不足，无法删除: $path")
        } catch (e: Exception) {
            ToolResult(false, "", "删除文件夹异常: ${e.message}")
        }
    }
}
