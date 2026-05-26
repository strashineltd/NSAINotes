package com.nsai.notes.domain.agent.tools

import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.ToolParameter
import com.nsai.notes.domain.agent.ToolResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WriteFileTool @Inject constructor() : AgentTool {
    override val name = "write_file"; override val displayName = "写入文件"
    override val description = "将内容写入文件"
    override val parameters = listOf(
        ToolParameter("path", "string", "文件完整路径"),
        ToolParameter("content", "string", "要写入的内容")
    )
    override val isDestructive = true
    override suspend fun execute(params: Map<String, String>): ToolResult {
        val path = params["path"] ?: return ToolResult(false, "", "缺少path参数")
        val content = params["content"] ?: return ToolResult(false, "", "缺少content参数")
        return try {
            File(path).apply { parentFile?.mkdirs() }.writeText(content)
            ToolResult(true, "文件已写入: $path")
        } catch (e: Exception) {
            ToolResult(false, "", "写入失败: ${e.message}")
        }
    }
}
