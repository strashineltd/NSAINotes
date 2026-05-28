package com.nsai.notes.presentation.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.presentation.ai.components.AIContent
import com.nsai.notes.presentation.ai.components.ConversationHistoryDrawer
import com.nsai.notes.presentation.ai.components.FlowInputBar
import com.nsai.notes.presentation.ai.components.FlowTab
import com.nsai.notes.presentation.ai.components.FlowTopBar
import com.nsai.notes.presentation.ai.components.SettingsSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIHomeScreen(
    onNavigateToNoteChat: (Long) -> Unit,
    onNavigateToModelSettings: () -> Unit,
    onNavigateToMCPSkill: () -> Unit = {},
    onNavigateToActivation: () -> Unit = {},
    viewModel: AIHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBrowser by remember { mutableStateOf(false) }
    var browserUrl by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    val selectedTab = when {
        uiState.isAgentMode -> FlowTab.AGENT
        uiState.isRagMode -> FlowTab.RAG
        else -> FlowTab.CHAT
    }

    val hasConversation = uiState.messages.size > 1

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(AIHomeEvent.ClearError)
        }
    }

    if (showSettings) {
        SettingsSheet(
            onDismiss = { showSettings = false },
            selectedProvider = uiState.selectedProvider,
            onProviderChange = { viewModel.onEvent(AIHomeEvent.SelectProvider(it)) },
            providerConfigs = uiState.providerConfigs,
            onUpdateApiKey = { provider, key -> viewModel.onEvent(AIHomeEvent.UpdateApiKey(provider, key)) },
            onUpdateBaseUrl = { provider, url -> viewModel.onEvent(AIHomeEvent.UpdateBaseUrl(provider, url)) },
            onTestConnection = { provider -> viewModel.onEvent(AIHomeEvent.TestConnection(provider)) },
            testResults = uiState.testResults,
            searchEngine = uiState.searchEngine,
            onSearchEngineChange = { viewModel.onEvent(AIHomeEvent.SetSearchEngine(it)) },
            onClearHistory = {
                uiState.conversationHistory.forEach { conv ->
                    viewModel.onEvent(AIHomeEvent.DeleteConversation(conv.id))
                }
                showSettings = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            FlowTopBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    when (tab) {
                        FlowTab.CHAT -> {
                            if (uiState.isAgentMode) viewModel.onEvent(AIHomeEvent.ToggleAgentMode)
                            if (uiState.isRagMode) viewModel.onEvent(AIHomeEvent.ToggleRagMode)
                        }
                        FlowTab.AGENT -> {
                            if (!uiState.isAgentMode) viewModel.onEvent(AIHomeEvent.ToggleAgentMode)
                        }
                        FlowTab.RAG -> {
                            if (!uiState.isRagMode) viewModel.onEvent(AIHomeEvent.ToggleRagMode)
                        }
                    }
                },
                onHistoryClick = { viewModel.onEvent(AIHomeEvent.ToggleHistory) },
                onSettingsClick = { showSettings = true }
            )

            AIContent(
                showChat = hasConversation,
                messages = uiState.messages,
                isLoading = uiState.isLoading,
                searchResults = uiState.searchResults,
                generatedImage = uiState.generatedImage,
                onUrlClick = { url ->
                    browserUrl = url
                    showBrowser = true
                },
                onSaveAsNote = { content -> viewModel.onEvent(AIHomeEvent.SaveAsNote(content)) },
                onRetry = { viewModel.onEvent(AIHomeEvent.SendMessage) },
                onSuggestion = { prompt ->
                    viewModel.onEvent(AIHomeEvent.UpdateInput(prompt))
                    viewModel.onEvent(AIHomeEvent.SendMessage)
                },
                modifier = Modifier.weight(1f)
            )

            FlowInputBar(
                text = uiState.inputText,
                onTextChange = { viewModel.onEvent(AIHomeEvent.UpdateInput(it)) },
                onSend = {
                    if (!uiState.isLoading) {
                        viewModel.onEvent(AIHomeEvent.SendMessage)
                    }
                },
                isLoading = uiState.isLoading,
                placeholder = when (selectedTab) {
                    FlowTab.AGENT -> "描述你要执行的任务..."
                    FlowTab.RAG -> "搜索笔记..."
                    else -> "输入问题..."
                },
                isWebSearchEnabled = uiState.isWebSearchMode,
                onToggleWebSearch = { viewModel.onEvent(AIHomeEvent.ToggleWebSearch) }
            )
        }
    }

    ConversationHistoryDrawer(
        visible = uiState.showHistory,
        conversations = uiState.conversationHistory,
        currentId = uiState.currentConversationId,
        onSelect = { viewModel.onEvent(AIHomeEvent.LoadConversation(it)) },
        onDelete = { viewModel.onEvent(AIHomeEvent.DeleteConversation(it)) },
        onNew = { viewModel.onEvent(AIHomeEvent.NewConversation) },
        onDismiss = { viewModel.onEvent(AIHomeEvent.ToggleHistory) }
    )

    if (showBrowser) {
        WebBrowserDialog(
            initialUrl = browserUrl,
            searchEngine = uiState.searchEngine,
            searchEngineCustomUrl = uiState.searchEngineCustomUrl,
            bookmarksTitles = uiState.bookmarks.map { it.title },
            bookmarkUrls = uiState.bookmarks.map { it.url },
            searchHistory = uiState.searchHistory,
            onSearchEngineChange = { viewModel.onEvent(AIHomeEvent.SetSearchEngine(it)) },
            onSearchEngineCustomUrlChange = { viewModel.onEvent(AIHomeEvent.SetSearchEngineCustomUrl(it)) },
            onAddBookmark = { title, url -> viewModel.onEvent(AIHomeEvent.AddBookmark(title, url)) },
            onRemoveBookmark = { url -> viewModel.onEvent(AIHomeEvent.RemoveBookmark(url)) },
            onAddSearchHistory = { q -> viewModel.onEvent(AIHomeEvent.AddSearchHistory(q)) },
            onClearSearchHistory = { viewModel.onEvent(AIHomeEvent.ClearSearchHistory) },
            onDismiss = { showBrowser = false }
        )
    }
}
