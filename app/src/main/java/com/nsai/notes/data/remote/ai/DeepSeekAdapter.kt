package com.nsai.notes.data.remote.ai

import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.data.local.security.ApiKeyProvider
import com.nsai.notes.domain.model.AIProvider
import com.google.gson.Gson
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekAdapter @Inject constructor(
    settingsDataStore: SettingsDataStore,
    apiKeyProvider: ApiKeyProvider,
    client: OkHttpClient,
    gson: Gson
) : BaseAIAdapter(settingsDataStore, apiKeyProvider, client, gson) {
    override val provider: AIProvider = AIProvider.DEEPSEEK
}
