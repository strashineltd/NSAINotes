package com.nsai.notes.data.remote.search

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.nsai.notes.BuildConfig
import com.nsai.notes.domain.model.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val iconUrl: String? = null
)

@Singleton
class WebSearchService @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    private val cacheMutex = Mutex()

    private val searchCache = object : LinkedHashMap<String, List<SearchResult>>(
        16, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<SearchResult>>?): Boolean {
            return size > 32
        }
    }

    suspend fun search(query: String, engine: SearchEngine = SearchEngine.BING): List<SearchResult> = cacheMutex.withLock {
        val normalized = "${engine.name}:${query.trim().lowercase()}"
        searchCache[normalized]?.let { return@withLock it }
        val results = when (engine) {
            SearchEngine.BING -> searchBing(query)
            SearchEngine.CUSTOM -> searchDuckDuckGo(query)
            else -> searchDuckDuckGo(query)
        }
        searchCache[normalized] = results
        results
    }

    private suspend fun searchBing(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://www.bing.com/search?q=$encodedQuery"
            val response = client.newCall(
                Request.Builder().url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                    .build()
            ).execute()
            val body = response.body?.string() ?: ""
            response.close()
            if (!response.isSuccessful || body.isBlank()) return@withContext searchDuckDuckGo(query)

            val doc = Jsoup.parse(body)
            val results = doc.select("li.b_algo").mapNotNull { el ->
                val link = el.selectFirst("h2 a") ?: return@mapNotNull null
                val url = link.attr("abs:href").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val title = link.text().trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val snippet = el.selectFirst(".b_caption p, .b_lineclamp2, .b_lineclamp3")?.text()?.trim() ?: ""
                if (snippet.length > 4 && snippet.startsWith("http")) {
                    // Skip search history injection-like entries
                }
                SearchResult(title, url, snippet, "https://www.bing.com/favicon.ico")
            }.take(8)

            if (results.isEmpty()) searchDuckDuckGo(query)
            else results
        } catch (_: Exception) {
            searchDuckDuckGo(query)
        }
    }

    private suspend fun searchDuckDuckGo(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"

            val request = Request.Builder().url(url)
                .addHeader("User-Agent", "NSAINotes-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()
            if (!response.isSuccessful || body.isBlank()) return@withContext buildFallback(query, encodedQuery)

            val ddgResponse = gson.fromJson(body, DuckDuckGoResponse::class.java)
                ?: return@withContext buildFallback(query, encodedQuery)
            val results = mutableListOf<SearchResult>()

            ddgResponse.AbstractText?.takeIf { it.isNotBlank() }?.let {
                results.add(SearchResult(
                    title = ddgResponse.Heading ?: query,
                    url = ddgResponse.AbstractURL?.takeIf { it.isNotBlank() } ?: "https://duckduckgo.com/?q=$encodedQuery",
                    snippet = it,
                    iconUrl = ddgResponse.Image
                ))
            }

            ddgResponse.RelatedTopics?.forEach { topic ->
                if (!topic.FirstURL.isNullOrBlank()) {
                    results.add(SearchResult(
                        title = topic.Text?.take(80)?.replace("<[^>]+>".toRegex(), "") ?: topic.FirstURL,
                        url = topic.FirstURL,
                        snippet = topic.Text?.replace("<[^>]+>".toRegex(), "") ?: "",
                        iconUrl = topic.Icon?.URL?.let { "https://duckduckgo.com$it" }
                    ))
                }
            }

            if (results.isEmpty()) buildFallback(query, encodedQuery)
            else results.take(8)
        } catch (_: Exception) {
            buildFallback(query, java.net.URLEncoder.encode(query, "UTF-8"))
        }
    }

    private fun buildFallback(query: String, encodedQuery: String): List<SearchResult> = listOf(
        SearchResult(
            title = "在 Bing 搜索 \"$query\"",
            url = "https://www.bing.com/search?q=$encodedQuery",
            snippet = "点击在浏览器中打开 Bing 搜索结果",
            iconUrl = "https://www.bing.com/favicon.ico"
        ),
        SearchResult(
            title = "在 Google 搜索 \"$query\"",
            url = "https://www.google.com/search?q=$encodedQuery",
            snippet = "点击在浏览器中打开 Google 搜索结果",
            iconUrl = "https://www.google.com/favicon.ico"
        )
    )

    // DuckDuckGo API response classes
    private data class DuckDuckGoResponse(
        @SerializedName("AbstractText") val AbstractText: String?,
        @SerializedName("AbstractURL") val AbstractURL: String?,
        @SerializedName("Heading") val Heading: String?,
        @SerializedName("Image") val Image: String?,
        @SerializedName("RelatedTopics") val RelatedTopics: List<Topic>?
    )

    private data class Topic(
        @SerializedName("FirstURL") val FirstURL: String?,
        @SerializedName("Text") val Text: String?,
        @SerializedName("Icon") val Icon: Icon?
    )

    private data class Icon(
        @SerializedName("URL") val URL: String?
    )
}
