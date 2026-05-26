package com.nsai.notes.presentation.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.domain.model.Note
import com.nsai.notes.presentation.common.ErrorView
import com.nsai.notes.presentation.theme.LocalAnimationConfig
import com.nsai.notes.presentation.common.LoadingIndicator
import com.nsai.notes.presentation.notes.components.NoteCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    onNavigateToEdit: (Long?) -> Unit,
    onNavigateToTags: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: NoteListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagingItems = viewModel.pagedNotes.collectAsLazyPagingItems()
    val tokens = LocalAnimationConfig.current
    val scope = rememberCoroutineScope()
    var pinDialog by remember { mutableStateOf<PINDialogState>(PINDialogState.Hidden) }
    var renameDialog by remember { mutableStateOf<Long?>(null) }
    var renameText by remember { mutableStateOf("") }
    var bottomSheetNote by remember { mutableStateOf<Note?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // PIN dialog
    when (pinDialog) {
        is PINDialogState.Set -> {
            var pin by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { pinDialog = PINDialogState.Hidden },
                title = { Text("设置隐私密码") },
                text = {
                    Column {
                        Text("请设置4-6位数字密码保护隐私笔记")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (pin.length >= 4) {
                            scope.launch { viewModel.setPrivacyPin(pin) }
                            pinDialog = PINDialogState.Hidden
                            onNavigateToEdit(null)
                        }
                    }, enabled = pin.length >= 4) { Text("确定") }
                },
                dismissButton = { TextButton(onClick = { pinDialog = PINDialogState.Hidden }) { Text("取消") } }
            )
        }
        is PINDialogState.Verify -> {
            var pin by remember { mutableStateOf("") }
            val targetId = (pinDialog as PINDialogState.Verify).noteId
            AlertDialog(
                onDismissRequest = { pinDialog = PINDialogState.Hidden },
                title = { Text("输入隐私密码") },
                text = {
                    Column {
                        Text("请输入密码访问隐私笔记")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val savedPin = viewModel.getPrivacyPin()
                            if (pin == savedPin) {
                                pinDialog = PINDialogState.Hidden
                                onNavigateToEdit(targetId)
                            }
                        }
                    }, enabled = pin.length >= 4) { Text("确定") }
                },
                dismissButton = { TextButton(onClick = { pinDialog = PINDialogState.Hidden }) { Text("取消") } }
            )
        }
        PINDialogState.Hidden -> {}
    }

    // Rename dialog
    renameDialog?.let { noteId ->
        AlertDialog(
            onDismissRequest = { renameDialog = null },
            title = { Text("重命名笔记") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("新标题") })
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) {
                        scope.launch { viewModel.renameNote(noteId, renameText) }
                        renameDialog = null
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { renameDialog = null }) { Text("取消") } }
        )
    }

    // Long-press action sheet
    bottomSheetNote?.let { note ->
        ModalBottomSheet(
            onDismissRequest = { bottomSheetNote = null },
            sheetState = sheetState
        ) {
            ListItem(
                headlineContent = { Text(if (note.pinned) "取消置顶" else "置顶") },
                leadingContent = { Icon(Icons.Default.PushPin, null) },
                modifier = Modifier.clickable {
                    scope.launch { viewModel.togglePin(note.id) }
                    bottomSheetNote = null
                }
            )
            ListItem(
                headlineContent = { Text("重命名") },
                leadingContent = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                modifier = Modifier.clickable {
                    renameText = note.title
                    renameDialog = note.id
                    bottomSheetNote = null
                }
            )
            ListItem(
                headlineContent = { Text("删除", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable {
                    scope.launch { viewModel.deleteNote(note.id) }
                    bottomSheetNote = null
                }
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NSAI笔记", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.onSurface, actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                actions = {
                    IconButton(onClick = onNavigateToTags) { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "标签管理") }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, contentDescription = "设置") }
                }
            )
        },
        floatingActionButton = {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val fabScale by animateFloatAsState(
                targetValue = if (isPressed) 1.12f else 1f,
                animationSpec = spring(
                    dampingRatio = tokens.springDamping,
                    stiffness = tokens.springStiffness
                ),
                label = "fabPress"
            )
            FloatingActionButton(
                onClick = { onNavigateToEdit(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                interactionSource = interactionSource,
                modifier = Modifier.scale(fabScale)
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建笔记")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 680.dp)) {
            TextField(value = uiState.searchQuery, onValueChange = { viewModel.onEvent(NoteListEvent.Search(it)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("搜索笔记...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = { if (uiState.searchQuery.isNotEmpty()) { IconButton(onClick = { viewModel.onEvent(NoteListEvent.ClearSearch) }) { Icon(Icons.Default.Clear, contentDescription = "清除搜索") } } },
                singleLine = true, shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxSize().weight(1f)) {
                when {
                    uiState.isLoading -> LoadingIndicator()
                    uiState.error != null -> ErrorView(message = uiState.error ?: "", onRetry = { viewModel.onEvent(NoteListEvent.LoadNotes) })
                    uiState.notes.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (uiState.isSearchActive) "未找到匹配的笔记" else "还没有笔记，点右下角创建", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) }
                    uiState.usePaging -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(count = pagingItems.itemCount) { index ->
                            pagingItems[index]?.let { note ->
                                Box(Modifier.animateItem()) {
                                    NoteCardWithMenu(
                                        note = note,
                                        onClick = {
                                            if (note.isPrivate) {
                                                scope.launch {
                                                    if (viewModel.getPrivacyPin().isEmpty()) {
                                                        pinDialog = PINDialogState.Set
                                                    } else {
                                                        pinDialog = PINDialogState.Verify(note.id)
                                                    }
                                                }
                                            } else onNavigateToEdit(note.id)
                                        },
                                        onToggleFavorite = { viewModel.onEvent(NoteListEvent.ToggleFavorite(note.id)) },
                                        onLongClick = { bottomSheetNote = note }
                                    )
                                }
                            }
                        }
                    }
                    else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(items = uiState.notes, key = { _, note -> "${note.id}_${note.updatedAt}" }, contentType = { _, _ -> "note_card" }) { index, note ->
                            Box(Modifier.animateItem()) {
                                NoteCardWithMenu(
                                    note = note,
                                    onClick = {
                                        if (note.isPrivate) {
                                            scope.launch {
                                                if (viewModel.getPrivacyPin().isEmpty()) {
                                                    pinDialog = PINDialogState.Set
                                                } else {
                                                    pinDialog = PINDialogState.Verify(note.id)
                                                }
                                            }
                                        } else onNavigateToEdit(note.id)
                                    },
                                    onToggleFavorite = { viewModel.onEvent(NoteListEvent.ToggleFavorite(note.id)) },
                                    onLongClick = { bottomSheetNote = note }
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

sealed class PINDialogState {
    data object Hidden : PINDialogState()
    data object Set : PINDialogState()
    data class Verify(val noteId: Long) : PINDialogState()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCardWithMenu(
    note: Note,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    NoteCard(
        note = note,
        onClick = onClick,
        onToggleFavorite = onToggleFavorite,
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
            interactionSource = interactionSource,
            indication = null
        ),
        interactionSource = interactionSource
    )
}
