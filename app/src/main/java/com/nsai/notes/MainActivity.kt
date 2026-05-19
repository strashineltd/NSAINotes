package com.nsai.notes

import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import android.widget.Toast
import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.domain.model.ThemeMode
import com.nsai.notes.performance.FluidityManager
import com.nsai.notes.performance.FluidityConfig
import com.nsai.notes.performance.FrameMonitor
import com.nsai.notes.performance.AppPerformanceManager
import com.nsai.notes.performance.CrashLogService
import com.nsai.notes.performance.InputThrottler
import com.nsai.notes.presentation.navigation.NSAINavGraph
import com.nsai.notes.presentation.theme.AnimationTokens
import com.nsai.notes.presentation.theme.NSAINotesTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var frameMonitor: FrameMonitor

    @Inject
    lateinit var fluidityManager: FluidityManager

    @Inject
    lateinit var inputThrottler: InputThrottler

    @Inject
    lateinit var crashLogService: CrashLogService

    @Inject
    lateinit var securityChecker: com.nsai.notes.data.local.security.SecurityChecker

    @Inject
    lateinit var appPerformanceManager: AppPerformanceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        matchDisplayRefreshRate()
        preventTapjacking()
        hideFromRecentApps()
        appPerformanceManager.startThermalMonitoring()
        setContent {
            NSAINotesMainFrame(
                settingsDataStore = settingsDataStore,
                fluidityManager = fluidityManager,
                inputThrottler = inputThrottler,
                crashLogService = crashLogService,
                securityChecker = securityChecker,
                onAcceptPrivacy = { settingsDataStore.acceptPrivacy() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        matchDisplayRefreshRate()
        frameMonitor.start()
        clearClipboardSensitive()
    }

    override fun onPause() {
        frameMonitor.stop()
        super.onPause()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        appPerformanceManager.onTrimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        appPerformanceManager.onLowMemory()
    }

    private fun preventTapjacking() {
        window?.decorView?.post {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window?.setSystemGestureExclusionRects(listOf(android.graphics.Rect(0, 0, 0, 0)))
            }
        }
        // Tapjacking prevention: FLAG_SECURE removed to allow screenshots.
        // Overlay click interception is handled by AndroidManifest filterTouchesWhenObscured.
    }

    fun setSecureWindow(secure: Boolean = true) {
        if (secure) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun hideFromRecentApps() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: hide recents preview without blocking screenshots
            setRecentsScreenshotEnabled(false)
        }
    }

    private fun clearClipboardSensitive() {
        // Clear any API keys that might have leaked to clipboard
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        if (clipboard?.hasPrimaryClip() == true) {
            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            // Detect API key patterns and clear them
            if (clipText.contains("sk-") || clipText.contains("Bearer ") ||
                Regex("[0-9a-fA-F]{32,}").containsMatchIn(clipText)) {
                clipboard.clearPrimaryClip()
            }
        }
    }

    private fun lockRefreshRate60Hz() {
        try {
            val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
            val display = dm.getDisplay(Display.DEFAULT_DISPLAY)
            val modes = display.supportedModes
            val mode60 = modes.minByOrNull { (it.refreshRate - 60.0f).let { d -> d * d } }
            if (mode60 != null && mode60.modeId != window.attributes.preferredDisplayModeId) {
                val params = window.attributes
                params.preferredDisplayModeId = mode60.modeId
                window.attributes = params
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun matchDisplayRefreshRate() {
        try {
            val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
            val display = dm.getDisplay(Display.DEFAULT_DISPLAY)
            val modes = display.supportedModes
            val bestMode = modes.maxByOrNull { it.refreshRate }
            if (bestMode != null && bestMode.modeId != window.attributes.preferredDisplayModeId) {
                val params = window.attributes
                params.preferredDisplayModeId = bestMode.modeId
                window.attributes = params
            }
        } catch (_: Exception) {}
    }
}

@Composable
private fun NSAINotesMainFrame(
    settingsDataStore: SettingsDataStore,
    fluidityManager: FluidityManager,
    inputThrottler: InputThrottler,
    crashLogService: CrashLogService,
    securityChecker: com.nsai.notes.data.local.security.SecurityChecker,
    onAcceptPrivacy: suspend () -> Unit
) {
    var privacyAccepted by remember { mutableStateOf<Boolean?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        privacyAccepted = settingsDataStore.isPrivacyAccepted()
        // Security check deferred to background — runs after first frame renders
        checkSecurityThreats(securityChecker, crashLogService, context)
    }

    val themeValue by settingsDataStore.themeMode.collectAsState(initial = 0)
    val themeMode = ThemeMode.fromValue(themeValue)
    val fontScale by settingsDataStore.fontScale.collectAsState(initial = 1.0f)
    val fluidityConfig by fluidityManager.config.collectAsState(initial = FluidityConfig())
    val animationTokens = AnimationTokens.fromBudget(fluidityConfig.animationBudget)

    val shouldShowDialog = privacyAccepted == false

    if (privacyAccepted == null) {
        // Show themed loading surface instead of blank frame
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = when (themeMode) {
                ThemeMode.LIGHT -> Color(0xFFF8F9FA)
                ThemeMode.DARK -> Color(0xFF1C1B1F)
                ThemeMode.SYSTEM -> Color(0xFFF8F9FA) // default to light
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    if (shouldShowDialog) {
        PrivacyDialog(onAccept = {
            privacyAccepted = true
        }, onAcceptPrivacy = onAcceptPrivacy)
    }

    NSAINotesTheme(themeMode = themeMode, fontScale = fontScale, animationTokens = animationTokens) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NSAINavGraph(fluidityManager = fluidityManager, inputThrottler = inputThrottler)
        }
    }
}

@Composable
private fun PrivacyDialog(
    onAccept: () -> Unit,
    onAcceptPrivacy: suspend () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var accepting by remember { mutableStateOf(false) }

    if (accepting) return

    AlertDialog(
        onDismissRequest = {},
        title = { Text("隐私许可协议") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("欢迎使用 NSAI笔记 v${BuildConfig.VERSION_NAME}！",
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Text("本应用尊重并保护您的隐私，请阅读以下核心条款：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(Modifier.height(12.dp))

                // 数据存储
                Text("📱 数据存储",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("所有笔记、文件、对话记录仅存储在您的设备本地，不会自动上传至任何服务器。",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))

                // 信息收集
                Text("🔒 信息收集",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("本应用不收集任何个人身份信息（姓名、邮箱、电话等），不收集位置信息、设备信息或使用行为数据。不含第三方分析、广告或追踪 SDK。",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))

                // AI 功能
                Text("🤖 AI 功能说明",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("AI 功能需您自行配置第三方大模型 API Key。对话内容直接发送至您选择的 AI 服务商处理，本应用不作中转、记录或审查。建议您查阅对应 AI 服务商的隐私政策。",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))

                // 安全保护
                Text("🛡️ 安全保护",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("API Key 和隐私笔记使用设备级安全加密存储。隐私笔记支持密码锁和生物识别（指纹）保护，指纹数据由设备安全硬件处理，不离开您的设备。系统云备份默认排除 API Key 和隐私配置文件。",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))

                // 联系方式
                Text("📢 更新与联系",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("官方 QQ 频道：strashine26518",
                    style = MaterialTheme.typography.bodySmall)
                Text("GitHub：github.com/strashineltd/NSAINotes",
                    style = MaterialTheme.typography.bodySmall)
                Text("反馈问题请通过 GitHub Issues 提交",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))

                Text(
                    "在「设置 → 隐私协议」可查看完整版隐私协议。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "点击「同意并继续」即表示您已知晓并接受以上全部条款。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                accepting = true
                scope.launch {
                    onAcceptPrivacy()
                    onAccept()
                }
            }) { Text("同意并继续") }
        },
        dismissButton = {
            TextButton(onClick = {
                (context as? android.app.Activity)?.finish()
            }) { Text("退出应用") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

private fun checkSecurityThreats(
    securityChecker: com.nsai.notes.data.local.security.SecurityChecker,
    crashLogService: CrashLogService,
    context: android.content.Context
) {
    val level = securityChecker.threatLevel
    if (level >= com.nsai.notes.data.local.security.SecurityChecker.ThreatLevel.HIGH) {
        crashLogService.log("SECURITY", "威胁等级: $level (rooted=${securityChecker.isRooted} hooked=${securityChecker.isHooked})")
        if (level == com.nsai.notes.data.local.security.SecurityChecker.ThreatLevel.COMPROMISED) {
            android.widget.Toast.makeText(
                context,
                "⚠️ 检测到安全威胁，敏感功能已禁用",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}
