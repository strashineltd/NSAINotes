package com.nsai.notes.data.remote.ai

import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.repository.AIOptions
import com.nsai.notes.domain.repository.AIResponse
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GLMAdapter @Inject constructor(
    settingsDataStore: SettingsDataStore,
    client: OkHttpClient
) : BaseAIAdapter(settingsDataStore, client) {
    override val provider: AIProvider = AIProvider.GLM

    override suspend fun generateImage(prompt: String, options: AIOptions): AIResponse =
        executeImageGeneration(prompt, options)
}
