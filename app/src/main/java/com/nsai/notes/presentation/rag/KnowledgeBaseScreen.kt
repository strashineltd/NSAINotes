package com.nsai.notes.presentation.rag

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsai.notes.data.local.vector.VectorStore
import com.nsai.notes.domain.rag.IndexNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KnowledgeBaseViewModel @Inject constructor(
    private val vectorStore: VectorStore,
    private val indexNotesUseCase: IndexNotesUseCase
) : ViewModel() {

    data class UiState(
        val indexedCount: Int = 0,
        val isRebuilding: Boolean = false,
        val message: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val count = vectorStore.count()
            _uiState.value = _uiState.value.copy(indexedCount = count)
        }
    }

    fun rebuild() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRebuilding = true, message = "正在重建索引...")
            indexNotesUseCase.rebuildAll()
            val count = vectorStore.count()
            _uiState.value = _uiState.value.copy(isRebuilding = false, indexedCount = count, message = "索引重建完成，共 $count 条")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseScreen(onNavigateBack: () -> Unit) {
    val viewModel: KnowledgeBaseViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("知识库管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${uiState.indexedCount}", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("已索引文本片段", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { viewModel.rebuild() }, enabled = !uiState.isRebuilding, shape = RoundedCornerShape(12.dp)) {
                if (uiState.isRebuilding) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (uiState.isRebuilding) "重建中..." else "重建全部索引")
            }
            uiState.message?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(24.dp))
            Text("知识库将你的笔记内容分割为文本片段并向量化存储。\nAI回答时可检索相关片段作为参考上下文。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}
