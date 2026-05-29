package com.nsai.notes.presentation.ai

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.data.remote.ai.ConnectionTester
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.AIProviderConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
data class AIModelSettingsUiState(
    val providerConfigs: List<AIProviderConfig> = emptyList(),
    val testResults: Map<AIProvider, String> = emptyMap()
)

sealed class AIModelSettingsEvent {
    data class UpdateApiKey(val provider: AIProvider, val apiKey: String, val customIndex: Int = -1) : AIModelSettingsEvent()
    data class UpdateBaseUrl(val provider: AIProvider, val baseUrl: String, val customIndex: Int = -1) : AIModelSettingsEvent()
    data class ToggleEnabled(val provider: AIProvider, val customIndex: Int = -1) : AIModelSettingsEvent()
    data class TestConnection(val provider: AIProvider) : AIModelSettingsEvent()
    data object LoadConfigs : AIModelSettingsEvent()
    data class AddCustomProvider(
        val displayName: String, val apiKey: String, val baseUrl: String, val modelName: String
    ) : AIModelSettingsEvent()
    data class UpdateCustomProvider(val index: Int, val config: AIProviderConfig) : AIModelSettingsEvent()
    data class DeleteCustomProvider(val index: Int) : AIModelSettingsEvent()
    data class ClearApiKey(val provider: AIProvider, val customIndex: Int = -1) : AIModelSettingsEvent()
}

@HiltViewModel
class AIModelSettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val connectionTester: ConnectionTester
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIModelSettingsUiState())
    val uiState: StateFlow<AIModelSettingsUiState> = _uiState.asStateFlow()

    init { /* load configs deferred to Screen on first composition */ }

    fun load() {
        loadConfigs()
    }

    fun onEvent(event: AIModelSettingsEvent) {
        when (event) {
            is AIModelSettingsEvent.UpdateApiKey -> updateApiKey(event.provider, event.apiKey, event.customIndex)
            is AIModelSettingsEvent.UpdateBaseUrl -> updateBaseUrl(event.provider, event.baseUrl, event.customIndex)
            is AIModelSettingsEvent.ToggleEnabled -> toggleEnabled(event.provider, event.customIndex)
            is AIModelSettingsEvent.TestConnection -> testConnection(event.provider)
            AIModelSettingsEvent.LoadConfigs -> loadConfigs()
            is AIModelSettingsEvent.AddCustomProvider -> addCustomProvider(event.displayName, event.apiKey, event.baseUrl, event.modelName)
            is AIModelSettingsEvent.UpdateCustomProvider -> updateCustom(event.index, event.config)
            is AIModelSettingsEvent.DeleteCustomProvider -> deleteCustom(event.index)
            is AIModelSettingsEvent.ClearApiKey -> clearApiKey(event.provider, event.customIndex)
        }
    }

    private fun addCustomProvider(displayName: String, apiKey: String, baseUrl: String, modelName: String) {
        viewModelScope.launch {
            val config = AIProviderConfig(
                provider = AIProvider.CUSTOM,
                apiKey = apiKey,
                baseUrl = baseUrl,
                isEnabled = true,
                customModelName = modelName,
                customDisplayName = displayName
            )
            settingsDataStore.addCustomProvider(config)
        }
    }

    private fun updateCustom(index: Int, config: AIProviderConfig) {
        viewModelScope.launch {
            settingsDataStore.updateCustomProvider(index, config)
        }
    }

    private fun deleteCustom(index: Int) {
        viewModelScope.launch {
            settingsDataStore.deleteCustomProvider(index)
        }
    }

    private fun clearApiKey(provider: AIProvider, customIndex: Int) {
        val isCustom = provider == AIProvider.CUSTOM && customIndex >= 0
        _uiState.update { state ->
            state.copy(providerConfigs = state.providerConfigs.mapIndexed { i, config ->
                if (isCustom && i == customIndex && config.provider == AIProvider.CUSTOM) config.copy(apiKey = "")
                else if (!isCustom && config.provider == provider) config.copy(apiKey = "")
                else config
            })
        }
        viewModelScope.launch {
            try {
                if (isCustom) {
                    val existing = settingsDataStore.customProviders.first()
                    if (customIndex < existing.size) {
                        settingsDataStore.updateCustomProvider(customIndex, existing[customIndex].copy(apiKey = ""))
                    }
                } else {
                    settingsDataStore.saveProviderConfig(
                        settingsDataStore.getProviderConfig(provider).copy(apiKey = "")
                    )
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadConfigs() {
        viewModelScope.launch {
            try {
                settingsDataStore.getAllProviderConfigs().collectLatest { configs ->
                    _uiState.value = _uiState.value.copy(providerConfigs = configs)
                }
            } catch (_: Exception) { }
        }
    }

    private fun updateApiKey(provider: AIProvider, apiKey: String, customIndex: Int) {
        val isCustom = provider == AIProvider.CUSTOM && customIndex >= 0
        _uiState.update { state ->
            state.copy(providerConfigs = state.providerConfigs.mapIndexed { i, config ->
                if (isCustom && i == customIndex && config.provider == AIProvider.CUSTOM) config.copy(apiKey = apiKey)
                else if (!isCustom && config.provider == provider) config.copy(apiKey = apiKey)
                else config
            })
        }
        viewModelScope.launch {
            try {
                if (isCustom) {
                    val existing = settingsDataStore.customProviders.first()
                    if (customIndex < existing.size) {
                        settingsDataStore.updateCustomProvider(customIndex, existing[customIndex].copy(apiKey = apiKey))
                    }
                } else {
                    settingsDataStore.saveProviderConfig(
                        settingsDataStore.getProviderConfig(provider).copy(apiKey = apiKey)
                    )
                }
            } catch (_: Exception) { }
        }
    }

    private fun updateBaseUrl(provider: AIProvider, baseUrl: String, customIndex: Int) {
        val isCustom = provider == AIProvider.CUSTOM && customIndex >= 0
        _uiState.update { state ->
            state.copy(providerConfigs = state.providerConfigs.mapIndexed { i, config ->
                if (isCustom && i == customIndex && config.provider == AIProvider.CUSTOM) config.copy(baseUrl = baseUrl)
                else if (!isCustom && config.provider == provider) config.copy(baseUrl = baseUrl)
                else config
            })
        }
        viewModelScope.launch {
            try {
                if (isCustom) {
                    val existing = settingsDataStore.customProviders.first()
                    if (customIndex < existing.size) {
                        settingsDataStore.updateCustomProvider(customIndex, existing[customIndex].copy(baseUrl = baseUrl))
                    }
                } else {
                    settingsDataStore.saveProviderConfig(
                        settingsDataStore.getProviderConfig(provider).copy(baseUrl = baseUrl)
                    )
                }
            } catch (_: Exception) { }
        }
    }

    private fun toggleEnabled(provider: AIProvider, customIndex: Int) {
        val isCustom = provider == AIProvider.CUSTOM && customIndex >= 0
        _uiState.update { state ->
            state.copy(providerConfigs = state.providerConfigs.mapIndexed { i, config ->
                if (isCustom && i == customIndex && config.provider == AIProvider.CUSTOM) config.copy(isEnabled = !config.isEnabled)
                else if (!isCustom && config.provider == provider) config.copy(isEnabled = !config.isEnabled)
                else config
            })
        }
        viewModelScope.launch {
            try {
                if (isCustom) {
                    val existing = settingsDataStore.customProviders.first()
                    if (customIndex < existing.size) {
                        settingsDataStore.updateCustomProvider(customIndex, existing[customIndex].copy(isEnabled = !existing[customIndex].isEnabled))
                    }
                } else {
                    val current = settingsDataStore.getProviderConfig(provider)
                    settingsDataStore.saveProviderConfig(current.copy(isEnabled = !current.isEnabled))
                }
            } catch (_: Exception) { }
        }
    }

    private fun testConnection(provider: AIProvider) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                testResults = _uiState.value.testResults + (provider to "测试中...")
            )
            val config = settingsDataStore.getProviderConfig(provider)
            val result = connectionTester.testConnection(provider, config.apiKey, config.baseUrl)
            _uiState.value = _uiState.value.copy(
                testResults = _uiState.value.testResults + (provider to result)
            )
        }
    }

}
