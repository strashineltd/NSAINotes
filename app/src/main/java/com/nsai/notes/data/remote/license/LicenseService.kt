package com.nsai.notes.data.remote.license

import com.google.gson.annotations.SerializedName
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
    private val client: OkHttpClient
) {
    // Backend management platform URL
    // Use 10.0.2.2 for Android emulator (maps to host localhost), or real IP for physical devices
    var serverUrl: String = "http://10.0.2.2:3005"

    fun validate(activationCode: String, deviceId: String): LicenseValidateResponse {
        return try {
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
            val gson = com.google.gson.Gson()
            gson.fromJson(responseBody, LicenseValidateResponse::class.java)
        } catch (e: Exception) {
            LicenseValidateResponse(valid = false, message = "网络错误: ${e.message}")
        }
    }
}
