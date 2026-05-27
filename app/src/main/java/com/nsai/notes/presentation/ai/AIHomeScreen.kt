package com.nsai.notes.presentation.ai

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.domain.model.AIMode
import com.nsai.notes.presentation.ai.components.AIContent
import com.nsai.notes.presentation.ai.components.ConversationHistoryDrawer
import com.nsai.notes.presentation.ai.components.CollapsibleTopBar
import com.nsai.notes.presentation.ai.components.MoreModesSheet
import com.nsai.notes.presentation.ai.components.WorkspaceBar
import com.nsai.notes.presentation.ai.components.WorkspaceTab
import com.nsai.notes.presentation.theme.LocalAnimationConfig

/**
 * AI主界面 - 精简状态管理层
 * 所有UI组件已提取到独立文件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIHomeScreen(
    onNavigateToNoteChat: (Long) -> Unit,
    onNavigateToModelSettings: () -> Unit,
    onNavigateToMCPSkill: () -> Unit = {},
    onNavigateToActivation: () -> Unit = {},
    onExitAI: () -> Unit = {},
    viewModel: AIHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // 浏览器对话框状态
    var showBrowser by remember { mutableStateOf(false) }
    var browserUrl by remember { mutableStateOf("https://www.google.com") }

    // 更多模式底部sheet
    var showMoreSheet by remember { mutableStateOf(false) }

    // 当前工作区标签
    val currentTab = when {
        uiState.isAgentMode -> WorkspaceTab.AGENT
        uiState.isRagMode -> WorkspaceTab.RAG
        else -> WorkspaceTab.CHAT
    }

    // 是否有对话内容
    val hasConversation = uiState.messages.size > 1

    // 自动滚动到底部
    LaunchedEffect(uiState.messages.size) {
        // 滚动逻辑由ChatContainer内部的LazyListState处理
    }

    // 错误提示
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(AIHomeEvent.ClearError)
        }
    }

    // 更多模式底部Sheet
    if (showMoreSheet) {
        MoreModesSheet(
            onDismiss = { showMoreSheet = false },
            onSelectAgent = { viewModel.onEvent(AIHomeEvent.ToggleAgentMode) },
            onSelectRag = { viewModel.onEvent(AIHomeEvent.ToggleRagMode) },
            onSelectImage = { viewModel.onEvent(AIHomeEvent.SelectMode(AIMode.IMAGE)) },
            onSelectDocGen = { viewModel.onEvent(AIHomeEvent.ToggleDocGenMode) }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsibleTopBar(
                selectedProvider = uiState.selectedProvider,
                onProviderSelected = { provider ->
                    viewModel.onEvent(AIHomeEvent.SelectProvider(provider))
                },
                onHistoryClick = { viewModel.onEvent(AIHomeEvent.ToggleHistory) },
                onSettingsClick = onNavigateToModelSettings,
                onMCPSkillClick = onNavigateToMCPSkill,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // 主内容区
            AIContent(
                showChat = hasConversation || uiState.isLoading,
                messages = uiState.messages,
                isLoading = uiState.isLoading,
                searchResults = uiState.searchResults,
                generatedImage = uiState.generatedImage,
                onUrlClick = { url ->
                    browserUrl = url
                    showBrowser = true
                },
                onSaveAsNote = { content ->
                    viewModel.onEvent(AIHomeEvent.SaveAsNote(content))
                },
                onRetry = { viewModel.onEvent(AIHomeEvent.SendMessage) },
                onSuggestion = { prompt ->
                    viewModel.onEvent(AIHomeEvent.UpdateInput(prompt))
                    viewModel.onEvent(AIHomeEvent.SendMessage)
                },
                notes = uiState.recentNotes,
                onNoteClick = onNavigateToNoteChat,
                modifier = Modifier.weight(1f)
            )

            // 底部工作区
            val isImageMode = uiState.currentMode == AIMode.IMAGE
            WorkspaceBar(
                currentTab = currentTab,
                onSelectTab = { tab ->
                    when (tab) {
                        WorkspaceTab.CHAT -> viewModel.onEvent(AIHomeEvent.SelectMode(AIMode.QUICK))
                        WorkspaceTab.AGENT -> viewModel.onEvent(AIHomeEvent.ToggleAgentMode)
                        WorkspaceTab.RAG -> viewModel.onEvent(AIHomeEvent.ToggleRagMode)
                    }
                },
                isImageMode = isImageMode,
                inputText = uiState.inputText,
                imagePrompt = uiState.imagePrompt,
                onTextChange = { viewModel.onEvent(AIHomeEvent.UpdateInput(it)) },
                onImagePromptChange = { viewModel.onEvent(AIHomeEvent.UpdateImagePrompt(it)) },
                onSend = {
                    if (isImageMode) {
                        viewModel.onEvent(AIHomeEvent.GenerateImage)
                    } else {
                        viewModel.onEvent(AIHomeEvent.SendMessage)
                    }
                },
                isLoading = uiState.isLoading,
                isWebSearchEnabled = uiState.isWebSearchMode,
                onToggleWebSearch = { viewModel.onEvent(AIHomeEvent.ToggleWebSearch) },
                contextLabel = contextLabel(uiState),
                onMoreClick = { showMoreSheet = true },
                onExitAI = onExitAI
            )
        }
    }

    // 历史抽屉
    ConversationHistoryDrawer(
        visible = uiState.showHistory,
        conversations = uiState.conversationHistory,
        currentId = uiState.currentConversationId,
        onSelect = { viewModel.onEvent(AIHomeEvent.LoadConversation(it)) },
        onDelete = { viewModel.onEvent(AIHomeEvent.DeleteConversation(it)) },
        onNew = { viewModel.onEvent(AIHomeEvent.NewConversation) },
        onDismiss = { viewModel.onEvent(AIHomeEvent.ToggleHistory) }
    )

    // 浏览器对话框
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
            onAddSearchHistory = { query -> viewModel.onEvent(AIHomeEvent.AddSearchHistory(query)) },
            onClearSearchHistory = { viewModel.onEvent(AIHomeEvent.ClearSearchHistory) },
            onDismiss = { showBrowser = false }
        )
    }
}

/**
 * 构建上下文标签文本
 */
private fun contextLabel(state: AIHomeUiState): String = buildString {
    val provider = state.selectedProvider.displayName
    append(provider)
    if (state.isWebSearchMode) append(" · 联网搜索")
    if (state.currentMode == AIMode.THINK) append(" · 思考模式")
    if (state.currentMode == AIMode.IMAGE) append(" · 图片生成")
    if (state.isAgentMode) append(" · 工具: 8个可用")
    if (state.isRagMode) append(" · 知识库检索")
}
