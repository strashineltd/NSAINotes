package com.nsai.notes.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val notes: String
)

@Singleton
class UpdateChecker @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    private val updateUrl = "https://nsainotes.github.io/version.json"

    suspend fun check(currentVersion: String): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(updateUrl)
                .header("User-Agent", "NSAINotes-Android")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("检查失败 (${response.code})"))
            }

            val json = gson.fromJson(body, Map::class.java)
            val latestVersion = json["version"] as? String ?: ""
            val downloadUrl = json["download_url"] as? String ?: ""
            val releaseNotes = json["notes"] as? String ?: ""

            if (compareVersions(latestVersion, currentVersion) > 0 && latestVersion.isNotBlank()) {
                Result.success(UpdateInfo(latestVersion, downloadUrl, releaseNotes))
            } else {
                Result.failure(NoUpdateException())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compareVersions(a: String, b: String): Int {
        val partsA = a.split(".").map { it.toIntOrNull() ?: 0 }
        val partsB = b.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val va = partsA.getOrElse(i) { 0 }
            val vb = partsB.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }
}

class NoUpdateException : Exception()
