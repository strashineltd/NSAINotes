package com.nsai.notes.data.local.security

import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.AIProviderConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyProvider @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val keyStoreManager: KeyStoreManager
) {
    /**
     * Use-pattern: decrypt API key, execute block, then discard reference.
     * Key exists in memory only during block execution.
     */
    suspend fun <T> withApiKey(
        provider: AIProvider,
        block: suspend (AIProviderConfig) -> T
    ): T {
        val config = settingsDataStore.getProviderConfig(provider)
        return block(config)
    }

    suspend fun getConfigSafe(provider: AIProvider): AIProviderConfig {
        return settingsDataStore.getProviderConfig(provider)
    }
}
