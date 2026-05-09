package com.nsai.notes.domain.agent

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.repository.AIOptions
import com.nsai.notes.domain.repository.AIService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReActLoop @Inject constructor(
    private val aiService: AIService,
    private val toolRegistry: ToolRegistry
) {
    private val gson = Gson()
    private val maxSteps = 8

    data class ReActStep(
        @SerializedName("thought") val thought: String = "",
        @SerializedName("action") val action: String? = null,
        @SerializedName("params") val params: Map<String, String>? = null,
        @SerializedName("final_answer") val finalAnswer: String? = null
    )

    data class AgentStepResult(val thought: String, val action: String?, val observation: String?)

    private val stepHistory = mutableListOf<AgentStepResult>()

    suspend fun execute(goal: String, provider: AIProvider): String {
        stepHistory.clear()
        val systemPrompt = buildSystemPrompt()

        for (step in 1..maxSteps) {
            val messages = listOf(ChatMessage(ChatMessage.Role.SYSTEM, systemPrompt)) +
                stepHistory.flatMap { s ->
                    listOf(
                        ChatMessage(ChatMessage.Role.ASSISTANT, "思考: ${s.thought}\n动作: ${s.action ?: "无"}"),
                        if (s.observation != null) ChatMessage(ChatMessage.Role.USER, "观察结果: ${s.observation}") else null
                    )
                }.filterNotNull() +
                listOf(ChatMessage(ChatMessage.Role.USER, if (step == 1) goal else "继续执行下一步"))

            val response = aiService.chat(provider = provider, messages = messages, options = AIOptions(temperature = 0.3f, maxTokens = 2048))
            val parsed = parseResponse(response.content)
            if (parsed == null) { stepHistory.add(AgentStepResult("无法解析AI响应", null, null)); continue }
            if (parsed.finalAnswer != null) return parsed.finalAnswer

            val toolName = parsed.action ?: continue
            val tool = toolRegistry.get(toolName)
            if (tool == null) { stepHistory.add(AgentStepResult(parsed.thought, toolName, "工具 '$toolName' 不存在")); continue }

            val result = tool.execute(parsed.params ?: emptyMap())
            val observation = if (result.success) result.data else "错误: ${result.error}"
            stepHistory.add(AgentStepResult(parsed.thought, toolName, observation))
        }
        return "Agent 在 $maxSteps 步内无法完成任务，请尝试更具体的指令。"
    }

    fun getSteps(): List<AgentStepResult> = stepHistory.toList()

    private fun buildSystemPrompt(): String = buildString {
        appendLine("你是一个AI Agent，能够使用工具完成用户的任务。")
        appendLine("可用工具：")
        toolRegistry.all().forEach { tool ->
            appendLine("- ${tool.name}: ${tool.description}")
            if (tool.parameters.isNotEmpty()) appendLine("  参数: ${tool.parameters.joinToString { "${it.name}(${it.type})" }}")
        }
        appendLine()
        appendLine("返回格式：")
        appendLine("使用工具: {\"thought\": \"思考\", \"action\": \"工具名\", \"params\": {参数}}")
        appendLine("完成任务: {\"thought\": \"思考\", \"final_answer\": \"最终答案\"}")
    }

    private fun parseResponse(content: String): ReActStep? {
        val jsonStart = content.indexOf('{')
        val jsonEnd = content.lastIndexOf('}') + 1
        if (jsonStart < 0 || jsonEnd <= jsonStart) return null
        return try { gson.fromJson(content.substring(jsonStart, jsonEnd), ReActStep::class.java) } catch (_: Exception) { null }
    }
}
