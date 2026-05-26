package com.nsai.notes.domain.model

import androidx.compose.runtime.Stable

@Stable
enum class SearchEngine(val displayName: String, val searchUrl: String, val favicon: String) {
    BAIDU("百度", "https://www.baidu.com/s?wd={query}", "https://www.baidu.com/favicon.ico"),
    BING("必应", "https://www.bing.com/search?q={query}", "https://www.bing.com/favicon.ico"),
    CUSTOM("自定义", "", "");

    fun buildUrl(query: String, customUrl: String = ""): String {
        val template = if (this == CUSTOM) customUrl else searchUrl
        // Fallback when custom template is empty — use Bing as default
        if (template.isBlank()) return "https://www.bing.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
        return template.replace("{query}", java.net.URLEncoder.encode(query, "UTF-8"))
    }

    fun homepage(): String = when (this) {
        BAIDU -> "https://www.baidu.com"
        BING -> "https://www.bing.com"
        CUSTOM -> searchUrl.ifBlank { "https://www.bing.com" }
            .substringBefore("{query}").removeSuffix("?").removeSuffix("&")
    }

    companion object {
        fun fromName(name: String): SearchEngine =
            entries.find { it.name == name } ?: BING
    }
}
