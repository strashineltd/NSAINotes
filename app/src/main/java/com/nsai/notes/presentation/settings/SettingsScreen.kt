package com.nsai.notes.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    when (val dialog = uiState.updateDialog) {
        is UpdateDialogState.Checking -> {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(SettingsEvent.DismissUpdateDialog) },
                title = { Text("检查更新") },
                text = { Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp))
                    Text("正在检查新版本...")
                }},
                confirmButton = {},
                dismissButton = { TextButton(onClick = { viewModel.onEvent(SettingsEvent.DismissUpdateDialog) }) { Text("取消") } }
            )
        }
        is UpdateDialogState.Available -> {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(SettingsEvent.DismissUpdateDialog) },
                title = { Text("发现新版本 v${dialog.version}") },
                text = { Column { Text("新版可用，是否前往下载？"); if (dialog.notes.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(dialog.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) } } },
                confirmButton = { TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(dialog.url))); viewModel.onEvent(SettingsEvent.DismissUpdateDialog) }) { Text("下载") } },
                dismissButton = { TextButton(onClick = { viewModel.onEvent(SettingsEvent.DismissUpdateDialog) }) { Text("取消") } }
            )
        }
        is UpdateDialogState.UpToDate -> {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(SettingsEvent.DismissUpdateDialog) },
                title = { Text("检查更新") }, text = { Text("当前已是最新版本") },
                confirmButton = { TextButton(onClick = { viewModel.onEvent(SettingsEvent.DismissUpdateDialog) }) { Text("确定") } }
            )
        }
        is UpdateDialogState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(SettingsEvent.DismissUpdateDialog) },
                title = { Text("检查更新") }, text = { Text(dialog.message, color = MaterialTheme.colorScheme.error) },
                confirmButton = { TextButton(onClick = { viewModel.onEvent(SettingsEvent.DismissUpdateDialog) }) { Text("确定") } }
            )
        }
        is UpdateDialogState.PrivacyPolicy -> {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(SettingsEvent.DismissUpdateDialog) },
                title = { Text("隐私协议") },
                text = {
                    Column {
                        Text("NSAI笔记 不会收集任何个人身份信息。所有数据仅存储在您的设备本地。", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("AI功能需要您自行配置API Key，笔记内容会发送到您选择的AI服务商处理。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))
                        Text("使用指纹识别保护隐私笔记时，指纹数据由系统安全硬件处理，不离开您的设备。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                },
                confirmButton = { TextButton(onClick = { viewModel.onEvent(SettingsEvent.DismissUpdateDialog) }) { Text("同意") } }
            )
        }
        UpdateDialogState.Hidden -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp).animateContentSize()) {

            // Theme
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
                        Text("主题外观", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = uiState.themeMode == mode,
                                onClick = { viewModel.onEvent(SettingsEvent.SetThemeMode(mode)) },
                                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                            ) { Text(when (mode) { ThemeMode.SYSTEM -> "跟随系统"; ThemeMode.LIGHT -> "浅色"; ThemeMode.DARK -> "深色" }) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Font size
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FontDownload, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
                        Text("字体大小: ${String.format("%.0f%%", uiState.fontScale * 100)}", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = uiState.fontScale,
                        onValueChange = { viewModel.onEvent(SettingsEvent.SetFontScale(it)) },
                        valueRange = 0.8f..1.5f,
                        steps = 6,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                        Text("小", style = MaterialTheme.typography.bodySmall)
                        Text("默认", style = MaterialTheme.typography.bodySmall)
                        Text("大", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("预览效果", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text("这是字体大小预览文本",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * uiState.fontScale))
                            Text("小字体更适合阅读长文",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize * uiState.fontScale),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Privacy + About
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
                        Text("关于", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("NSAI笔记", style = MaterialTheme.typography.bodyLarge)
                            Text("v${uiState.appVersion}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { viewModel.onEvent(SettingsEvent.CheckUpdate) }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SystemUpdate, null, modifier = Modifier.height(20.dp))
                                Text("检查更新", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.onEvent(SettingsEvent.ShowPrivacyPolicy) }) {
                        Icon(Icons.Default.PrivacyTip, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp))
                        Text("隐私协议", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
