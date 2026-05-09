package com.nsai.notes.domain.model

import androidx.compose.runtime.Stable

@Stable
data class MCPServer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val apiKey: String = "",
    val transport: Transport = Transport.SSE,
    val isEnabled: Boolean = true,
    val description: String = ""
) {
    enum class Transport(val label: String) {
        SSE("SSE (Server-Sent Events)"),
        STDIO("STDIO")
    }
}
