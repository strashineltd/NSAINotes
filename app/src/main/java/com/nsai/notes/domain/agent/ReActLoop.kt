package com.nsai.notes.domain.agent

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.repository.AIOptions
import com.nsai.notes.domain.repository.AIResponse
import com.nsai.notes.domain.repository.AIService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReActLoop @Inject constructor(
    private val aiService: AIService,
    private val toolRegistry: ToolRegistry,
    private val gson: Gson
) {
    private val maxSteps = 8
    private val maxRetries = 2

    data class ReActStep(
        @SerializedName("thought") val thought: String = "",
        @SerializedName("action") val action: String? = null,
        @SerializedName("params") val params: Map<String, String>? = null,
        @SerializedName("final_answer") val finalAnswer: String? = null
    )

    data class AgentStepResult(val thought: String, val action: String?, val observation: String?)

    private val stepHistory = mutableListOf<AgentStepResult>()
    private val historyLock = Any()

    private fun readStepHistory(): List<AgentStepResult> = synchronized(historyLock) { stepHistory.toList() }

    suspend fun execute(goal: String, provider: AIProvider): String {
        synchronized(historyLock) { stepHistory.clear() }
        val systemPrompt = buildSystemPrompt()

        for (step in 1..maxSteps) {
            val messages = listOf(ChatMessage(ChatMessage.Role.SYSTEM, systemPrompt)) +
                readStepHistory().flatMap { s ->
                    listOf(
                        ChatMessage(ChatMessage.Role.ASSISTANT, "思考: ${s.thought}\n动作: ${s.action ?: "无"}"),
                        if (s.observation != null) ChatMessage(ChatMessage.Role.USER, "观察结果: ${s.observation}") else null
                    )
                }.filterNotNull() +
                listOf(ChatMessage(ChatMessage.Role.USER, if (step == 1) goal else "继续执行下一步"))

            val response = chatWithRetry(provider, messages, step)
            val parsed = parseResponse(response.content)
            if (parsed == null) { synchronized(historyLock) { stepHistory.add(AgentStepResult("无法解析AI响应", null, null)) }; continue }
            if (parsed.finalAnswer != null) return parsed.finalAnswer

            val toolName = parsed.action ?: continue
            val tool = toolRegistry.get(toolName)
            if (tool == null) { synchronized(historyLock) { stepHistory.add(AgentStepResult(parsed.thought, toolName, "工具 '$toolName' 不存在")) }; continue }

            val result = try {
                tool.execute(parsed.params ?: emptyMap())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ToolResult(false, "", "工具执行异常: ${e.message}")
            }
            val observation = if (result.success) result.data else "错误: ${result.error}"
            synchronized(historyLock) { stepHistory.add(AgentStepResult(parsed.thought, toolName, observation)) }
        }
        return "Agent 在 $maxSteps 步内无法完成任务，请尝试更具体的指令。"
    }

    private suspend fun chatWithRetry(provider: AIProvider, messages: List<ChatMessage>, step: Int): AIResponse {
        var lastEx: Exception? = null
        for (attempt in 0..maxRetries) {
            try {
                return aiService.chat(provider = provider, messages = messages,
                    options = AIOptions(temperature = 0.3f, maxTokens = 4096))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastEx = e
                if (attempt < maxRetries) delay(1000L * (attempt + 1))
            }
        }
        throw AIException("Agent AI调用失败(${maxRetries + 1}次重试): ${lastEx?.message}", "")
    }

    fun getSteps(): List<AgentStepResult> = synchronized(historyLock) { stepHistory.toList() }

    private fun buildSystemPrompt(): String = buildString {
        appendLine("你是NSAI笔记的AI Agent。你可以使用工具完成用户的各类任务。")
        appendLine()
        appendLine("【核心原则】")
        appendLine("- 每次只调用一个工具，等待结果后再决定下一步")
        appendLine("- 先思考(thought)，再行动(action)，最后给出最终回答(final_answer)")
        appendLine("- 如果工具返回了笔记列表，自动提取其中的ID用于后续操作")
        appendLine("- 完成任务后给出清晰、有帮助的总结")
        appendLine("- 如果用户请求模糊，先澄清再行动")
        appendLine()
        appendLine("【可用工具】")
        toolRegistry.all().forEach { tool ->
            val params = if (tool.parameters.isNotEmpty()) {
                " 需要参数: ${tool.parameters.joinToString { "${it.name}(${it.type})${if (!it.required) " [可选]" else ""}" }}"
            } else " 无需参数"
            appendLine("- ${tool.name}: ${tool.description}$params")
        }
        appendLine()
        appendLine("【返回格式】")
        appendLine("使用工具时，只返回一行JSON: {\"thought\": \"我的推理\", \"action\": \"工具名\", \"params\": {\"参数名\": \"参数值\"}}")
        appendLine("完成任务时，只返回一行JSON: {\"thought\": \"我的推理\", \"final_answer\": \"给用户的最终回答\"}")
        appendLine()
        appendLine("【示例】")
        appendLine("用户: 帮我搜索关于RAG的笔记")
        appendLine("你: {\"thought\": \"用户想找关于RAG的笔记，我用search_notes搜索\", \"action\": \"search_notes\", \"params\": {\"keyword\": \"RAG\"}}")
        appendLine("观察: - 笔记1 (ID:5) - 笔记2 (ID:8)")
        appendLine("你: {\"thought\": \"找到了2篇相关笔记，现在获取详细内容\", \"action\": \"get_note_content\", \"params\": {\"note_id\": \"5\"}}")
        appendLine("观察: 标题: RAG架构设计 内容: ...")
        appendLine("你: {\"thought\": \"已获取全文，可以给用户总结\", \"final_answer\": \"找到2篇RAG相关笔记：\\n1. RAG架构设计(ID:5)\\n2. RAG实现方案(ID:8)\\n需要我展开哪篇？\"}")
    }

    private fun parseResponse(content: String): ReActStep? {
        val jsonStart = content.indexOf('{')
        val jsonEnd = content.lastIndexOf('}') + 1
        if (jsonStart < 0 || jsonEnd <= jsonStart || jsonEnd > content.length) return null
        return try { gson.fromJson(content.substring(jsonStart, jsonEnd), ReActStep::class.java) } catch (_: Exception) { null }
    }
}

private class AIException(message: String, type: String) : Exception(message)
