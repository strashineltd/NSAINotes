package com.nsai.notes.domain.model

import androidx.compose.runtime.Stable

@Stable
data class SkillPlugin(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val prompt: String,
    val isEnabled: Boolean = true,
    val category: Category = Category.GENERAL,
    val pluginType: PluginType = PluginType.LOCAL_PROMPT,
    val pluginUrl: String = "",
    val pluginHeaders: String = ""  // JSON: {"key":"value"}
) {
    enum class Category(val label: String) {
        GENERAL("通用"),
        WRITING("写作"),
        CODE("编程"),
        ANALYSIS("分析"),
        TRANSLATION("翻译"),
        CUSTOM("自定义")
    }

    enum class PluginType(val label: String) {
        LOCAL_PROMPT("本地提示词"),
        EXTERNAL_API("外部API"),
        MCP_SERVER("MCP服务器"),
        WEBHOOK("Webhook")
    }
}
