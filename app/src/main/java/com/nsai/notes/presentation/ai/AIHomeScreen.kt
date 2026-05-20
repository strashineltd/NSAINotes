package com.nsai.notes.presentation.ai

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import com.nsai.notes.presentation.ai.components.WorkspaceTabs
import com.nsai.notes.presentation.theme.LocalAnimationConfig

private data class Suggestion(
    val icon: ImageVector, val title: String, val prompt: String, val gradient: List<Color>
)

private val suggestions = listOf(
    Suggestion(Icons.Default.Lightbulb, "创意灵感", "给我一些创意写作的灵感", listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))),
    Suggestion(Icons.Default.Search, "知识问答", "解释一下量子计算的基本原理", listOf(Color(0xFF4ECDC4), Color(0xFF44B09E))),
    Suggestion(Icons.Default.Code, "代码助手", "用Kotlin写一个快速排序算法", listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE))),
    Suggestion(Icons.Default.Translate, "翻译专家", "把这段文字翻译成英文", listOf(Color(0xFF0984E3), Color(0xFF74B9FF))),
    Suggestion(Icons.Default.Description, "文档生成", "帮我写一份项目周报模板", listOf(Color(0xFF00B894), Color(0xFF55EFC4))),
    Suggestion(Icons.Default.Brush, "内容润色", "帮我润色这段文字使其更流畅", listOf(Color(0xFFFD79A8), Color(0xFFFDCB6E)))
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AIHomeScreen(
    onNavigateToNoteChat: (Long) -> Unit,
    onNavigateToModelSettings: () -> Unit,
    onNavigateToMCPSkill: () -> Unit = {},
    onNavigateToActivation: () -> Unit = {},
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
    var showModelMenu by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    val hasConversation = uiState.messages.size > 1

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
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("NSAI AI", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        Box {
                            Card(
                                modifier = Modifier.clickable { showModelMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            ) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(
                                        if (uiState.selectedProvider.supportsVision) Color(0xFF4CAF50) else Color(0xFFBDBDBD)))
                                    Spacer(Modifier.width(6.dp))
                                    Text(uiState.selectedProvider.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(3.dp))
                                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                }
                            }
                            DropdownMenu(expanded = showModelMenu, onDismissRequest = { showModelMenu = false }) {
                                AIProvider.entries.forEach { provider ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(provider.displayName, style = MaterialTheme.typography.bodyMedium)
                                                if (provider == uiState.selectedProvider) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        },
                                        onClick = { showModelMenu = false; viewModel.onEvent(AIHomeEvent.SelectProvider(provider)) },
                                        leadingIcon = {
                                            val dotColor = if (provider.supportsVision) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
                                            Box(Modifier.size(12.dp).clip(CircleShape).background(dotColor))
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    // Membership badge — temporarily removed
                    /* val licenseActive = viewModel.isLicenseActive()
                    IconButton(onClick = onNavigateToActivation) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, "会员",
                                Modifier.size(22.dp),
                                tint = if (licenseActive) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(
                                Modifier.size(8.dp).align(Alignment.BottomEnd).offset(2.dp, 2.dp)
                                    .clip(CircleShape)
                                    .background(if (licenseActive) Color(0xFF4CAF50) else Color(0xFFBDBDBD))
                            )
                        }
                    } */
                    IconButton(onClick = { viewModel.onEvent(AIHomeEvent.ToggleHistory) }) {
                        Icon(Icons.Default.History, "历史",
                            tint = if (uiState.showHistory) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onNavigateToMCPSkill) {
                        Icon(Icons.Default.Extension, "插件", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onNavigateToModelSettings) {
                        Icon(Icons.Default.Settings, "设置", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

            // Workspace tabs (v1.1)
            val currentTab = when {
                uiState.isAgentMode -> WorkspaceTab.AGENT
                uiState.isRagMode -> WorkspaceTab.RAG
                else -> WorkspaceTab.CHAT
            }
            WorkspaceTabs(currentTab) { tab ->
                when (tab) {
                    WorkspaceTab.CHAT -> viewModel.onEvent(AIHomeEvent.SelectMode(AIMode.QUICK))
                    WorkspaceTab.AGENT -> viewModel.onEvent(AIHomeEvent.ToggleAgentMode)
                    WorkspaceTab.RAG -> viewModel.onEvent(AIHomeEvent.ToggleRagMode)
                }
            }
            
            // Mode selection chips — Quick / Think / Image
            if (!uiState.isAgentMode && !uiState.isRagMode) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = AIMode.QUICK == uiState.currentMode,
                        onClick = { viewModel.onEvent(AIHomeEvent.SelectMode(AIMode.QUICK)) },
                        label = { Text("快速", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                    FilterChip(
                        selected = AIMode.THINK == uiState.currentMode,
                        onClick = { viewModel.onEvent(AIHomeEvent.SelectMode(AIMode.THINK)) },
                        label = { Text("思考", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                    FilterChip(
                        selected = AIMode.IMAGE == uiState.currentMode,
                        onClick = { viewModel.onEvent(AIHomeEvent.SelectMode(AIMode.IMAGE)) },
                        label = { Text("图片", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                    // Web search toggle
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.onEvent(AIHomeEvent.ToggleWebSearch) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.TravelExplore, "联网搜索",
                            Modifier.size(18.dp),
                            tint = if (uiState.isWebSearchMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

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

            // Input bar
            val isImageMode = uiState.currentMode == AIMode.IMAGE
            InputBar(
                text = if (isImageMode) uiState.imagePrompt else uiState.inputText,
                isLoading = uiState.isLoading,
                placeholder = when (currentTab) {
                    WorkspaceTab.AGENT -> "输入任务指令..."
                    WorkspaceTab.RAG -> "输入问题，从笔记中检索..."
                    else -> if (isImageMode) "输入图片描述..." else "输入消息..."
                },
                onTextChange = { if (isImageMode) viewModel.onEvent(AIHomeEvent.UpdateImagePrompt(it)) else viewModel.onEvent(AIHomeEvent.UpdateInput(it)) },
                onSend = {
                    // License check temporarily disabled
                    if (isImageMode) {
                        viewModel.onEvent(AIHomeEvent.GenerateImage)
                    } else {
                        viewModel.onEvent(AIHomeEvent.SendMessage)
                    }
                },
                onBrowser = { browserUrl = ""; showBrowser = true }
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

// ─── Mode Chip ───
@Composable
private fun ModeChip(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val tokens = LocalAnimationConfig.current
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(tokens.fastDuration), label = "chipBg"
    )
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = Modifier.clickable(onClick = onClick), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor), elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(14.dp), tint = contentColor)
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor)
        }
    }
}

// ─── Welcome View ───
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeView(onSuggestion: (String) -> Unit, notes: List<Note>, onNoteClick: (Long) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("你好，今天需要什么帮助？", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(14.dp))
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                suggestions.forEach { s ->
                    Card(
                        modifier = Modifier.clickable { onSuggestion(s.prompt) }, shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.background(s.gradient.first().copy(alpha = 0.08f))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(s.icon, null, Modifier.size(20.dp), tint = s.gradient.first())
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(s.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                Text(s.prompt, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), maxLines = 1)
                            }
                        }
                    }
                }
            }
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(), state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
            items(messages, key = { it.timestamp }) { msg ->
            val lastAiMsg = messages.lastOrNull { it.role == ChatMessage.Role.ASSISTANT }
            Box(Modifier.animateItem()) {
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

    // Typewriter effect for AI messages — only for the last AI message
    val initialChars = if (isUser || !isLastAIMessage) message.content.length else 1
    var displayedChars by remember(message.content) { mutableStateOf(initialChars) }
    if (!isUser && isLastAIMessage) {
        LaunchedEffect(message.content) {
            while (displayedChars < message.content.length) {
                delay(33L)
                displayedChars = (displayedChars + 8).coerceAtMost(message.content.length)
            }
        }
    }
    val typewriterDone = displayedChars >= message.content.length

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalAlignment = alignment) {
        if (!isUser) {
            Text("AI 助手", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
        }
        Card(colors = CardDefaults.cardColors(containerColor = bg), shape = shape, elevation = CardDefaults.cardElevation(0.dp)) {
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

// ─── Input Bar ───
@Composable
private fun InputBar(
    text: String, isLoading: Boolean, onTextChange: (String) -> Unit,
    onSend: () -> Unit, onBrowser: () -> Unit,
    placeholder: String = "输入消息..."
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text, onValueChange = onTextChange, modifier = Modifier.weight(1f),
                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
                singleLine = true, shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                )
            )
            IconButton(onClick = onBrowser, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.TravelExplore, "浏览器", Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val canSend = text.isNotBlank() && !isLoading
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                else IconButton(onClick = onSend, enabled = canSend, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, "发送", Modifier.size(18.dp),
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
            }
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
