package com.nsai.notes.domain.agent.tools

import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.ToolParameter
import com.nsai.notes.domain.agent.ToolResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadFileTool @Inject constructor() : AgentTool {
    override val name = "read_file"; override val displayName = "读取文件"
    override val description = "读取文件内容"
    override val parameters = listOf(ToolParameter("path", "string", "文件完整路径"))
    override val isDestructive = false
    override suspend fun execute(params: Map<String, String>): ToolResult {
        val path = params["path"] ?: return ToolResult(false, "", "缺少path参数")
        val file = File(path)
        return when {
            !file.exists() -> ToolResult(false, "", "文件不存在: $path")
            file.isDirectory -> ToolResult(false, "", "是文件夹而非文件: $path")
            else -> try {
                val content = file.readText()
                ToolResult(true, content.take(3000))
            } catch (e: Exception) {
                ToolResult(false, "", "读取失败: ${e.message}")
            }
        }
    }
}
