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
import androidx.compose.foundation.shape.RoundedCornerShape
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
        // Block overlay attacks on API 26+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }
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
            // Android 14+: hide sensitive content from app switcher
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("欢迎使用 NSAI笔记 v${BuildConfig.VERSION_NAME}！", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Text("本应用尊重并保护您的隐私：")
                Spacer(Modifier.height(8.dp))
                Text("• 所有笔记数据仅存储在您的设备本地，不会上传至任何服务器。")
                Spacer(Modifier.height(4.dp))
                Text("• 应用不会收集任何个人身份信息、位置信息或设备信息。")
                Spacer(Modifier.height(4.dp))
                Text("• AI功能需自行配置第三方大模型API Key，对话内容会直接发送到您选择的AI服务商，本应用不作中转或存储。")
                Spacer(Modifier.height(4.dp))
                Text("• MCP服务器连接由您自行配置，连接信息仅存储在本地。")
                Spacer(Modifier.height(4.dp))
                Text("• 语音识别使用设备本地语音引擎，不会上传音频数据。")
                Spacer(Modifier.height(8.dp))
                Text(
                    "点击「同意并继续」即表示您已知晓并接受以上条款。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
