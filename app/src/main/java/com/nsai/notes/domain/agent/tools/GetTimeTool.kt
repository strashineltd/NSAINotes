package com.nsai.notes.domain.agent.tools

import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.ToolParameter
import com.nsai.notes.domain.agent.ToolResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetTimeTool @Inject constructor() : AgentTool {
    override val name = "get_current_time"
    override val displayName = "获取当前时间"
    override val description = "获取当前日期和时间，用于计算截止日期、安排日程等"
    override val parameters = emptyList<ToolParameter>()
    override val isDestructive = false
    override suspend fun execute(params: Map<String, String>): ToolResult {
        val now = Date()
        val dateStr = SimpleDateFormat("yyyy年MM月dd日 EEEE HH:mm:ss", Locale.CHINESE).format(now)
        return ToolResult(true, "当前时间: $dateStr")
    }
}
