package com.nsai.notes.domain.model

import androidx.compose.runtime.Stable

@Stable
enum class AIMode(val label: String) {
    QUICK("快速模式"),
    THINK("思考模式"),
    IMAGE("图片生成")
}

@Stable
enum class AIProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val quickModel: String,
    val thinkModel: String,
    val imageModel: String?,
    val supportsVision: Boolean = false
) {
    DEEPSEEK(
        displayName = "DeepSeek-v4Pro",
        defaultBaseUrl = "https://api.deepseek.com/v1",
        quickModel = "deepseek-chat",
        thinkModel = "deepseek-reasoner",
        imageModel = null
    ),
    KIMI(
        displayName = "Kimi 2.6",
        defaultBaseUrl = "https://api.moonshot.cn/v1",
        quickModel = "moonshot-v1-8k",
        thinkModel = "moonshot-v1-8k",
        imageModel = null
    ),
    GLM(
        displayName = "GLM 5.1",
        defaultBaseUrl = "https://open.bigmodel.cn/api/paas/v4",
        quickModel = "glm-4-flash",
        thinkModel = "glm-4-plus",
        imageModel = "cogview-3",
        supportsVision = true
    ),
    MINIMAX(
        displayName = "MiniMax 2.7",
        defaultBaseUrl = "https://api.minimax.chat/v1",
        quickModel = "minimax-text-01",
        thinkModel = "minimax-text-01",
        imageModel = null
    ),
    QWEN(
        displayName = "Qwen3.6Max",
        defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        quickModel = "qwen3-max",
        thinkModel = "qwen3-max",
        imageModel = null,
        supportsVision = true
    );

    fun getModelForMode(mode: AIMode): String? = when (mode) {
        AIMode.QUICK -> quickModel
        AIMode.THINK -> thinkModel
        AIMode.IMAGE -> imageModel
    }

    val supportsImage: Boolean get() = imageModel != null
    val hasThinkMode: Boolean get() = true
}
