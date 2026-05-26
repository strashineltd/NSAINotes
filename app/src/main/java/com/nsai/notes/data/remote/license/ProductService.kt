package com.nsai.notes.data.remote.license

import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

data class ProductInfo(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Long = 0,
    @SerializedName("duration_days") val durationDays: Int = 0,
    val features: List<String>? = null,
    @SerializedName("is_active") val isActive: Boolean = true
) {
    val priceYuan: String get() = "%.2f".format(price / 100.0)
    val durationLabel: String get() = when {
        durationDays >= 1095 -> "3年"
        durationDays >= 730 -> "2年"
        durationDays >= 365 -> "1年"
        durationDays >= 180 -> "${durationDays / 30}个月"
        else -> "${durationDays}天"
    }
}

data class OrderResult(
    val orderId: String = "",
    val amount: Long = 0,
    val status: String = "",
    val error: String? = null
)

@Singleton
class ProductService @Inject constructor(
    private val client: OkHttpClient
) {
    var serverUrl: String = com.nsai.notes.data.remote.ServerConfig.baseUrl

    fun fetchProducts(): List<ProductInfo> {
        return try {
            val request = Request.Builder().url("$serverUrl/api/v1/products").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            response.close()
            val gson = com.google.gson.Gson()
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, ProductInfo::class.java).type
            gson.fromJson<List<ProductInfo>>(body, type)?.filter { it.isActive } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun createOrder(productId: String, deviceId: String): OrderResult {
        return try {
            val json = org.json.JSONObject().apply {
                put("product_id", productId); put("device_id", deviceId); put("payment_method", "app")
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$serverUrl/api/v1/orders").post(body).build()
            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: "{}"
            response.close()
            val r = org.json.JSONObject(respBody)
            if (response.isSuccessful) OrderResult(r.optString("order_id", ""), r.optLong("amount", 0), r.optString("status", "pending"))
            else OrderResult(error = r.optString("error", r.optString("message", "创建失败")))
        } catch (e: Exception) { OrderResult(error = "网络错误: ${e.message}") }
    }

    fun checkOrderStatus(orderId: String): String? {
        return try {
            val req = Request.Builder().url("$serverUrl/api/v1/orders/$orderId/activation").get().build()
            val resp = client.newCall(req).execute()
            val activationRespBody = resp.body?.string() ?: "{}"
            resp.close()
            if (resp.isSuccessful) org.json.JSONObject(activationRespBody).optString("activation_code", null)
            else null
        } catch (_: Exception) { null }
    }
}
