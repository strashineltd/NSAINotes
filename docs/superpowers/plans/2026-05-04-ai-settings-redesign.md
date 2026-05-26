# AI Model Settings Redesign + Qwen3.6Max Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Qwen3.6Max provider, eliminate navigation lag, redesign AI settings UI with collapsible cards and status indicators.

**Architecture:** Add QWEN to the AIProvider enum + a QwenAdapter. Replace 5 sequential DataStore reads with one `getAllProviderConfigs().first()`. Replace `Column+forEach` with `LazyColumn+key`. Use optimistic StateFlow updates for API key/base URL inputs.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, OkHttp (existing patterns)

---

### Task 1: Add QWEN to AIProvider enum + QwenAdapter

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/domain/model/AIProvider.kt`
- Create: `app/src/main/java/com/nsai/notes/data/remote/ai/QwenAdapter.kt`
- Modify: `app/src/main/java/com/nsai/notes/di/AIModule.kt`

- [ ] **Step 1: Add QWEN entry to AIProvider enum**

In `app/src/main/java/com/nsai/notes/domain/model/AIProvider.kt`, add after MINIMAX:

```kotlin
    QWEN(
        displayName = "Qwen3.6Max",
        defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        quickModel = "qwen3-max",
        thinkModel = "qwen3-max",
        imageModel = null
    );
```

- [ ] **Step 2: Create QwenAdapter**

Create `app/src/main/java/com/nsai/notes/data/remote/ai/QwenAdapter.kt`:

```kotlin
package com.nsai.notes.data.remote.ai

import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.domain.model.AIMode
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.repository.AIOptions
import com.nsai.notes.domain.repository.AIResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QwenAdapter @Inject constructor(
    settingsDataStore: SettingsDataStore
) : BaseAIAdapter(settingsDataStore), AIProviderAdapter {

    override val provider: AIProvider = AIProvider.QWEN

    override suspend fun chat(messages: List<ChatMessage>, options: AIOptions, mode: AIMode): AIResponse =
        executeChat(messages, options, mode)

    override suspend fun summarize(text: String, options: AIOptions): String {
        val response = executeChat(buildSummaryMessages(text), options.copy(maxTokens = 1024), AIMode.QUICK)
        return response.content
    }

    override suspend fun generateImage(prompt: String, options: AIOptions): AIResponse =
        throw AIException("Qwen3.6Max不支持图片生成", AIExceptionType.UNKNOWN)
}
```

- [ ] **Step 3: Register QwenAdapter in AIModule**

In `app/src/main/java/com/nsai/notes/di/AIModule.kt`, add import and binding:

Add import:
```kotlin
import com.nsai.notes.data.remote.ai.QwenAdapter
```

Add after MiniMax binding:
```kotlin
    @Binds
    @IntoSet
    @Singleton
    abstract fun bindQwenAdapter(adapter: QwenAdapter): AIProviderAdapter
```

- [ ] **Step 4: Build and verify it compiles**

Run: `cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1`

Expected: BUILD SUCCESSFUL

---

### Task 2: Fix ViewModel — single I/O load + optimistic updates

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/AIModelSettingsViewModel.kt`

- [ ] **Step 1: Rewrite loadConfigs to use getAllProviderConfigs().first()**

Replace the `loadConfigs()` method:

```kotlin
    private fun loadConfigs() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    providerConfigs = settingsDataStore.getAllProviderConfigs().first()
                )
            } catch (_: Exception) {
                // DataStore reads are stable; ignore transient failures
            }
        }
    }
```

- [ ] **Step 2: Rewrite updateApiKey with optimistic local update**

Replace `updateApiKey`:

```kotlin
    private fun updateApiKey(provider: AIProvider, apiKey: String) {
        _uiState.update { state ->
            state.copy(providerConfigs = state.providerConfigs.map {
                if (it.provider == provider) it.copy(apiKey = apiKey) else it
            })
        }
        viewModelScope.launch {
            settingsDataStore.saveProviderConfig(
                settingsDataStore.getProviderConfig(provider).copy(apiKey = apiKey)
            )
        }
    }
```

- [ ] **Step 3: Rewrite updateBaseUrl with same pattern**

Replace `updateBaseUrl`:

```kotlin
    private fun updateBaseUrl(provider: AIProvider, baseUrl: String) {
        _uiState.update { state ->
            state.copy(providerConfigs = state.providerConfigs.map {
                if (it.provider == provider) it.copy(baseUrl = baseUrl) else it
            })
        }
        viewModelScope.launch {
            settingsDataStore.saveProviderConfig(
                settingsDataStore.getProviderConfig(provider).copy(baseUrl = baseUrl)
            )
        }
    }
```

- [ ] **Step 4: Rewrite toggleEnabled with same pattern**

Replace `toggleEnabled`:

```kotlin
    private fun toggleEnabled(provider: AIProvider) {
        _uiState.update { state ->
            state.copy(providerConfigs = state.providerConfigs.map {
                if (it.provider == provider) it.copy(isEnabled = !it.isEnabled) else it
            })
        }
        viewModelScope.launch {
            val current = settingsDataStore.getProviderConfig(provider)
            settingsDataStore.saveProviderConfig(current.copy(isEnabled = !current.isEnabled))
        }
    }
```

- [ ] **Step 5: Add missing import for update**

Add import at top of file:

```kotlin
import kotlinx.coroutines.flow.update
```

- [ ] **Step 6: Build and verify**

Run: `cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1`

Expected: BUILD SUCCESSFUL

---

### Task 3: Redesign AIModelSettingsScreen UI

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/AIModelSettingsScreen.kt`
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/AIHomeScreen.kt` (update model list text)

- [ ] **Step 1: Rewrite AIModelSettingsScreen with LazyColumn + collapsible cards**

Replace the entire content of `app/src/main/java/com/nsai/notes/presentation/ai/AIModelSettingsScreen.kt`:

```kotlin
package com.nsai.notes.presentation.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkCheck
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.AIProviderConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIModelSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: AIModelSettingsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Spacer(Modifier.height(4.dp))
                val enabledCount = uiState.providerConfigs.count { it.isEnabled }
                Text(
                    "已启用 $enabledCount / ${uiState.providerConfigs.size} 个模型",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
            }

            items(
                items = uiState.providerConfigs,
                key = { it.provider.name }
            ) { config ->
                ExpandableModelCard(
                    config = config,
                    testResult = uiState.testResults[config.provider],
                    onApiKeyChange = { viewModel.onEvent(AIModelSettingsEvent.UpdateApiKey(config.provider, it)) },
                    onBaseUrlChange = { viewModel.onEvent(AIModelSettingsEvent.UpdateBaseUrl(config.provider, it)) },
                    onToggleEnabled = { viewModel.onEvent(AIModelSettingsEvent.ToggleEnabled(config.provider)) },
                    onTestConnection = { viewModel.onEvent(AIModelSettingsEvent.TestConnection(config.provider)) }
                )
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ExpandableModelCard(
    config: AIProviderConfig,
    testResult: String?,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onToggleEnabled: () -> Unit,
    onTestConnection: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (config.isEnabled) 2.dp else 1.dp,
        animationSpec = tween(200),
        label = "cardElevation"
    )
    val alpha = if (config.isEnabled) 1f else 0.6f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (config.isEnabled) 0.6f else 0.35f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row — always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Provider initial icon
                val initial = remember(config.provider) { config.provider.displayName.first().uppercase() }
                val iconColor = when (config.provider) {
                    AIProvider.DEEPSEEK -> Color(0xFF4D6BFE)
                    AIProvider.KIMI -> Color(0xFF6B4CFF)
                    AIProvider.GLM -> Color(0xFF10A37F)
                    AIProvider.MINIMAX -> Color(0xFFE91E63)
                    AIProvider.QWEN -> Color(0xFF0070F3)
                }
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initial,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = iconColor
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        config.provider.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    )
                    // Status indicator
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

                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }

            // Expandable section
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = config.apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text("API Key") },
                        placeholder = { Text("sk-...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = config.isEnabled,
                        shape = RoundedCornerShape(12.dp)
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
                        Button(
                            onClick = onTestConnection,
                            enabled = config.isEnabled && config.apiKey.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.NetworkCheck, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("测试连接")
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
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update AIHomeScreen model list text**

In `app/src/main/java/com/nsai/notes/presentation/ai/AIHomeScreen.kt`, find and replace the model list text (around line 73):

Replace:
```kotlin
            text = "支持 DeepSeek-v4Pro、Kimi 2.6、GLM 5.1、MiniMax 2.7",
```

With:
```kotlin
            text = "支持 DeepSeek-v4Pro、Kimi 2.6、GLM 5.1、MiniMax 2.7、Qwen3.6Max",
```

Wait — this text is actually in `AIModelSettingsScreen.kt`, not `AIHomeScreen.kt`. Since the new UI rewrites the whole screen and removes that hardcoded text, this step is not needed — the new header already dynamically shows `已启用 X / Y 个模型`.

- [ ] **Step 3: Build and install**

Build:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat assembleDebug" 2>&1
```
Expected: BUILD SUCCESSFUL

Install and launch:
```
"/d/androidSDK/platform-tools/adb.exe" -s KJXCDY8D9DCAUWNB install -r "D:/NSAI笔记/app/build/outputs/apk/debug/app-debug.apk"
"/d/androidSDK/platform-tools/adb.exe" -s KJXCDY8D9DCAUWNB shell am start -n com.nsai.notes/.MainActivity
```

- [ ] **Step 4: Verify on device**

Manual verification checklist:
- Navigate to AI tab → Settings gear icon → AI Model Settings
- Confirm no visible lag on transition
- Confirm Qwen3.6Max card appears with "Q" icon (blue)
- Tap a card to expand → API Key / Base URL fields visible
- Type in API Key field → no stutter or lag
- Toggle enable/disable switch → instant visual feedback
- Tap "测试连接" with a valid key → green checkmark or red error icon
- Back out and re-enter → all settings persisted
```

