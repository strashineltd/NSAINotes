package com.nsai.notes.presentation.memory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsai.notes.data.local.memory.MemoryDao
import com.nsai.notes.data.local.memory.MemoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewViewModel @Inject constructor(
    private val memoryDao: MemoryDao
) : ViewModel() {

    data class UiState(val memories: List<MemoryEntity> = emptyList())
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            memoryDao.getAllFlow().collect { memories ->
                _uiState.value = UiState(memories)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { memoryDao.delete(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryViewScreen(onNavigateBack: () -> Unit) {
    val viewModel: MemoryViewViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("AI 记忆管理") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) }
    ) { padding ->
        val grouped = uiState.memories.groupBy { it.type }
        if (uiState.memories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无记忆\nAI将在对话中自动提取重要信息", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                grouped.forEach { (type, memories) ->
                    item(key = "header_$type") { Text(typeDisplayName(type), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
                    items(memories, key = { it.id }) { memory ->
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
                                Icon(typeIcon(type), null, Modifier.size(20.dp).padding(top = 2.dp), tint = typeColor(type))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(memory.key, style = MaterialTheme.typography.bodyMedium)
                                    Text(memory.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                IconButton(onClick = { viewModel.delete(memory.id) }, Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, "删除", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

private fun typeDisplayName(type: String) = when (type) { "PREFERENCE" -> "偏好"; "FACT" -> "事实"; "CONVERSATION" -> "对话摘要"; "DATE" -> "重要日期"; "RELATIONSHIP" -> "关联"; else -> type }
private fun typeIcon(type: String) = when (type) { "PREFERENCE" -> Icons.Default.Star; "FACT" -> Icons.Default.Info; "CONVERSATION" -> Icons.Default.Chat; "DATE" -> Icons.Default.DateRange; "RELATIONSHIP" -> Icons.Default.Link; else -> Icons.Default.Circle }
private fun typeColor(type: String) = when (type) { "PREFERENCE" -> Color(0xFFFFB300); "FACT" -> Color(0xFF4CAF50); "CONVERSATION" -> Color(0xFF2196F3); "DATE" -> Color(0xFFFF5722); "RELATIONSHIP" -> Color(0xFF9C27B0); else -> Color.Gray }
