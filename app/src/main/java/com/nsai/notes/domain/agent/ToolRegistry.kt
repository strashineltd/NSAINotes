package com.nsai.notes.domain.agent

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(
    private val tools: Set<@JvmSuppressWildcards AgentTool>
) {
    fun get(name: String): AgentTool? = tools.find { it.name == name }
    fun all(): List<AgentTool> = tools.toList()
}
