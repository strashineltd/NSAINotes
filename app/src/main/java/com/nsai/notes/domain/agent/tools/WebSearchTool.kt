package com.nsai.notes.domain.agent.tools

import com.nsai.notes.data.remote.search.WebSearchService
import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.ToolParameter
import com.nsai.notes.domain.agent.ToolResult
import com.nsai.notes.domain.model.SearchEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSearchTool @Inject constructor(private val webSearchService: WebSearchService) : AgentTool {
    override val name = "web_search"; override val displayName = "联网搜索"
    override val description = "在互联网上搜索信息"
    override val parameters = listOf(ToolParameter("query", "string", "搜索查询"))
    override val isDestructive = false
    override suspend fun execute(params: Map<String, String>): ToolResult {
        val query = params["query"] ?: return ToolResult(false, "", "缺少query参数")
        val results = webSearchService.search(query, SearchEngine.BING)
        return ToolResult(true, if (results.isEmpty()) "未找到结果" else results.take(5).joinToString("\n") { "- ${it.title}: ${it.snippet.take(200)}" })
    }
}
