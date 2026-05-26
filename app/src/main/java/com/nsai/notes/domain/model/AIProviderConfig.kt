package com.nsai.notes.domain.model

import androidx.compose.runtime.Stable

@Stable
data class AIProviderConfig(
    val provider: AIProvider,
    val apiKey: String = "",
    val baseUrl: String = provider.defaultBaseUrl,
    val isEnabled: Boolean = false
)
