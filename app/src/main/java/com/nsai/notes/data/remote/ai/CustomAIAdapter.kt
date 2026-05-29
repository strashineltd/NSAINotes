package com.nsai.notes.data.remote.ai

import com.google.gson.Gson
import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.data.local.security.ApiKeyProvider
import com.nsai.notes.domain.model.AIMode
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.repository.AIOptions
import com.nsai.notes.domain.repository.AIResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomAIAdapter @Inject constructor(
    settingsDataStore: SettingsDataStore,
    apiKeyProvider: ApiKeyProvider,
    client: OkHttpClient,
    gson: Gson,
    private val dataStore: SettingsDataStore
) : BaseAIAdapter(settingsDataStore, apiKeyProvider, client, gson) {
    override val provider: AIProvider = AIProvider.CUSTOM

    override suspend fun chat(messages: List<ChatMessage>, options: AIOptions, mode: AIMode): AIResponse {
        val configs = dataStore.customProviders.first()
        val config = configs.firstOrNull { it.isEnabled }
            ?: throw AIException("未启用自定义模型", AIExceptionType.AUTH)

        if (config.apiKey.isBlank()) throw AIException("API Key未配置", AIExceptionType.AUTH)

        return withContext(Dispatchers.IO) {
            val model = config.customModelName?.takeIf { it.isNotBlank() }
                ?: throw AIException("模型名未配置", AIExceptionType.UNKNOWN)

            val body = ChatRequestBody(
                model = model,
                messages = messages.map { msg ->
                    ChatRequestBody.Message(role = msg.role.name.lowercase(), content = msg.content)
                },
                temperature = options.temperature,
                max_tokens = options.maxTokens
            )

            val jsonBody = gson.toJson(body)
            val url = "${config.baseUrl.trimEnd('/')}/chat/completions"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            response.close()

            if (!response.isSuccessful) throw parseError(response.code, responseBody)

            val chatResponse = gson.fromJson(responseBody, ChatResponseBody::class.java)
            val message = chatResponse.choices?.firstOrNull()?.message
            val content = message?.content ?: ""
            val usage = chatResponse.usage?.let {
                com.nsai.notes.domain.repository.TokenUsage(it.prompt_tokens, it.completion_tokens, it.total_tokens)
            }

            AIResponse(content = content, model = model, usage = usage)
        }
    }
}
