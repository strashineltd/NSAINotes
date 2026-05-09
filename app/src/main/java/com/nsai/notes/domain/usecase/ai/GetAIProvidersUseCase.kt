package com.nsai.notes.domain.usecase.ai

import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.AIProviderConfig
import javax.inject.Inject

class GetAIProvidersUseCase @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(): List<AIProviderConfig> {
        return AIProvider.entries.map { provider ->
            settingsDataStore.getProviderConfig(provider)
        }
    }
}
