package com.nsai.notes.presentation.ai

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nsai.notes.domain.model.AIMode
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.model.Conversation
import com.nsai.notes.domain.model.Note
import com.nsai.notes.presentation.ai.components.AIReplyActions
import com.nsai.notes.presentation.ai.components.AgentStepCard
import com.nsai.notes.presentation.ai.components.WorkspaceTab
import com.nsai.notes.presentation.ai.components.WorkspaceSegmentedSwitch
import com.nsai.notes.presentation.ai.components.CollapsibleTopBar
import com.nsai.notes.presentation.ai.components.ElevatedInputIsland
import com.nsai.notes.presentation.ai.components.SuggestionGrid
import com.nsai.notes.presentation.ai.components.Suggestion
import com.nsai.notes.presentation.theme.LocalAnimationConfig

private val suggestions = listOf(
    Suggestion(Icons.Default.Lightbulb, "创意灵感", "给我一些创意写作的灵感", listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))),
    Suggestion(Icons.Default.Search, "知识问答", "解释一下量子计算的基本原理", listOf(Color(0xFF4ECDC4), Color(0xFF44B09E))),
    Suggestion(Icons.Default.Code, "代码助手", "用Kotlin写一个快速排序算法", listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE))),
    Suggestion(Icons.Default.Translate, "翻译专家", "把这段文字翻译成英文", listOf(Color(0xFF0984E3), Color(0xFF74B9FF))),
    Suggestion(Icons.Default.Description, "文档生成", "帮我写一份项目周报模板", listOf(Color(0xFF00B894), Color(0xFF55EFC4))),
    Suggestion(Icons.Default.Brush, "内容润色", "帮我润色这段文字使其更流畅", listOf(Color(0xFFFD79A8), Color(0xFFFDCB6E)))
)

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
    val tokens = LocalAnimationConfig.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val chatListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showBrowser by remember { mutableStateOf(false) }
    var browserUrl by remember { mutableStateOf("https://www.google.com") }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    val hasConversation = uiState.messages.size > 1
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val bottomThresholdPx = with(density) { config.screenHeightDp.dp.toPx() * 0.75f }
    var dragStartY by remember { mutableFloatStateOf(0f) }
    
    // 可折叠顶栏滚动行为
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            val lastVisible = chatListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisible >= uiState.messages.size - 3) {
                chatListState.scrollToItem(uiState.messages.size - 1)
            }
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.onEvent(AIHomeEvent.ClearError) }
    }

    // "+" more modes bottom sheet
    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("Agent") },
                    supportingContent = { Text("AI 多步推理，调用工具完成任务") },
                    leadingContent = { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        viewModel.onEvent(AIHomeEvent.ToggleAgentMode)
                    }
                )
                ListItem(
                    headlineContent = { Text("知识库搜索") },
                    supportingContent = { Text("从你的笔记中检索相关内容辅助回答") },
                    leadingContent = { Icon(Icons.Default.Search, null, tint = Color(0xFF10A37F)) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        viewModel.onEvent(AIHomeEvent.ToggleRagMode)
                    }
                )
                ListItem(
                    headlineContent = { Text("图片生成") },
                    supportingContent = { Text("输入描述，AI 生成图片") },
                    leadingContent = { Icon(Icons.Default.Image, null, tint = Color(0xFFE91E63)) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        viewModel.onEvent(AIHomeEvent.SelectMode(AIMode.IMAGE))
                    }
                )
                ListItem(
                    headlineContent = { Text("文档生成") },
                    supportingContent = { Text("AI 生成完整文档并自动保存为笔记") },
                    leadingContent = { Icon(Icons.Default.Description, null, tint = Color(0xFF0070F3)) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        viewModel.onEvent(AIHomeEvent.ToggleDocGenMode)
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> dragStartY = offset.y },
                    onVerticalDrag = { _, _ -> },
                    onDragEnd = {
                        if (dragStartY > bottomThresholdPx) onExitAI()
                    }
                )
            },
        topBar = {
            CollapsibleTopBar(
                selectedProvider = uiState.selectedProvider,
                onProviderSelected = { provider -> viewModel.onEvent(AIHomeEvent.SelectProvider(provider)) },
                onHistoryClick = { viewModel.onEvent(AIHomeEvent.ToggleHistory) },
                onSettingsClick = onNavigateToModelSettings,
                onMCPSkillClick = onNavigateToMCPSkill,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Main content area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = hasConversation || uiState.isLoading,
                    transitionSpec = { fadeIn(tween(tokens.fastDuration)) togetherWith fadeOut(tween(tokens.fastDuration)) },
                    label = "content"
                ) { showChat ->
                    if (showChat) {
                        ChatView(
                            messages = uiState.messages, isLoading = uiState.isLoading,
                            listState = chatListState, searchResults = uiState.searchResults,
                            generatedImage = uiState.generatedImage,
                            onUrlClick = { browserUrl = it; showBrowser = true },
                            onSaveAsNote = { content -> viewModel.onEvent(AIHomeEvent.SaveAsNote(content)) },
                            onRetry = { viewModel.onEvent(AIHomeEvent.SendMessage) }
                        )
                    } else {
                        WelcomeView(
                            onSuggestion = { viewModel.onEvent(AIHomeEvent.UpdateInput(it)); viewModel.onEvent(AIHomeEvent.SendMessage) },
                            notes = uiState.recentNotes, onNoteClick = onNavigateToNoteChat
                        )
                    }
                }
            }

            // Workspace segmented switch
            val currentTab = when {
                uiState.isAgentMode -> WorkspaceTab.AGENT
                uiState.isRagMode -> WorkspaceTab.RAG
                else -> WorkspaceTab.CHAT
            }
            WorkspaceSegmentedSwitch(
                currentTab = currentTab,
                onSelectTab = { tab ->
                    when (tab) {
                        WorkspaceTab.CHAT -> viewModel.onEvent(AIHomeEvent.SelectMode(AIMode.QUICK))
                        WorkspaceTab.AGENT -> viewModel.onEvent(AIHomeEvent.ToggleAgentMode)
                        WorkspaceTab.RAG -> viewModel.onEvent(AIHomeEvent.ToggleRagMode)
                    }
                }
            )
            
            // "+" more button + context inline
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val ctxLabel = contextLabel(uiState)
                if (ctxLabel.isNotEmpty()) {
                    Text(ctxLabel, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else { Spacer(Modifier.width(1.dp)) }
                TextButton(onClick = { showMoreSheet = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    Icon(Icons.Default.MoreHoriz, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("更多", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Exit AI button
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onExitAI) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "退出 AI 模式",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Elevated Input Island
            val isImageMode = uiState.currentMode == AIMode.IMAGE
            ElevatedInputIsland(
                text = if (isImageMode) uiState.imagePrompt else uiState.inputText,
                onTextChange = { if (isImageMode) viewModel.onEvent(AIHomeEvent.UpdateImagePrompt(it)) else viewModel.onEvent(AIHomeEvent.UpdateInput(it)) },
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
                placeholder = when (currentTab) {
                    WorkspaceTab.AGENT -> "输入任务指令..."
                    WorkspaceTab.RAG -> "输入问题，从笔记中检索..."
                    else -> if (isImageMode) "输入图片描述..." else "输入消息..."
                }
            )
        }
    }

    // History panel (after Scaffold for z-order)
    AnimatedVisibility(
        visible = uiState.showHistory,
        enter = slideInVertically(tween(tokens.fastDuration)) { -it } + fadeIn(tween(tokens.fastDuration)),
        exit = slideOutVertically(tween(tokens.fastDuration)) { -it } + fadeOut(tween(tokens.fastDuration))
    ) {
        ConversationHistoryPanel(
            conversations = uiState.conversationHistory, currentId = uiState.currentConversationId,
            onSelect = { viewModel.onEvent(AIHomeEvent.LoadConversation(it)) },
            onDelete = { viewModel.onEvent(AIHomeEvent.DeleteConversation(it)) },
            onNew = { viewModel.onEvent(AIHomeEvent.NewConversation) },
            onDismiss = { viewModel.onEvent(AIHomeEvent.ToggleHistory) }
        )
    }

    // Upgrade dialog — temporarily removed
    /* if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = { Text("套餐购买或激活", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text("AI功能需要激活后才能使用")
                    Spacer(Modifier.height(8.dp))
                    Text("💬 AI智能对话", style = MaterialTheme.typography.bodyMedium)
                    Text("🤖 Agent多步推理", style = MaterialTheme.typography.bodyMedium)
                    Text("📚 知识库检索", style = MaterialTheme.typography.bodyMedium)
                    Text("🎨 AI图片生成", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("¥10/年起 · 绑定设备", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showUpgradeDialog = false
                    onNavigateToActivation()
                }) { Text("套餐购买") }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) { Text("取消") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    } */

    // Browser overlay
    if (showBrowser) WebBrowserDialog(
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

private fun contextLabel(state: AIHomeUiState): String = buildString {
    val provider = state.selectedProvider.displayName
    append("$provider")
    if (state.isWebSearchMode) append(" · 联网搜索")
    if (state.currentMode == AIMode.THINK) append(" · 思考模式")
    if (state.currentMode == AIMode.IMAGE) append(" · 图片生成")
    if (state.isAgentMode) append(" · 工具: 8个可用")
    if (state.isRagMode) append(" · 知识库检索")
}

// ─── Welcome View ───
@Composable
private fun WelcomeView(onSuggestion: (String) -> Unit, notes: List<Note>, onNoteClick: (Long) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("你好，今天需要什么帮助？", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(14.dp))
        }
        item {
            SuggestionGrid(
                suggestions = suggestions,
                onSuggestionClick = onSuggestion
            )
        }
        if (notes.isNotEmpty()) {
            item { HorizontalDivider(modifier = Modifier.padding(top = 4.dp)) }
            item { Text("最近的笔记", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
            items(notes, key = { it.id }) { note ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNoteClick(note.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(note.title.ifBlank { "无标题" }, style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(note.content.take(60).replace("\n", " "), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// ─── Chat View ───
@Composable
private fun ChatView(
    messages: List<ChatMessage>, isLoading: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    searchResults: List<com.nsai.notes.data.remote.search.SearchResult>,
    generatedImage: String?,
    onUrlClick: (String) -> Unit,
    onSaveAsNote: (String) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val tokens = LocalAnimationConfig.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(), state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
            items(messages, key = { it.timestamp }, contentType = { it.role }) { msg ->
            val lastAiMsg = messages.lastOrNull { it.role == ChatMessage.Role.ASSISTANT }
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(tokens.normalDuration)) +
                    slideInVertically(tween(tokens.normalDuration)) { it / 4 },
            ) {
                ChatBubble(msg, isLastAIMessage = msg == lastAiMsg, onUrlClick,
                onSaveAsNote = { content -> onSaveAsNote(content) },
                onRetry = { onRetry() })
            }
        }
        if (generatedImage != null) {
            item(key = "generated_image") {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    AsyncImage(generatedImage, null, Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop)
                }
            }
        }
        if (searchResults.isNotEmpty()) {
            item(key = "search_results") {
                SearchResultsCard(
                    query = messages.lastOrNull { it.role == ChatMessage.Role.USER }?.content ?: "",
                    results = searchResults, onResultClick = onUrlClick
                )
            }
        }
        if (isLoading) {
            item(key = "thinking") {
                Row(modifier = Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ThinkingDots()
                    Spacer(Modifier.width(8.dp))
                    Text("思考中...", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun ThinkingDots(modifier: Modifier = Modifier) {
    val tokens = LocalAnimationConfig.current
    val transition = rememberInfiniteTransition(label = "dots")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(0, 150, 300).forEach { d ->
            val scale by transition.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(tokens.normalDuration * 2, delayMillis = d), RepeatMode.Reverse),
                label = "dot$d"
            )
            Box(Modifier.size(5.dp).scale(scale).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
        }
    }
}

// ─── Chat Bubble ───
@Composable
private fun ChatBubble(message: ChatMessage, isLastAIMessage: Boolean = false, onUrlClick: (String) -> Unit = {}, onSaveAsNote: (String) -> Unit = {}, onRetry: () -> Unit = {}) {
    val tokens = LocalAnimationConfig.current
    val isUser = message.role == ChatMessage.Role.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val shape = if (isUser) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    else RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    val elevation = if (isUser) 2.dp else 1.dp

    // Typewriter effect for AI messages — frame-synced for smooth rendering
    val initialChars = if (isUser || !isLastAIMessage) message.content.length else 1
    var displayedChars by remember(message.content) { mutableStateOf(initialChars) }
    if (!isUser && isLastAIMessage) {
        LaunchedEffect(message.content) {
            val targetLength = message.content.length
            val charsPerSecond = 120
            var lastFrameTime = 0L
            while (displayedChars < targetLength) {
                val frameTime = withFrameNanos { it }
                if (lastFrameTime > 0) {
                    val deltaSecs = (frameTime - lastFrameTime) / 1_000_000_000f
                    val charsToAdd = (deltaSecs * charsPerSecond).toInt().coerceAtLeast(1)
                    displayedChars = (displayedChars + charsToAdd).coerceAtMost(targetLength)
                }
                lastFrameTime = frameTime
            }
        }
    }
    val typewriterDone = displayedChars >= message.content.length

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalAlignment = alignment) {
        // AI 头像 + 名称 + 时间
        if (!isUser) {
            Row(
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI 头像
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "AI 助手",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = bg),
            shape = shape,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
        ) {
            Column {
                message.reasoningContent?.let { reasoning ->
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        Modifier.clickable { expanded = !expanded }.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Psychology, null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        Spacer(Modifier.width(6.dp))
                        Text("思考过程", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f))
                        Text(if (expanded) "收起" else "展开", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    }
                    AnimatedVisibility(expanded,
                        enter = expandVertically(tween(tokens.fastDuration)) + fadeIn(tween(tokens.fastDuration)),
                        exit = shrinkVertically(tween(tokens.fastDuration)) + fadeOut(tween(tokens.fastDuration))
                    ) {
                        Text(reasoning,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
                if (isUser || typewriterDone) {
                    val urlText = remember(message.content) { buildUrlText(message.content) }
                    androidx.compose.foundation.text.ClickableText(
                        text = urlText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        onClick = { o -> com.nsai.notes.presentation.ai.getUrlAt(message.content, o)?.let { onUrlClick(it) } }
                    )
                } else {
                    Text(
                        message.content.take(displayedChars),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
        // Reply actions for AI messages
        if (!isUser && message.content.isNotBlank()) {
            AIReplyActions(
                content = message.content,
                onSaveAsNote = { onSaveAsNote(message.content) },
                onRetry = onRetry
            )
        }
    }
}

// ─── URL text helpers ───
private fun buildUrlText(text: String) = buildUrlAnnotatedText(text)

// ─── Conversation History Panel ───
@Composable
private fun ConversationHistoryPanel(
    conversations: List<Conversation>, currentId: Long,
    onSelect: (Conversation) -> Unit, onDelete: (Long) -> Unit,
    onNew: () -> Unit, onDismiss: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { onDismiss() }) {
        Card(
            Modifier.align(Alignment.TopStart).fillMaxWidth(0.82f).fillMaxSize().clickable(enabled = false) {},
            shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("对话历史", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onNew) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp)); Text("新建")
                    }
                }
                if (conversations.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无对话记录", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                } else LazyColumn(Modifier.weight(1f)) {
                    items(conversations, key = { it.id }) { conv ->
                        Card(
                            Modifier.animateItem().fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable { onSelect(conv) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (conv.id == currentId)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surface)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(conv.title, style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (conv.id == currentId) FontWeight.Bold else FontWeight.Normal)
                                    Spacer(Modifier.height(2.dp))
                                    Text("${conv.messages.size} 条消息 · ${java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(conv.updatedAt))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { onDelete(conv.id) }, Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, "删除", Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Search Results Card ───
@Composable
private fun SearchResultsCard(
    query: String, results: List<com.nsai.notes.data.remote.search.SearchResult>, onResultClick: (String) -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TravelExplore, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("搜索: $query", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            results.take(5).forEach { r ->
                Row(Modifier.fillMaxWidth().clickable { onResultClick(r.url) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top) {
                    AsyncImage(
                        r.iconUrl ?: "https://www.google.com/s2/favicons?domain=${java.net.URI(r.url).host}",
                        null, Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(r.snippet.take(80), style = MaterialTheme.typography.bodySmall, maxLines = 2,
                            overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    }
                }
            }
        }
    }
}
