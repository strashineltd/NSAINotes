package com.nsai.notes.presentation.voice

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputDialog(
    onTextResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    var phase by remember { mutableStateOf(VoicePhase.CHECKING) }
    var recognizedText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Counter to re-trigger the launch sequence
    var launchTrigger by remember { mutableIntStateOf(0) }

    // Speech intent launcher
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when {
            result.resultCode == android.app.Activity.RESULT_OK -> {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    recognizedText = text
                    phase = VoicePhase.RESULT
                    onTextResult(text)
                } else {
                    errorText = "未能识别到语音，请重试"
                    phase = VoicePhase.ERROR
                }
            }
            result.resultCode == android.app.Activity.RESULT_CANCELED -> {
                // User dismissed the speech UI — just stay ready
                phase = VoicePhase.READY
            }
            else -> {
                errorText = "语音识别服务返回异常"
                phase = VoicePhase.ERROR
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted — directly trigger speech
            launchTrigger++
        } else {
            errorText = "录音权限已被拒绝，请在系统设置中手动授予"
            phase = VoicePhase.ERROR
        }
    }

    // Main launch logic — re-runs when launchTrigger changes
    LaunchedEffect(launchTrigger) {
        phase = VoicePhase.CHECKING
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return@LaunchedEffect
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toString())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.CHINESE.toString())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        // Check if any speech recognizer is available
        val activities = context.packageManager.queryIntentActivities(intent, 0)
        if (activities.isEmpty()) {
            errorText = "设备未安装语音识别服务\n\n请安装 Google 语音搜索或其他语音识别应用"
            phase = VoicePhase.ERROR
            return@LaunchedEffect
        }

        try {
            phase = VoicePhase.LISTENING
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            errorText = "设备未安装语音识别服务"
            phase = VoicePhase.ERROR
        } catch (e: Exception) {
            errorText = "无法启动语音识别：${e.message}"
            phase = VoicePhase.ERROR
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (phase) {
                VoicePhase.CHECKING -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("正在准备...", style = MaterialTheme.typography.bodyLarge)
                }

                VoicePhase.READY -> {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Mic, null, modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("请开始说话", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { launchTrigger++ }) { Text("点击开始语音识别") }
                }

                VoicePhase.LISTENING -> {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Mic, null, modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("正在聆听...", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("语音识别界面已打开", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                VoicePhase.RESULT -> {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Mic, null, modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("识别结果:", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(4.dp))
                    Text(recognizedText.ifBlank { "（已识别）" },
                        style = MaterialTheme.typography.bodyLarge)
                }

                VoicePhase.ERROR -> {
                    Icon(Icons.Default.Mic, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text(errorText ?: "未知错误", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Text("请确保：\n• 已授予录音权限\n• 设备安装了语音识别服务",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = {
                            launchTrigger++
                            errorText = null
                        }) { Text("重试") }
                        Button(onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }) { Text("打开系统设置") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    }
}

private enum class VoicePhase { CHECKING, READY, LISTENING, RESULT, ERROR }
