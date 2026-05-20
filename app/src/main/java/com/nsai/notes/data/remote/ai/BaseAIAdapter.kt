package com.nsai.notes.data.remote.ai

import com.google.gson.Gson
import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.data.local.security.ApiKeyProvider
import com.nsai.notes.domain.model.AIMode
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.repository.AIOptions
import com.nsai.notes.domain.repository.AIResponse
import com.nsai.notes.domain.repository.TokenUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

abstract class BaseAIAdapter(
    protected val settingsDataStore: SettingsDataStore,
    protected val apiKeyProvider: ApiKeyProvider,
    protected val client: OkHttpClient,
    protected val gson: Gson
) : AIProviderAdapter {
    abstract override val provider: AIProvider

    override suspend fun chat(messages: List<ChatMessage>, options: AIOptions, mode: AIMode): AIResponse =
        executeChat(messages, options, mode)

    override suspend fun summarize(text: String, options: AIOptions): String {
        val response = executeChat(buildSummaryMessages(text), options.copy(maxTokens = 1024), AIMode.QUICK)
        return response.content
    }

    override suspend fun generateImage(prompt: String, options: AIOptions): AIResponse =
        throw AIException("${provider.displayName}不支持图片生成", AIExceptionType.UNKNOWN)

    protected data class ChatRequestBody(
        val model: String,
        val messages: List<Message>,
        val temperature: Float = 0.7f,
        val max_tokens: Int = 2048,
        val stream: Boolean = false
    ) {
        data class Message(val role: String, val content: Any)
        data class ContentPart(val type: String, val text: String? = null, val image_url: ImageUrl? = null)
        data class ImageUrl(val url: String)
    }

    protected data class ChatResponseBody(
        val id: String? = null,
        val choices: List<Choice>? = null,
        val data: List<ImageData>? = null,
        val usage: Usage? = null
    ) {
        data class Choice(
            val index: Int = 0,
            val message: Message? = null,
            val finish_reason: String? = null
        )
        data class Message(val role: String? = null, val content: String? = null, val reasoning_content: String? = null)
        data class ImageData(val url: String? = null, val b64_json: String? = null)
        data class Usage(
            val prompt_tokens: Int = 0,
            val completion_tokens: Int = 0,
            val total_tokens: Int = 0
        )
    }

    protected open fun getEndpointPath(): String = "chat/completions"
    protected open fun getImageEndpointPath(): String = "images/generations"

    protected suspend fun executeChat(
        messages: List<ChatMessage>,
        options: AIOptions,
        mode: AIMode = AIMode.QUICK
    ): AIResponse = apiKeyProvider.withApiKey(provider) { config ->
        withContext(Dispatchers.IO) {
            if (!config.isEnabled) throw AIException("${provider.displayName}已禁用", AIExceptionType.AUTH)
            if (config.apiKey.isBlank()) throw AIException("API Key未配置", AIExceptionType.AUTH)

            val model = provider.getModelForMode(mode) ?: provider.quickModel
            val temp = if (mode == AIMode.THINK) 0.1f else options.temperature

            val body = ChatRequestBody(
                model = model,
                messages = messages.map { msg ->
                    if (options.imageData != null && msg.role == ChatMessage.Role.USER) {
                        ChatRequestBody.Message(
                            role = msg.role.name.lowercase(),
                            content = listOf(
                                ChatRequestBody.ContentPart(type = "text", text = msg.content),
                                ChatRequestBody.ContentPart(type = "image_url", image_url = ChatRequestBody.ImageUrl("data:image/jpeg;base64,${options.imageData}"))
                            )
                        )
                    } else {
                        ChatRequestBody.Message(role = msg.role.name.lowercase(), content = msg.content)
                    }
                },
                temperature = temp,
                max_tokens = if (mode == AIMode.THINK) 4096 else options.maxTokens
            )

            val jsonBody = gson.toJson(body)
            val url = "${config.baseUrl.trimEnd('/')}/${getEndpointPath()}"

            val request = Request.Builder()
                .url(url).addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType())).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            response.close()
            if (!response.isSuccessful) throw parseError(response.code, responseBody)

            val chatResponse = gson.fromJson(responseBody, ChatResponseBody::class.java)
            val message = chatResponse.choices?.firstOrNull()?.message
            val content = message?.content ?: ""
            val reasoning = message?.reasoning_content
            val usage = chatResponse.usage?.let { TokenUsage(it.prompt_tokens, it.completion_tokens, it.total_tokens) }

            AIResponse(content = content, model = model, reasoning = reasoning, usage = usage)
        }
    }

    protected suspend fun executeImageGeneration(
        prompt: String,
        options: AIOptions
    ): AIResponse = apiKeyProvider.withApiKey(provider) { config ->
        withContext(Dispatchers.IO) {
            if (config.apiKey.isBlank()) throw AIException("API Key未配置", AIExceptionType.AUTH)

            val model = provider.imageModel ?: throw AIException("该模型不支持图片生成", AIExceptionType.UNKNOWN)
            val body = mapOf("model" to model, "prompt" to prompt, "n" to 1, "size" to "1024x1024")
            val jsonBody = gson.toJson(body)
            val url = "${config.baseUrl.trimEnd('/')}/${getImageEndpointPath()}"

            val request = Request.Builder()
                .url(url).addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType())).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            response.close()
            if (!response.isSuccessful) throw parseError(response.code, responseBody)

            val imageResponse = gson.fromJson(responseBody, ChatResponseBody::class.java)
            val imageUrl = imageResponse.data?.firstOrNull()?.url ?: ""
            AIResponse(content = imageUrl, model = model)
        }
    }

    protected open fun parseError(code: Int, body: String?): AIException {
        return when (code) {
            400 -> {
                val detail = try {
                    val errorBody = gson.fromJson(body ?: "", Map::class.java)
                    (errorBody["error"] as? Map<*, *>)?.get("message") as? String
                        ?: errorBody["message"] as? String
                        ?: ""
                } catch (_: Exception) { "" }
                AIException(if (detail.isNotBlank()) "请求参数错误: $detail" else "请求参数错误", AIExceptionType.UNKNOWN)
            }
            401 -> AIException("API Key无效", AIExceptionType.AUTH)
            429 -> AIException("请求过频，稍后重试", AIExceptionType.RATE_LIMIT)
            500, 502, 503 -> AIException("AI服务暂不可用", AIExceptionType.SERVER)
            else -> AIException("请求失败: $code", AIExceptionType.UNKNOWN)
        }
    }

    protected fun buildSummaryMessages(text: String): List<ChatMessage> = listOf(
        ChatMessage(ChatMessage.Role.SYSTEM, "你是一个专业笔记助手。请用中文简洁总结以下内容。"),
        ChatMessage(ChatMessage.Role.USER, text)
    )
}

class AIException(message: String, val type: AIExceptionType) : Exception(message)
enum class AIExceptionType { AUTH, RATE_LIMIT, SERVER, UNKNOWN }
