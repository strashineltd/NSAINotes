package com.nsai.notes.presentation.notes

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.presentation.notes.components.MarkdownPreview
import com.nsai.notes.presentation.theme.LocalAnimationConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long?,
    onNavigateBack: () -> Unit,
    onNavigateToAIChat: (Long) -> Unit,
    viewModel: NoteEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(noteId) {
        delay(100) // let enter animation (tokens.fastDuration) settle before I/O
        if (noteId != null) viewModel.onEvent(NoteEditEvent.LoadNote(noteId))
        viewModel.onEvent(NoteEditEvent.LoadTags)
    }

    var justSaved by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isSaving) {
        if (justSaved && !uiState.isSaving && uiState.error == null && uiState.title.isNotBlank()) {
            snackbarHostState.showSnackbar("已保存", duration = SnackbarDuration.Short)
        }
        justSaved = uiState.isSaving
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImageSheet by remember { mutableStateOf(false) }
    var pendingImageAction by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val bitmap = withContext(Dispatchers.IO) { android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, it) }
                withContext(Dispatchers.Main) {
                    if (pendingImageAction == "describe") {
                        viewModel.onEvent(NoteEditEvent.DescribeImage(bitmap))
                    } else {
                        viewModel.onEvent(NoteEditEvent.CaptureImage(bitmap))
                    }
                }
            }
            pendingImageAction = null
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            viewModel.onEvent(NoteEditEvent.CaptureImage(it))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (noteId == null) "新建笔记" else "编辑笔记",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    val scope = rememberCoroutineScope()
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.saveAndReturn()
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(NoteEditEvent.TogglePrivate) }) {
                        Icon(
                            if (uiState.isPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (uiState.isPrivate) "隐私" else "公开",
                            tint = if (uiState.isPrivate) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val canChat = noteId != null || uiState.title.isNotBlank() || uiState.content.isNotBlank()
                    val currentId = noteId ?: uiState.savedNoteId
                    if (canChat) {
                        IconButton(onClick = {
                            if (currentId != null) {
                                onNavigateToAIChat(currentId)
                            } else {
                                viewModel.onEvent(NoteEditEvent.Save)
                            }
                        }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // Save button — top-right
                    val canSave = uiState.title.isNotBlank() || uiState.content.isNotBlank()
                    if (canSave) {
                        IconButton(onClick = { viewModel.onEvent(NoteEditEvent.Save) }) {
                            Icon(
                                Icons.Default.Done, contentDescription = "保存",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(horizontal = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp > 600) 24.dp else 0.dp)
        ) {
            // Title - large, borderless
            BasicTextField(
                value = uiState.title,
                onValueChange = { viewModel.onEvent(NoteEditEvent.UpdateTitle(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (uiState.title.isEmpty()) {
                            Text(
                                "标题",
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    lineHeight = 32.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                )
                            )
                        }
                        innerTextField()
                    }
                },
                singleLine = true
            )

            // Content area - card wrapped
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Toggle: editor or preview — Crossfade is lighter than AnimatedContent;
                    // it uses graphicsLayer-based alpha fading (GPU-composited) instead of
                    // keeping both children in the composition tree simultaneously.
                    val tokens = LocalAnimationConfig.current
                    Crossfade(
                        targetState = uiState.isPreview,
                        animationSpec = tween(tokens.normalDuration),
                        label = "editorPreview",
                        modifier = Modifier.weight(1f)
                    ) { isPreview ->
                        if (isPreview) {
                            MarkdownPreview(
                                markdown = uiState.content,
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            )
                        } else {
                            BasicTextField(
                                value = uiState.content,
                                onValueChange = { viewModel.onEvent(NoteEditEvent.UpdateContent(it)) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 26.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (uiState.content.isEmpty()) {
                                            Text(
                                                "开始写作...",
                                                style = TextStyle(
                                                    fontSize = 16.sp,
                                                    lineHeight = 26.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }

                    // Bottom bar: word count + actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${uiState.content.length} 字",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = { viewModel.onEvent(NoteEditEvent.TogglePreview) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Preview,
                                if (uiState.isPreview) "编辑" else "预览",
                                modifier = Modifier.size(20.dp),
                                tint = if (uiState.isPreview) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Formatting toolbar - only in edit mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .graphicsLayer {
                        alpha = if (!uiState.isPreview) 1f else 0f
                        translationY = if (!uiState.isPreview) 0f else 12f
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolButton(Icons.Default.Title, "标题") { viewModel.onEvent(NoteEditEvent.InsertText("# ")) }
                ToolButton(Icons.Default.FormatBold, "粗体") { viewModel.onEvent(NoteEditEvent.InsertText("**粗体**")) }
                ToolButton(Icons.Default.FormatItalic, "斜体") { viewModel.onEvent(NoteEditEvent.InsertText("*斜体*")) }
                ToolButton(Icons.AutoMirrored.Filled.FormatListBulleted, "列表") { viewModel.onEvent(NoteEditEvent.InsertText("\n- ")) }
                ToolButton(Icons.Default.FormatListNumbered, "编号") { viewModel.onEvent(NoteEditEvent.InsertText("\n1. ")) }
                ToolButton(Icons.Default.FormatQuote, "引用") { viewModel.onEvent(NoteEditEvent.InsertText("\n> ")) }
                ToolButton(Icons.Default.Code, "代码") { viewModel.onEvent(NoteEditEvent.InsertText("\n```\n\n```")) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showImageSheet = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.CameraAlt, "图片识别", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
        }
        }
    }

    // Bottom sheet for image options
    if (showImageSheet) {
        ModalBottomSheet(onDismissRequest = { showImageSheet = false }) {
            Column(Modifier.padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("拍照识别文字") },
                    supportingContent = { Text("使用相机拍摄并提取文字") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                    modifier = Modifier.clickable {
                        showImageSheet = false
                        takePictureLauncher.launch(null)
                    }
                )
                ListItem(
                    headlineContent = { Text("从相册选择") },
                    supportingContent = { Text("从相册选择图片并提取文字") },
                    leadingContent = { Icon(Icons.Default.Photo, null) },
                    modifier = Modifier.clickable {
                        showImageSheet = false
                        imagePickerLauncher.launch("image/*")
                    }
                )
                ListItem(
                    headlineContent = { Text("AI 描述图片") },
                    supportingContent = { Text("从相册选择图片，AI 详细描述内容") },
                    leadingContent = { Icon(Icons.Default.AutoAwesome, null) },
                    modifier = Modifier.clickable {
                        showImageSheet = false
                        pendingImageAction = "describe"
                        imagePickerLauncher.launch("image/*")
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
