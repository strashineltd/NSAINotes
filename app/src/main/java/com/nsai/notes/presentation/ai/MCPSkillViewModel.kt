package com.nsai.notes.presentation.ai

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.domain.model.MCPServer
import com.nsai.notes.domain.model.SkillPlugin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Stable
data class MCPSkillUiState(
    val mcpServers: List<MCPServer> = emptyList(),
    val skillPlugins: List<SkillPlugin> = emptyList(),
    val testResults: Map<String, String> = emptyMap(),
    val editingServer: MCPServer? = null,
    val editingSkill: SkillPlugin? = null,
    val activeTab: Int = 0 // 0=MCP, 1=Skill
)

sealed class MCPSkillEvent {
    data object LoadAll : MCPSkillEvent()
    data class SwitchTab(val index: Int) : MCPSkillEvent()
    data class StartEditServer(val server: MCPServer?) : MCPSkillEvent()
    data class StartEditSkill(val skill: SkillPlugin?) : MCPSkillEvent()
    data class SaveServer(val server: MCPServer) : MCPSkillEvent()
    data class SaveSkill(val skill: SkillPlugin) : MCPSkillEvent()
    data class DeleteServer(val id: String) : MCPSkillEvent()
    data class DeleteSkill(val id: String) : MCPSkillEvent()
    data class TestServer(val serverId: String) : MCPSkillEvent()
    data object CancelEdit : MCPSkillEvent()
}

@HiltViewModel
class MCPSkillViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MCPSkillUiState())
    val uiState: StateFlow<MCPSkillUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun onEvent(event: MCPSkillEvent) {
        when (event) {
            MCPSkillEvent.LoadAll -> loadAll()
            is MCPSkillEvent.SwitchTab -> _uiState.value = _uiState.value.copy(activeTab = event.index)
            is MCPSkillEvent.StartEditServer -> _uiState.value = _uiState.value.copy(editingServer = event.server)
            is MCPSkillEvent.StartEditSkill -> _uiState.value = _uiState.value.copy(editingSkill = event.skill)
            is MCPSkillEvent.SaveServer -> saveServer(event.server)
            is MCPSkillEvent.SaveSkill -> saveSkill(event.skill)
            is MCPSkillEvent.DeleteServer -> deleteServer(event.id)
            is MCPSkillEvent.DeleteSkill -> deleteSkill(event.id)
            is MCPSkillEvent.TestServer -> testServer(event.serverId)
            MCPSkillEvent.CancelEdit -> _uiState.value = _uiState.value.copy(editingServer = null, editingSkill = null)
        }
    }

    private fun loadAll() {
        viewModelScope.launch {
            val servers = settingsDataStore.mcpServers.first()
            val skills = settingsDataStore.skillPlugins.first()
            _uiState.value = _uiState.value.copy(mcpServers = servers, skillPlugins = skills)
        }
    }

    private fun saveServer(server: MCPServer) {
        viewModelScope.launch {
            val existing = _uiState.value.mcpServers.find { it.id == server.id }
            if (existing != null) settingsDataStore.updateMCPServer(server)
            else settingsDataStore.addMCPServer(server)
            _uiState.value = _uiState.value.copy(editingServer = null)
            loadAll()
        }
    }

    private fun saveSkill(skill: SkillPlugin) {
        viewModelScope.launch {
            val existing = _uiState.value.skillPlugins.find { it.id == skill.id }
            if (existing != null) settingsDataStore.updateSkillPlugin(skill)
            else settingsDataStore.addSkillPlugin(skill)
            _uiState.value = _uiState.value.copy(editingSkill = null)
            loadAll()
        }
    }

    private fun deleteServer(id: String) {
        viewModelScope.launch {
            settingsDataStore.deleteMCPServer(id)
            loadAll()
        }
    }

    private fun deleteSkill(id: String) {
        viewModelScope.launch {
            settingsDataStore.deleteSkillPlugin(id)
            loadAll()
        }
    }

    private fun testServer(serverId: String) {
        val server = _uiState.value.mcpServers.find { it.id == serverId } ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                testResults = _uiState.value.testResults + (serverId to "测试中...")
            )
            try {
                val result = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(8, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build()
                    val request = Request.Builder()
                        .url(server.url)
                        .apply { if (server.apiKey.isNotBlank()) addHeader("Authorization", "Bearer ${server.apiKey}") }
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) "连接成功 ✓"
                    else "连接失败 (${response.code})"
                }
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + (serverId to result)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testResults = _uiState.value.testResults + (serverId to "连接失败: ${e.message}")
                )
            }
        }
    }
}
