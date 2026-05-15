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
     * Decrypts the API key, executes [block] with a config containing the plaintext key,
     * then discards the reference. The plaintext key exists in memory only during block execution.
     */
    suspend fun <T> withApiKey(
        provider: AIProvider,
        block: suspend (AIProviderConfig) -> T
    ): T {
        val config = settingsDataStore.getProviderConfig(provider)
        return block(config)
    }

    /** Returns config metadata without the API key. Safe for UI display. */
    suspend fun getConfigSafe(provider: AIProvider): AIProviderConfig {
        return settingsDataStore.getProviderConfigSafe(provider)
    }
}
