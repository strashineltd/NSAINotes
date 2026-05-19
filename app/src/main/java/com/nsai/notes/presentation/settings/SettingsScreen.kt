package com.nsai.notes.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.R
import com.nsai.notes.domain.model.ThemeMode
import com.nsai.notes.presentation.theme.LocalAnimationConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToActivation: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDonateDialog by remember { mutableStateOf(false) }
    var expandedQr by remember { mutableStateOf<QrViewerState?>(null) }

    // Dialogs ...
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
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        // 一、信息收集与使用
                        Text("一、信息收集与使用",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("NSAI笔记 无需注册账号，不会收集您的姓名、邮箱、手机号、位置等任何个人身份信息。不会收集设备标识符（如 IMEI、MAC 地址）、使用行为日志或崩溃数据。本应用不含任何第三方分析 SDK、广告 SDK 或社交媒体 SDK。",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))

                        // 二、数据存储与安全
                        Text("二、数据存储与安全",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("所有笔记内容、文件、对话记录默认存储在您的设备内部存储中，不上传至任何服务器。API Key 使用 Android Keystore 进行设备级加密存储，仅本应用可通过系统安全服务解密使用。隐私笔记支持密码锁（4-6 位数字PIN）和生物识别（指纹）保护——指纹数据由设备安全硬件（TEE/SE）处理，本应用无法获取原始指纹信息。",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))

                        // 三、AI 功能说明
                        Text("三、AI 功能说明",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("本应用的 AI 对话、图片生成、文档总结等功能依赖第三方大模型服务（如 DeepSeek、Kimi、GLM、Qwen、MiniMax、MiMo 等）。使用 AI 功能前，您需要在「设置 → AI 模型设置」中自行配置对应服务商的 API Key。您发送的对话内容（包括笔记上下文）会通过网络直接发送至您选择的 AI 服务商服务器进行处理。本应用不会在中间服务器拦截、记录或审查您的对话数据。建议您在使用前查阅对应 AI 服务商的隐私政策和使用条款。",
                            style = MaterialTheme.typography.bodySmall)
                        Text("MCP（Model Context Protocol）服务器扩展由您自行配置连接地址和凭据，连接仅针对您指定的 URL，数据流向由您控制的服务器。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Spacer(Modifier.height(12.dp))

                        // 四、备份说明
                        Text("四、备份说明",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("如需使用系统云备份（Android Backup），本应用仅备份笔记内容和基本配置。API Key 和隐私相关配置文件已通过 backup_rules.xml 明确排除，不会随系统备份上传至任何云服务。建议您定期自行导出重要数据进行本地备份。",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))

                        // 五、第三方服务
                        Text("五、第三方服务",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("内置 WebView 浏览器由您主动使用，浏览行为遵循标准浏览器规则，本应用不对浏览内容进行监控或记录。本应用未集成任何第三方分析与追踪服务。",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))

                        // 六、政策更新与联系
                        Text("六、政策更新与联系",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("本隐私协议可能因功能更新而调整。如发生重大变更，应用首次启动时将重新提示您确认。",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        Text("官方 QQ 频道：strashine26518",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text("GitHub 仓库：github.com/strashineltd/NSAINotes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text("问题反馈与技术支持请通过 GitHub Issues 提交。",
                            style = MaterialTheme.typography.bodySmall)
                        Text("本应用为开源项目，完整源码可在 GitHub 查阅验证。",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.onEvent(SettingsEvent.DismissUpdateDialog) }) {
                        Text("已了解")
                    }
                }
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
                            var tapCount by remember { mutableIntStateOf(0) }
                            Text(
                                "v${uiState.appVersion}" + if (uiState.devModeEnabled) " 🛠️" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    if (uiState.devModeEnabled) return@clickable
                                    tapCount++
                                    if (tapCount >= 5) {
                                        tapCount = 0
                                        viewModel.onEvent(SettingsEvent.ActivateDevMode)
                                        android.widget.Toast.makeText(context, "🛠️ 开发者模式已开启", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
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
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showDonateDialog = true }) {
                            Icon(Icons.Default.Favorite, null, tint = Color(0xFFE91E63))
                            Spacer(Modifier.width(8.dp))
                            Text("支持作者", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/strashineltd/NSAINotes"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.OpenInBrowser, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("GitHub 仓库", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // License status
                    if (uiState.licenseActive && uiState.licenseProductName != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(uiState.licenseProductName ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("剩余 ${uiState.licenseExpireDays} 天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    // Developer options
                    if (uiState.devModeEnabled) {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("🛠️ 开发者选项", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Switch(checked = true, onCheckedChange = { /* stay on */ })
                        }
                        // Log filter chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                            listOf("ALL" to "全部", "FATAL" to "崩溃", "ERROR" to "错误").forEach { (key, label) ->
                                FilterChip(selected = uiState.logFilter == key, onClick = { viewModel.onEvent(SettingsEvent.SetLogFilter(key)) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(28.dp))
                            }
                        }
                        // Refresh button + animation toggle
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("动画开关", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(8.dp))
                                Switch(checked = uiState.animationsEnabled, onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleAnimations) }, modifier = Modifier.height(24.dp))
                            }
                            IconButton(onClick = { viewModel.onEvent(SettingsEvent.RefreshLogs) }, Modifier.size(28.dp)) {
                                Icon(Icons.Default.Refresh, "刷新日志", Modifier.size(16.dp))
                            }
                        }
                        // Log viewer
                        if (uiState.logLines.isNotEmpty()) {
                            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
                                LazyColumn(Modifier.heightIn(max = 250.dp).padding(8.dp)) {
                                    items(uiState.logLines, key = { it.hashCode().toString() }) { line ->
                                        val lineColor = when { line.contains("FATAL") -> Color(0xFFFF4444); line.contains("Error") -> Color(0xFFFF6B6B); else -> Color(0xFF4CAF50) }
                                        Text(line, style = MaterialTheme.typography.bodySmall, color = lineColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.animateItem())
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    // Donation dialog
    if (showDonateDialog) {
        AlertDialog(
            onDismissRequest = { showDonateDialog = false },
            title = { Text("支持作者") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "如果 NSAI笔记 对你有帮助，欢迎支持作者！",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QrCodeColumn(
                            painterResource(R.drawable.wechat_qr),
                            "微信收款",
                            onClick = { expandedQr = QrViewerState(R.drawable.wechat_qr, "微信收款码") }
                        )
                        QrCodeColumn(
                            painterResource(R.drawable.alipay_qr),
                            "支付宝收款",
                            onClick = { expandedQr = QrViewerState(R.drawable.alipay_qr, "支付宝收款码") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDonateDialog = false }) {
                    Text("关闭")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Full-screen QR viewer
    if (expandedQr != null) {
        BackHandler { expandedQr = null }
    }
    FullScreenQrViewer(
        state = expandedQr,
        onDismiss = { expandedQr = null }
    )
}

@Composable
private fun QrCodeColumn(
    painter: Painter,
    label: String,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painter,
            contentDescription = label,
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private data class QrViewerState(
    @androidx.annotation.DrawableRes val resId: Int,
    val label: String
)

@Composable
private fun FullScreenQrViewer(
    state: QrViewerState?,
    onDismiss: () -> Unit
) {
    val tokens = LocalAnimationConfig.current
    AnimatedVisibility(
        visible = state != null,
        enter = fadeIn(tween(tokens.normalDuration)) +
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = spring(dampingRatio = tokens.springDamping, stiffness = tokens.springStiffness)
                ),
        exit = fadeOut(tween(tokens.fastDuration))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(enabled = false) {} // prevent click-through to background
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    state?.let { s ->
                        Image(
                            painter = painterResource(s.resId),
                            contentDescription = s.label,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(280.dp)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    state?.label ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "扫描二维码向作者转账",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "点击任意位置关闭",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }
        }
    }
}
