package com.nsai.notes.domain.agent

data class ToolParameter(val name: String, val type: String, val description: String, val required: Boolean = true)
data class ToolResult(val success: Boolean, val data: String, val error: String? = null)

interface AgentTool {
    val name: String
    val displayName: String
    val description: String
    val parameters: List<ToolParameter>
    val isDestructive: Boolean
    suspend fun execute(params: Map<String, String>): ToolResult
}
