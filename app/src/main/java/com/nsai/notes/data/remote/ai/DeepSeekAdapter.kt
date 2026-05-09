package com.nsai.notes.data.remote.ai

import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.domain.model.AIProvider
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekAdapter @Inject constructor(
    settingsDataStore: SettingsDataStore,
    client: OkHttpClient
) : BaseAIAdapter(settingsDataStore, client) {
    override val provider: AIProvider = AIProvider.DEEPSEEK
}
