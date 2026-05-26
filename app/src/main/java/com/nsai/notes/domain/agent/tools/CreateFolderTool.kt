package com.nsai.notes.domain.agent.tools

import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.ToolParameter
import com.nsai.notes.domain.agent.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateFolderTool @Inject constructor(@ApplicationContext private val context: Context) : AgentTool {
    override val name = "create_folder"; override val displayName = "创建文件夹"
    override val description = "在文件区创建一个新文件夹"
    override val parameters = listOf(
        ToolParameter("path", "string", "父目录路径，留空为根目录"),
        ToolParameter("name", "string", "文件夹名称")
    )
    override val isDestructive = false
    override suspend fun execute(params: Map<String, String>): ToolResult {
        val parent = params["path"]?.takeIf { it.isNotBlank() } ?: context.filesDir.path
        val name = params["name"] ?: return ToolResult(false, "", "缺少name参数")
        val dir = File(parent, name)
        return try {
            when {
                dir.exists() -> ToolResult(false, "", "文件夹已存在: ${dir.path}")
                dir.mkdirs() -> ToolResult(true, "文件夹创建成功: ${dir.path}")
                else -> ToolResult(false, "", "创建失败: ${dir.path}")
            }
        } catch (e: SecurityException) {
            ToolResult(false, "", "权限不足，无法创建: ${dir.path}")
        } catch (e: Exception) {
            ToolResult(false, "", "创建文件夹异常: ${e.message}")
        }
    }
}
