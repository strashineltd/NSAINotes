package com.nsai.notes.presentation.ai

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.AIMode
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.model.SearchEngine
import com.nsai.notes.presentation.theme.LocalAnimationConfig
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    noteId: Long,
    onNavigateBack: () -> Unit,
    viewModel: AIChatViewModel = hiltViewModel()
) {
    val tokens = LocalAnimationConfig.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var immersive by remember { mutableStateOf(false) }
    var showTopBar by remember { mutableStateOf(true) }
    var showBrowser by remember { mutableStateOf(false) }
    var browserUrl by remember { mutableStateOf(SearchEngine.fromName(uiState.searchEngine).homepage()) }
    var userScrolledUp by remember { mutableStateOf(false) }

    // Detect manual scroll-up
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val total = listState.layoutInfo.totalItemsCount
            lastIndex == null || total <= 1 || lastIndex < total - 2
        }.collect { scrolledUp ->
            if (scrolledUp) userScrolledUp = true
        }
    }

    LaunchedEffect(immersive, showTopBar) {
        if (immersive && showTopBar) { delay(3000L); showTopBar = false }
    }
    LaunchedEffect(noteId) { viewModel.onEvent(AIChatEvent.LoadNote(noteId)) }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.onEvent(AIChatEvent.ClearError) }
    }
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty() && !userScrolledUp) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisible >= uiState.messages.size - 3) {
                listState.scrollToItem(uiState.messages.size - 1)
            }
        }
    }

    if (uiState.isSummarizing || uiState.summary != null) {
        AISummaryDialog(summary = uiState.summary, isSummarizing = uiState.isSummarizing,
            onDismiss = { viewModel.onEvent(AIChatEvent.DismissSummary) })
    }

    Box(modifier = Modifier.fillMaxSize().then(
        if (immersive) Modifier.clickable(indication = null,
            interactionSource = remember { MutableInteractionSource() }) { showTopBar = true }
        else Modifier
    )) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                AnimatedVisibility(
                    visible = showTopBar,
                    enter = fadeIn(tween(tokens.fastDuration)) + slideInVertically(tween(tokens.fastDuration)) { -it },
                    exit = fadeOut(tween(tokens.fastDuration)) + slideOutVertically(tween(tokens.fastDuration)) { -it }
                ) {
                    Surface(shadowElevation = 1.dp) {
                        Column {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("AI助手", style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.width(10.dp))
                                        // Model badge
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(uiState.selectedProvider.displayName,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                                style = TextStyle(fontSize = 12.sp),
                                                color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = onNavigateBack) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { immersive = !immersive; showTopBar = true }) {
                                        Icon(if (immersive) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                            if (immersive) "退出沉浸" else "沉浸模式",
                                            tint = if (immersive) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { viewModel.onEvent(AIChatEvent.Summarize) }) {
                                        Icon(Icons.Default.AutoAwesome, "摘要",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                            )
                            // Model chips
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(AIProvider.entries, key = { it.name }) { provider ->
                                    FilterChip(
                                        selected = provider == uiState.selectedProvider,
                                        onClick = { viewModel.onEvent(AIChatEvent.SelectProvider(provider)) },
                                        label = { Text(provider.displayName, style = MaterialTheme.typography.labelMedium) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                     )
                                 }
                            }
                        }
                    }
                }
                // Mode chips — always visible, outside TopAppBar animation
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = AIMode.QUICK == uiState.currentMode,
                        onClick = { viewModel.onEvent(AIChatEvent.SelectMode(AIMode.QUICK)) },
                        label = { Text("快速", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                    FilterChip(
                        selected = AIMode.THINK == uiState.currentMode,
                        onClick = { viewModel.onEvent(AIChatEvent.SelectMode(AIMode.THINK)) },
                        label = { Text("思考", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                    FilterChip(
                        selected = AIMode.IMAGE == uiState.currentMode,
                        onClick = { viewModel.onEvent(AIChatEvent.SelectMode(AIMode.IMAGE)) },
                        label = { Text("图片", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                }
            },
            bottomBar = {
                // Input bar
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = uiState.inputText,
                            onValueChange = { viewModel.onEvent(AIChatEvent.UpdateInput(it)) },
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                if (uiState.inputText.isEmpty()) {
                                    Text("输入消息...", style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }
                                inner()
                            }
                        )
                        val canSend = uiState.inputText.isNotBlank() && !uiState.isLoading
                        IconButton(
                            onClick = { userScrolledUp = false; viewModel.onEvent(AIChatEvent.SendMessage) },
                            enabled = canSend,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(
                                if (canSend) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "发送", Modifier.size(18.dp),
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .padding(horizontal = if (immersive) 8.dp else 16.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (immersive) 6.dp else 16.dp)
            ) {
                items(uiState.messages, key = { it.timestamp }) { message ->
                    Box(Modifier.animateItem()) {
                        when (message.role) {
                            ChatMessage.Role.SYSTEM -> SystemMessage(message)
                            else -> MessageBubble(message, immersive, onUrlClick = { url ->
                            browserUrl = url; showBrowser = true
                        }, onAppendToNote = if (message.role != ChatMessage.Role.USER) {
                            {
                                viewModel.onEvent(AIChatEvent.AppendToNote(message.content))
                                onNavigateBack()
                            }
                        } else null)
                        }
                    }
                }
                if (uiState.isLoading) {
                    item { LoadingBubble(immersive) }
                }
            }
        }
    }

    if (showBrowser) {
        WebBrowserDialog(
            initialUrl = browserUrl,
            searchEngine = uiState.searchEngine,
            searchEngineCustomUrl = uiState.searchEngineCustomUrl,
            bookmarksTitles = uiState.bookmarks.map { it.title },
            bookmarkUrls = uiState.bookmarks.map { it.url },
            searchHistory = uiState.searchHistory,
            onSearchEngineChange = { viewModel.onEvent(AIChatEvent.SetSearchEngine(it)) },
            onSearchEngineCustomUrlChange = { viewModel.onEvent(AIChatEvent.SetSearchEngineCustomUrl(it)) },
            onAddBookmark = { t, u -> viewModel.onEvent(AIChatEvent.AddBookmark(t, u)) },
            onRemoveBookmark = { viewModel.onEvent(AIChatEvent.RemoveBookmark(it)) },
            onAddSearchHistory = { viewModel.onEvent(AIChatEvent.AddSearchHistory(it)) },
            onClearSearchHistory = { viewModel.onEvent(AIChatEvent.ClearSearchHistory) },
            onDismiss = { showBrowser = false }
        )
    }
}

// ── Message Bubble ──
@Composable
private fun MessageBubble(message: ChatMessage, immersive: Boolean, onUrlClick: (String) -> Unit, onAppendToNote: (() -> Unit)? = null) {
    val isUser = message.role == ChatMessage.Role.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val shape = if (isUser) RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    else RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    val maxW = if (immersive) 380.dp else 320.dp

    // Typewriter effect for AI messages
    var displayedChars by remember(message.content) { mutableStateOf(if (isUser) message.content.length else 1) }
    if (!isUser) {
        LaunchedEffect(message.content) {
            val target = message.content.length
            while (displayedChars < target) {
                delay(15L) // ~66 chars/sec
                displayedChars = (displayedChars + 4).coerceAtMost(target)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        if (!isUser && !immersive) {
            Row(Modifier.padding(start = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(6.dp))
                Text("AI助手", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }

        if (!isUser && message.reasoningContent != null) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.widthIn(max = maxW).padding(bottom = 4.dp)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    Spacer(Modifier.width(6.dp))
                    Text(message.reasoningContent ?: "", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Surface(modifier = Modifier.widthIn(max = maxW), shape = shape,
            color = bg, shadowElevation = if (immersive) 0.dp else 1.dp) {
            Text(message.content.take(displayedChars), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
        // Insert into note button — AI messages only
        if (!isUser && onAppendToNote != null) {
            TextButton(
                onClick = onAppendToNote,
                modifier = Modifier.padding(top = 2.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.NoteAdd, null, Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                Spacer(Modifier.width(4.dp))
                Text("插入笔记", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
        }
    }
}

// ── System Message ──
@Composable
private fun SystemMessage(message: ChatMessage) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(message.content, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}

// ── Loading Bubble ──
@Composable
private fun LoadingBubble(immersive: Boolean) {
    val infinite = rememberInfiniteTransition(label = "load")
    val a1 by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Restart), label = "l1")
    val a2 by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Restart), label = "l2")
    val a3 by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Restart), label = "l3")

    Row(Modifier.padding(start = if (immersive) 4.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(8.dp))
        Text("AI思考中", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Spacer(Modifier.width(6.dp))
        listOf(a1, a2, a3).forEachIndexed { i, a ->
            val s = ((a + i * 0.33f).let { if (it > 1f) it - 1f else it })
            Box(Modifier.size(5.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + s * 0.5f)))
            if (i < 2) Spacer(Modifier.width(3.dp))
        }
    }
}
