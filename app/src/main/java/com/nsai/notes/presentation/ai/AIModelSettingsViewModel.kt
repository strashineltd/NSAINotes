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
    data class UpdateApiKey(val provider: AIProvider, val apiKey: String) : AIModelSettingsEvent()
    data class UpdateBaseUrl(val provider: AIProvider, val baseUrl: String) : AIModelSettingsEvent()
    data class ToggleEnabled(val provider: AIProvider) : AIModelSettingsEvent()
    data class TestConnection(val provider: AIProvider) : AIModelSettingsEvent()
    data object LoadConfigs : AIModelSettingsEvent()
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
            is AIModelSettingsEvent.UpdateApiKey -> updateApiKey(event.provider, event.apiKey)
            is AIModelSettingsEvent.UpdateBaseUrl -> updateBaseUrl(event.provider, event.baseUrl)
            is AIModelSettingsEvent.ToggleEnabled -> toggleEnabled(event.provider)
            is AIModelSettingsEvent.TestConnection -> testConnection(event.provider)
            AIModelSettingsEvent.LoadConfigs -> loadConfigs()
        }
    }

    private fun loadConfigs() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    providerConfigs = settingsDataStore.getAllProviderConfigs().first()
                )
            } catch (_: Exception) {
                // DataStore reads are stable; ignore transient failures
            }
        }
    }

    private fun updateApiKey(provider: AIProvider, apiKey: String) {
        _uiState.update { state ->
            state.copy(providerConfigs = state.providerConfigs.map {
                if (it.provider == provider) it.copy(apiKey = apiKey) else it
            })
        }
        viewModelScope.launch {
            settingsDataStore.saveProviderConfig(
                settingsDataStore.getProviderConfig(provider).copy(apiKey = apiKey)
            )
        }
    }

    private fun updateBaseUrl(provider: AIProvider, baseUrl: String) {
        _uiState.update { state ->
            state.copy(providerConfigs = state.providerConfigs.map {
                if (it.provider == provider) it.copy(baseUrl = baseUrl) else it
            })
        }
        viewModelScope.launch {
            settingsDataStore.saveProviderConfig(
                settingsDataStore.getProviderConfig(provider).copy(baseUrl = baseUrl)
            )
        }
    }

    private fun toggleEnabled(provider: AIProvider) {
        _uiState.update { state ->
            state.copy(providerConfigs = state.providerConfigs.map {
                if (it.provider == provider) it.copy(isEnabled = !it.isEnabled) else it
            })
        }
        viewModelScope.launch {
            val current = settingsDataStore.getProviderConfig(provider)
            settingsDataStore.saveProviderConfig(current.copy(isEnabled = !current.isEnabled))
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
