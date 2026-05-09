package com.nsai.notes.data.remote.ai

import com.nsai.notes.domain.model.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionTester @Inject constructor(
    private val client: OkHttpClient
) {

    suspend fun testConnection(provider: AIProvider, apiKey: String, baseUrl: String): String =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext "API Key未配置"

            val jsonBody = """{"model":"${provider.quickModel}","messages":[{"role":"user","content":"hi"}],"max_tokens":1}"""
            val url = "${baseUrl.trimEnd('/')}/chat/completions"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) "连接成功 ✓"
            else "连接失败 (${response.code})"
        }
}
