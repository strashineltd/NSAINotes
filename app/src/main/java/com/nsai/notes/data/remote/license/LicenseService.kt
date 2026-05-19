package com.nsai.notes.data.remote.license

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class LicenseValidateRequest(
    @SerializedName("activation_code") val activationCode: String,
    @SerializedName("device_id") val deviceId: String
)

data class LicenseValidateResponse(
    val valid: Boolean = false,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("expire_days") val expireDays: Long? = null,
    @SerializedName("expire_timestamp") val expireTimestamp: Long? = null,
    val features: List<String>? = null,
    val message: String = ""
)

@Singleton
class LicenseService @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    var serverUrl: String = com.nsai.notes.data.remote.ServerConfig.baseUrl

    suspend fun validate(activationCode: String, deviceId: String): LicenseValidateResponse = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("activation_code", activationCode)
                put("device_id", deviceId)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$serverUrl/api/v1/license/validate")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: """{"valid":false,"message":"服务器无响应"}"""
            gson.fromJson(responseBody, LicenseValidateResponse::class.java)
        } catch (e: Exception) {
            LicenseValidateResponse(valid = false, message = "网络错误: ${e.message}")
        }
    }
}
