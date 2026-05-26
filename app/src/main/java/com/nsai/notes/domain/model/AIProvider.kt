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
        displayName = "DeepSeek-v4-Pro",
        defaultBaseUrl = "https://api.deepseek.com/v1",
        quickModel = "deepseek-v4-pro",
        thinkModel = "deepseek-v4-pro",
        imageModel = null,
        supportsVision = true
    ),
    KIMI(
        displayName = "Kimi K2.6",
        defaultBaseUrl = "https://api.moonshot.cn/v1",
        quickModel = "kimi-k2.6",
        thinkModel = "kimi-thinking-preview",
        imageModel = null
    ),
    GLM(
        displayName = "GLM 5.1",
        defaultBaseUrl = "https://open.bigmodel.cn/api/paas/v4",
        quickModel = "glm-5.1",
        thinkModel = "glm-5.1",
        imageModel = "cogview-3",
        supportsVision = true
    ),
    MINIMAX(
        displayName = "MiniMax 2.7",
        defaultBaseUrl = "https://api.minimax.chat/v1",
        quickModel = "minimax-text-01",
        thinkModel = "minimax-text-01",
        imageModel = "image-01"
    ),
    QWEN(
        displayName = "Qwen3.6Max",
        defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        quickModel = "qwen3.6-max",
        thinkModel = "qwen3.6-max",
        imageModel = "qwen3.6-max",
        supportsVision = true
    ),
    MIMO(
        displayName = "MiMo 2.5 Pro",
        defaultBaseUrl = "https://api.xiaomimimo.com/v1",
        quickModel = "mimo-2.5-pro",
        thinkModel = "mimo-2.5-pro",
        imageModel = null,
        supportsVision = false
    );

    fun getModelForMode(mode: AIMode): String? = when (mode) {
        AIMode.QUICK -> quickModel
        AIMode.THINK -> thinkModel
        AIMode.IMAGE -> imageModel
    }

    val supportsImage: Boolean get() = imageModel != null
    val hasThinkMode: Boolean get() = true
}
