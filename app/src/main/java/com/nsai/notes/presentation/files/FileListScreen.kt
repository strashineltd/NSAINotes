package com.nsai.notes.presentation.files

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.presentation.theme.LocalAnimationConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(viewModel: FileListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.openFileIntent) {
        uiState.openFileIntent?.let { intent ->
            try { context.startActivity(intent) } catch (e: Exception) {
                android.widget.Toast.makeText(context, "无法打开文件", android.widget.Toast.LENGTH_SHORT).show()
            }
            viewModel.onEvent(FileListEvent.DismissDialog)
        }
    }

    // Create dialog
    if (uiState.showCreateDialog) CreateDialog(
        isFolder = uiState.createIsFolder, name = uiState.createName,
        onNameChange = { viewModel.onEvent(FileListEvent.UpdateCreateName(it)) },
        onConfirm = { viewModel.onEvent(FileListEvent.ConfirmCreate) },
        onDismiss = { viewModel.onEvent(FileListEvent.DismissDialog) }
    )
    // Rename dialog
    uiState.renameTarget?.let { RenameDialog(it.name, onNameChange = { viewModel.onEvent(FileListEvent.UpdateRenameName(it)) }, onConfirm = { viewModel.onEvent(FileListEvent.ConfirmRename) }, onDismiss = { viewModel.onEvent(FileListEvent.DismissDialog) }) }
    // Delete confirm
    uiState.deleteConfirmTarget?.let { target ->
        AlertDialog(onDismissRequest = { viewModel.onEvent(FileListEvent.DismissDialog) }, title = { Text("确认删除") },
            text = { Text("确定删除「${target.name}」？") },
            confirmButton = { TextButton(onClick = { viewModel.onEvent(FileListEvent.ConfirmDelete(target)); viewModel.onEvent(FileListEvent.DismissDialog) }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { viewModel.onEvent(FileListEvent.DismissDialog) }) { Text("取消") } })
    }
    // PIN dialog
    if (uiState.showPinDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { viewModel.onEvent(FileListEvent.DismissDialog) }, title = { Text("隐私文件") },
            text = { Column { Text("输入密码查看隐私文件"); Spacer(Modifier.height(8.dp))
                OutlinedTextField(pin, { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it }, Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)) } },
            confirmButton = { TextButton(onClick = { viewModel.onEvent(FileListEvent.VerifyPin(pin)) }, enabled = pin.length >= 4) { Text("确定") } },
            dismissButton = { TextButton(onClick = { viewModel.onEvent(FileListEvent.DismissDialog) }) { Text("取消") } })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.canGoUp) { IconButton(onClick = { viewModel.onEvent(FileListEvent.NavigateUp) }, Modifier.size(32.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回上级", Modifier.size(20.dp)) }; Spacer(Modifier.width(4.dp)) }
                        Text(if (uiState.canGoUp) uiState.currentPath else "文件管理", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    if (!uiState.showSystemFiles) {
                        IconButton(onClick = { viewModel.onEvent(FileListEvent.ShowCreateFolder) }) { Icon(Icons.Default.CreateNewFolder, "新建文件夹") }
                        IconButton(onClick = { viewModel.onEvent(FileListEvent.ShowCreateFile) }) { Icon(Icons.AutoMirrored.Filled.NoteAdd, "新建文件") }
                    }
                    if (!uiState.showSystemFiles) {
                        IconButton(onClick = { if (uiState.isPrivateUnlocked) viewModel.onEvent(FileListEvent.LockPrivate) else viewModel.onEvent(FileListEvent.ShowUnlockPin) }) {
                            Icon(if (uiState.isPrivateUnlocked) Icons.Default.LockOpen else Icons.Default.Lock, "隐私文件", tint = if (uiState.isPrivateUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { viewModel.onEvent(FileListEvent.ToggleSystemView) }) {
                        Icon(Icons.Default.DeveloperMode, "系统文件", tint = if (uiState.showSystemFiles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.onSurface, actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(12.dp)); Button(onClick = { viewModel.onEvent(FileListEvent.LoadFiles) }) { Text("重试") } } }
                uiState.files.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Folder, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) }
                        Spacer(Modifier.height(16.dp)); Text("还没有文件", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(Modifier.height(4.dp)); Text("点击右上角创建文件或文件夹", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
                else -> {
                    val tokens = LocalAnimationConfig.current
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item(key = "section_folders") {
                            val folderCount = uiState.files.count { it.isDirectory }
                            if (folderCount > 0) Text("文件夹 ($folderCount)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                        }
                        itemsIndexed(uiState.files, key = { _, f -> f.path }) { index, file ->
                            val staggeredDelay = tokens.staggeredDelay * index.coerceAtMost(6)
                            AnimatedVisibility(visible = true, modifier = Modifier.animateItem(), enter = fadeIn(tween(durationMillis = tokens.normalDuration, delayMillis = staggeredDelay)) + slideInVertically(tween(durationMillis = tokens.normalDuration, delayMillis = staggeredDelay)) { it / 6 }) {
                                FileRow(file = file, onDelete = { viewModel.onEvent(FileListEvent.DeleteItem(file)) }, onRename = { viewModel.onEvent(FileListEvent.StartRename(file)) },
                                    onTogglePrivate = { viewModel.onEvent(FileListEvent.TogglePrivate(file)) }, isPrivateUnlocked = uiState.isPrivateUnlocked,
                                    onClick = { if (file.isDirectory) viewModel.onEvent(FileListEvent.OpenFolder(file)) else viewModel.onEvent(FileListEvent.OpenFile(file)) })
                            }
                        }
                        item(key = "bottom_spacer") { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileRow(file: AppFileItem, onDelete: () -> Unit, onRename: () -> Unit, onClick: () -> Unit = {}, onTogglePrivate: () -> Unit = {}, isPrivateUnlocked: Boolean = false) {
    val isPrivate = file.path.contains("/.private/")
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { when (it) { SwipeToDismissBoxValue.EndToStart -> onDelete(); SwipeToDismissBoxValue.StartToEnd -> onRename(); else -> {} }; false })
    SwipeToDismissBox(state = dismissState, enableDismissFromStartToEnd = true, enableDismissFromEndToStart = true,
        backgroundContent = {
            Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.DriveFileRenameOutline, "重命名", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("重命名", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("删除", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error); Spacer(Modifier.width(6.dp)); Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(20.dp)) }
            }
        }
    ) {
        Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (file.isDirectory) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                val icon = fileIcon(file)
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(if (file.isDirectory) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { Icon(icon, null, modifier = Modifier.size(22.dp), tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(file.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (isPrivate) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Lock, "隐私", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(file.formattedDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        if (!file.isDirectory && file.size > 0) { Spacer(Modifier.width(8.dp)); Text(formatSize(file.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) }
                        if (isPrivateUnlocked) { Spacer(Modifier.width(8.dp)); Text(if (isPrivate) "🔒" else "🔓", style = MaterialTheme.typography.labelSmall, color = if (isPrivate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.clickable { onTogglePrivate() }) }
                    }
                }
            }
        }
    }
}

@Composable private fun CreateDialog(isFolder: Boolean, name: String, onNameChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(if (isFolder) Icons.Default.CreateNewFolder else Icons.AutoMirrored.Filled.NoteAdd, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(if (isFolder) "新建文件夹" else "新建文件") },
        text = { OutlinedTextField(name, onNameChange, Modifier.fillMaxWidth(), label = { Text(if (isFolder) "文件夹名称" else "文件名称") }, singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm() })) },
        confirmButton = { Button(onClick = onConfirm, enabled = name.isNotBlank(), shape = RoundedCornerShape(10.dp)) { Text("创建") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable private fun RenameDialog(name: String, onNameChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Default.DriveFileRenameOutline, null, tint = MaterialTheme.colorScheme.primary) }, title = { Text("重命名") },
        text = { OutlinedTextField(name, onNameChange, Modifier.fillMaxWidth(), label = { Text("新名称") }, singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm() })) },
        confirmButton = { Button(onClick = onConfirm, enabled = name.isNotBlank(), shape = RoundedCornerShape(10.dp)) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

private fun fileIcon(file: AppFileItem): ImageVector {
    if (file.isDirectory) return Icons.Default.Folder
    return when (file.name.substringAfterLast('.', "").lowercase()) { "jpg", "jpeg", "png", "gif", "webp", "bmp" -> Icons.Default.Image; "pdf" -> Icons.Default.PictureAsPdf; "txt", "md", "markdown" -> Icons.Default.Description; else -> Icons.AutoMirrored.Filled.InsertDriveFile }
}

private fun formatSize(bytes: Long): String = when { bytes < 1024 -> "$bytes B"; bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0); bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024)); else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024)) }
