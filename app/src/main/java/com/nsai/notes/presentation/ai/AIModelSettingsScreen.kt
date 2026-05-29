package com.nsai.notes.presentation.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.AIProviderConfig
import com.nsai.notes.presentation.theme.LocalAnimationConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIModelSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: AIModelSettingsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val clipboardManager = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.copyText.collect { text ->
            clipboardManager.setText(AnnotatedString(text))
            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 模型设置", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "header") {
                Spacer(Modifier.height(4.dp))
                val enabledCount = uiState.providerConfigs.count { it.isEnabled }
                Text("已启用 $enabledCount / ${uiState.providerConfigs.size} 个模型",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
            }

            itemsIndexed(
                items = uiState.providerConfigs,
                key = { _, config -> if (config.provider == AIProvider.CUSTOM) config.hashCode().toString() else config.provider.name }
            ) { index, config ->
                val customIdx = if (config.provider == AIProvider.CUSTOM)
                    uiState.providerConfigs.take(index + 1).count { it.provider == AIProvider.CUSTOM } - 1
                else -1
                ExpandableModelCard(
                    config = config,
                    customIndex = customIdx,
                    testResult = if (config.provider != AIProvider.CUSTOM) uiState.testResults[config.provider] else null,
                    onApiKeyChange = { viewModel.onEvent(AIModelSettingsEvent.UpdateApiKey(config.provider, it, customIdx)) },
                    onBaseUrlChange = { viewModel.onEvent(AIModelSettingsEvent.UpdateBaseUrl(config.provider, it, customIdx)) },
                    onToggleEnabled = { viewModel.onEvent(AIModelSettingsEvent.ToggleEnabled(config.provider, customIdx)) },
                    onTestConnection = { viewModel.onEvent(AIModelSettingsEvent.TestConnection(config.provider)) },
                    onDelete = if (config.provider == AIProvider.CUSTOM) ({
                        viewModel.onEvent(AIModelSettingsEvent.DeleteCustomProvider(
                            uiState.providerConfigs.take(index + 1).count { it.provider == AIProvider.CUSTOM } - 1
                        ))
                    }) else null,
                    onClearApiKey = { viewModel.onEvent(AIModelSettingsEvent.ClearApiKey(config.provider, customIdx)) },
                    onCopyApiKey = { viewModel.onEvent(AIModelSettingsEvent.CopyApiKey(config.provider, customIdx)) }
                )
            }

            item(key = "add_custom") {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showAddDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("添加自定义模型", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item(key = "browser_info") { BrowserInfoCard() }
            item(key = "bottom_spacer") { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showAddDialog) {
        AddCustomProviderDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, key, url, model ->
                viewModel.onEvent(AIModelSettingsEvent.AddCustomProvider(name, key, url, model))
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ExpandableModelCard(
    config: AIProviderConfig,
    customIndex: Int = -1,
    testResult: String?,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onToggleEnabled: () -> Unit,
    onTestConnection: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onClearApiKey: (() -> Unit)? = null,
    onCopyApiKey: (() -> Unit)? = null
) {
    val tokens = LocalAnimationConfig.current
    var expanded by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val displayName = config.customDisplayName ?: config.provider.displayName

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = if (!config.isEnabled) null
        else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val initial = remember(config) { displayName.firstOrNull()?.uppercase() ?: "?" }
                val iconTint = when {
                    config.provider == AIProvider.CUSTOM -> Color(0xFF808080)
                    config.provider == AIProvider.DEEPSEEK -> Color(0xFF4D6BFE)
                    config.provider == AIProvider.KIMI -> Color(0xFF6B4CFF)
                    config.provider == AIProvider.GLM -> Color(0xFF10A37F)
                    config.provider == AIProvider.MINIMAX -> Color(0xFFE91E63)
                    config.provider == AIProvider.QWEN -> Color(0xFF0070F3)
                    config.provider == AIProvider.MIMO -> Color(0xFFFF6900)
                    else -> Color(0xFF808080)
                }
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initial,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = iconTint
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (config.isEnabled) 1f else 0.6f
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(6.dp).clip(CircleShape).background(
                                if (config.isEnabled) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
                            )
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (config.isEnabled) "已启用" else "未启用",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(tokens.fastDuration)) + fadeIn(tween(tokens.fastDuration)),
                exit = shrinkVertically(tween(tokens.fastDuration)) + fadeOut(tween(tokens.fastDuration))
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Spacer(Modifier.height(4.dp))

                    if (config.provider == AIProvider.CUSTOM) {
                        OutlinedTextField(
                            value = config.customModelName ?: "",
                            onValueChange = { /* handled by parent */ },
                            label = { Text("模型名称") },
                            placeholder = { Text("gpt-4o-mini") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = config.isEnabled,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = config.apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text("API Key") },
                        placeholder = { Text("sk-...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = config.isEnabled,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Row {
                                if (onClearApiKey != null && config.apiKey.isNotEmpty()) {
                                    IconButton(onClick = { showClearConfirm = true }) {
                                        Icon(Icons.Default.Delete, "删除密钥", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (onCopyApiKey != null && config.apiKey.isNotEmpty()) {
                                    IconButton(onClick = { onCopyApiKey() }) {
                                        Icon(Icons.Default.ContentCopy, "复制", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (config.apiKey.isEmpty()) {
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                            ?: com.nsai.notes.data.local.ClipboardKeyHolder.pendingKey
                                        if (!text.isNullOrBlank()) {
                                            onApiKeyChange(text)
                                            com.nsai.notes.data.local.ClipboardKeyHolder.pendingKey = null
                                            Toast.makeText(context, "已粘贴", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(Icons.Default.ContentPaste, "粘贴", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = config.baseUrl,
                        onValueChange = onBaseUrlChange,
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = config.isEnabled,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (config.provider != AIProvider.CUSTOM) {
                            Button(
                                onClick = onTestConnection,
                                enabled = config.isEnabled && config.apiKey.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.NetworkCheck, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("测试连接")
                            }
                        }

                        testResult?.let { result ->
                            val isSuccess = result.contains("成功") || result.contains("✓")
                            val isTesting = result.contains("测试中")
                            if (!isTesting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (isSuccess) "连接成功" else "连接失败",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    if (showClearConfirm && onClearApiKey != null) {
                        AlertDialog(
                            onDismissRequest = { showClearConfirm = false },
                            title = { Text("删除密钥") },
                            text = { Text("确定删除此模型的 API Key 吗？") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showClearConfirm = false
                                    onClearApiKey()
                                }) { Text("确定", color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaggeredCardItem(
    index: Int,
    config: AIProviderConfig,
    testResult: String?,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onToggleEnabled: () -> Unit,
    onTestConnection: () -> Unit
) {
    val tokens = LocalAnimationConfig.current
    val delayMs = (index * tokens.staggeredDelay).coerceAtMost(tokens.normalDuration)
    AnimatedVisibility(
        visible = true,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = tokens.springDamping,
                stiffness = tokens.springStiffness
            ),
            initialScale = 0.94f
        ) + fadeIn(animationSpec = tween(delayMillis = delayMs))
    ) {
        ExpandableModelCard(
            config = config,
            testResult = testResult,
            onApiKeyChange = onApiKeyChange,
            onBaseUrlChange = onBaseUrlChange,
            onToggleEnabled = onToggleEnabled,
            onTestConnection = onTestConnection
        )
    }
}

@Composable
private fun AddCustomProviderDialog(
    onDismiss: () -> Unit,
    onAdd: (displayName: String, apiKey: String, baseUrl: String, modelName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加自定义模型") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("显示名称") }, placeholder = { Text("我的模型") },
                    singleLine = true, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = model, onValueChange = { model = it },
                    label = { Text("模型名称") }, placeholder = { Text("gpt-4o-mini") },
                    singleLine = true, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = url, onValueChange = { url = it },
                    label = { Text("Base URL") }, placeholder = { Text("https://api.openai.com/v1") },
                    singleLine = true, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = key, onValueChange = { key = it },
                    label = { Text("API Key") }, placeholder = { Text("sk-...") },
                    singleLine = true, shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, key, url, model) },
                enabled = name.isNotBlank() && model.isNotBlank() && url.isNotBlank()) {
                Text("添加")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun BrowserInfoCard() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val webViewPackage = remember {
        try { android.webkit.WebView.getCurrentWebViewPackage() } catch (_: Exception) { null }
    }
    val versionName = webViewPackage?.versionName ?: "未知"
    val packageName = webViewPackage?.packageName ?: ""
    val isChrome = packageName.contains("chrome", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TravelExplore, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("浏览器内核", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isChrome) "Chrome 内核" else "Android WebView",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("版本 $versionName", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(2.dp))
                    Text(packageName, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
                TextButton(onClick = {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", packageName, null)
                        }
                        ctx.startActivity(intent)
                    } catch (_: Exception) {}
                }) {
                    Text("更新")
                }
            }
        }
    }
}
