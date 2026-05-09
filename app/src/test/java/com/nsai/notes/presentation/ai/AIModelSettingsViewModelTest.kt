package com.nsai.notes.presentation.ai

import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.data.remote.ai.ConnectionTester
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.AIProviderConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class AIModelSettingsViewModelTest {

    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var connectionTester: ConnectionTester
    private lateinit var viewModel: AIModelSettingsViewModel

    @Before
    fun setup() {
        settingsDataStore = mock()
        connectionTester = mock()
        viewModel = AIModelSettingsViewModel(settingsDataStore, connectionTester)
    }

    @Test
    fun `load should populate provider configs`() = runTest {
        val configs = AIProvider.entries.map {
            AIProviderConfig(provider = it, apiKey = "", baseUrl = it.defaultBaseUrl, isEnabled = false)
        }
        `when`(settingsDataStore.getAllProviderConfigs()).thenReturn(flowOf(configs))
        `when`(settingsDataStore.getProviderConfig(any())).thenAnswer { inv ->
            val provider = inv.getArgument<AIProvider>(0)
            configs.first { it.provider == provider }
        }

        viewModel.load()

        val state = viewModel.uiState.first { it.providerConfigs.isNotEmpty() }
        assertEquals(AIProvider.entries.size, state.providerConfigs.size)
        assertTrue(state.providerConfigs.all { it.apiKey.isEmpty() })
    }

    @Test
    fun `toggleEnabled should update state optimistically`() = runTest {
        val deepSeekConfig = AIProviderConfig(provider = AIProvider.DEEPSEEK)
        `when`(settingsDataStore.getAllProviderConfigs()).thenReturn(
            flowOf(listOf(deepSeekConfig))
        )
        `when`(settingsDataStore.getProviderConfig(AIProvider.DEEPSEEK)).thenReturn(deepSeekConfig)

        viewModel.load()
        viewModel.onEvent(AIModelSettingsEvent.ToggleEnabled(AIProvider.DEEPSEEK))

        val state = viewModel.uiState.first { it.providerConfigs.isNotEmpty() }
        assertEquals(true, state.providerConfigs.first { it.provider == AIProvider.DEEPSEEK }.isEnabled)
        verify(settingsDataStore).saveProviderConfig(
            eq(deepSeekConfig.copy(isEnabled = true))
        )
    }

    @Test
    fun `updateApiKey should update state optimistically`() = runTest {
        val config = AIProviderConfig(provider = AIProvider.DEEPSEEK)
        `when`(settingsDataStore.getAllProviderConfigs()).thenReturn(flowOf(listOf(config)))
        `when`(settingsDataStore.getProviderConfig(AIProvider.DEEPSEEK)).thenReturn(config)

        viewModel.load()
        viewModel.onEvent(AIModelSettingsEvent.UpdateApiKey(AIProvider.DEEPSEEK, "sk-test"))

        val state = viewModel.uiState.first { it.providerConfigs.isNotEmpty() }
        assertEquals("sk-test", state.providerConfigs.first { it.provider == AIProvider.DEEPSEEK }.apiKey)
    }

    @Test
    fun `testConnection should update test result`() = runTest {
        val config = AIProviderConfig(provider = AIProvider.DEEPSEEK, apiKey = "sk-test")
        `when`(settingsDataStore.getAllProviderConfigs()).thenReturn(flowOf(listOf(config)))
        `when`(settingsDataStore.getProviderConfig(AIProvider.DEEPSEEK)).thenReturn(config)
        `when`(connectionTester.testConnection(AIProvider.DEEPSEEK, "sk-test", config.baseUrl))
            .thenReturn("连接成功 ✓")

        viewModel.load()
        viewModel.onEvent(AIModelSettingsEvent.TestConnection(AIProvider.DEEPSEEK))

        val state = viewModel.uiState.first { it.testResults.isNotEmpty() }
        assertEquals("连接成功 ✓", state.testResults[AIProvider.DEEPSEEK])
    }
}
