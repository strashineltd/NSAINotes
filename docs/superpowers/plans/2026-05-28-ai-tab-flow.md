# AI Tab Flow 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将AI Tab重构为极简Flow风格界面 — 无气泡、无头像、文档流对话、pill切换器、浮动输入栏

**Architecture:** 重写AIHomeScreen为三层结构（pill顶栏 + 内容区 + 输入栏），对话以文档流呈现（用户消息纯文本右对齐，AI回复左侧竖线+surfaceVariant背景），删除CollapsibleTopBar/WorkspaceBar/MoreModesSheet等7个组件，新建FlowTopBar/FlowInputBar/SettingsSheet 3个组件，重写WelcomeView和ChatBubble。

**Tech Stack:** Jetpack Compose, Material3, Kotlin, Hilt, Coil

**设计文档:** `docs/superpowers/specs/2026-05-28-ai-tab-flow-design.md`

---

## 文件结构

### 新建文件
| 文件 | 职责 |
|------|------|
| `components/FlowTopBar.kt` | pill模式切换器 + 设置齿轮按钮 |
| `components/FlowInputBar.kt` | 极简浮动输入栏（pill形状，BasicTextField + 发送按钮） |
| `components/SettingsSheet.kt` | ModalBottomSheet设置面板（模型/搜索引擎/历史） |

### 重写文件
| 文件 | 变更 |
|------|------|
| `AIHomeScreen.kt` | 完全重写为三层结构，移除CollapsibleTopBar/WorkspaceBar/MoreModesSheet引用 |
| `components/WelcomeView.kt` | 重写为极简问候页（问候语 + 文字建议列表） |
| `components/ChatBubble.kt` | 重写为文档流样式（用户纯文本右对齐，AI左竖线+surfaceVariant背景） |
| `components/AIContent.kt` | 更新参数签名，适配新组件 |
| `components/ChatContainer.kt` | 更新消息渲染逻辑 |
| `components/ThinkingIndicator.kt` | 简化为3点脉冲 + 左竖线 |
| `components/SearchResultCard.kt` | 重写为折叠式纯文字列表 |

### 删除文件
| 文件 | 原因 |
|------|------|
| `components/CollapsibleTopBar.kt` | 替换为FlowTopBar |
| `components/ElevatedInputIsland.kt` | 替换为FlowInputBar |
| `components/WorkspaceTabs.kt` | 替换为FlowTopBar中的pill |
| `components/WorkspaceBar.kt` | 功能拆分到FlowTopBar + FlowInputBar |
| `components/SuggestionGrid.kt` | 替换为WelcomeView内文字建议 |
| `components/MoreModesSheet.kt` | 合并到SettingsSheet |

### 不修改文件
| 文件 | 原因 |
|------|------|
| `AIHomeViewModel.kt` | 状态管理不变，仅UI层重构 |
| `AIChatScreen.kt` | 笔记内AI助手，独立界面，不在本次范围 |
| `components/ConversationHistoryDrawer.kt` | 保留现有实现 |
| `components/AgentStepCard.kt` | Agent模式暂不重写 |
| `components/AIReplyBlock.kt` | 操作按钮保留 |
| `components/GeneratedImagePreview.kt` | 图片预览保留 |
| `UrlTextUtils.kt` | 工具函数不变 |

---

### Task 1: 创建 FlowTopBar 组件

**Files:**
- Create: `app/src/main/java/com/nsai/notes/presentation/ai/components/FlowTopBar.kt`

- [ ] **Step 1: 创建FlowTopBar.kt**

```kotlin
package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

enum class FlowTab(val label: String) {
    CHAT("对话"),
    AGENT("Agent"),
    RAG("知识")
}

@Composable
fun FlowTopBar(
    selectedTab: FlowTab,
    onTabSelected: (FlowTab) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowTab.entries.forEach { tab ->
                FlowPill(
                    label = tab.label,
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "设置",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FlowPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "pillBg"
    )
    val textColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "pillText"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/nsai/notes/presentation/ai/components/FlowTopBar.kt
git commit -m "feat: add FlowTopBar pill switcher component"
```

---

### Task 2: 创建 FlowInputBar 组件

**Files:**
- Create: `app/src/main/java/com/nsai/notes/presentation/ai/components/FlowInputBar.kt`

- [ ] **Step 1: 创建FlowInputBar.kt**

```kotlin
package com.nsai.notes.presentation.ai.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun FlowInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    placeholder: String = "输入问题...",
    isWebSearchEnabled: Boolean = false,
    onToggleWebSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isWebSearchEnabled) {
                Text(
                    text = "联网",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .clickable { onToggleWebSearch() }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    maxLines = 4
                )
            }
            IconButton(
                onClick = if (isLoading) onSend else if (text.isNotBlank()) onSend else ({}),
                modifier = Modifier.size(32.dp),
                enabled = isLoading || text.isNotBlank()
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isLoading) MaterialTheme.colorScheme.error
                    else if (text.isNotBlank()) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.outline
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                        if (isLoading) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "停止",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "发送",
                                tint = if (text.isNotBlank()) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
```

- [ ] **Step 2: 验证编译**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/nsai/notes/presentation/ai/components/FlowInputBar.kt
git commit -m "feat: add FlowInputBar minimal floating input"
```

---

### Task 3: 创建 SettingsSheet 组件

**Files:**
- Create: `app/src/main/java/com/nsai/notes/presentation/ai/components/SettingsSheet.kt`

- [ ] **Step 1: 创建SettingsSheet.kt**

```kotlin
package com.nsai.notes.presentation.ai.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.SearchEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    selectedProvider: AIProvider,
    onProviderChange: (AIProvider) -> Unit,
    searchEngine: String,
    onSearchEngineChange: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("设置", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))

            Text("模型", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            ProviderDropdown(
                selected = selectedProvider,
                onSelect = onProviderChange
            )
            Spacer(Modifier.height(20.dp))

            Text("搜索引擎", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SearchEngineDropdown(
                selected = searchEngine,
                onSelect = onSearchEngineChange
            )
            Spacer(Modifier.height(20.dp))

            Text("对话历史", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onClearHistory) {
                Text("清除所有对话", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProviderDropdown(
    selected: AIProvider,
    onSelect: (AIProvider) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { expanded = true }) {
            Text(selected.displayName, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AIProvider.entries.forEach { provider ->
                DropdownMenuItem(
                    text = { Text(provider.displayName) },
                    onClick = {
                        onSelect(provider)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchEngineDropdown(
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val engines = SearchEngine.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { expanded = true }) {
            Text(selected, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            engines.forEach { engine ->
                DropdownMenuItem(
                    text = { Text(engine.name) },
                    onClick = {
                        onSelect(engine.name)
                        expanded = false
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/nsai/notes/presentation/ai/components/SettingsSheet.kt
git commit -m "feat: add SettingsSheet modal bottom sheet"
```

---

### Task 4: 重写 WelcomeView

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/components/WelcomeView.kt`

- [ ] **Step 1: 重写WelcomeView.kt**

完全替换文件内容为：

```kotlin
package com.nsai.notes.presentation.ai.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

private val defaultSuggestions = listOf(
    "总结今天的笔记",
    "帮我整理待办事项",
    "搜索相关知识",
    "写一份项目周报"
)

@Composable
fun WelcomeView(
    onSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
    suggestions: List<String> = defaultSuggestions
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "早上好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.35f))

        Text(
            text = greeting,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "有什么我可以帮你的",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        suggestions.forEachIndexed { index, suggestion ->
            Text(
                text = "→  $suggestion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSuggestion(suggestion) }
                    .padding(vertical = 14.dp)
            )
            if (index < suggestions.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(Modifier.weight(0.65f))
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL（可能有调用方报错，Task 6统一修复）

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/nsai/notes/presentation/ai/components/WelcomeView.kt
git commit -m "refactor: rewrite WelcomeView as minimal greeting + text suggestions"
```

---

### Task 5: 重写 ChatBubble 为文档流样式

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/components/ChatBubble.kt`

- [ ] **Step 1: 重写ChatBubble.kt**

完全替换文件内容为：

```kotlin
package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.presentation.ai.buildUrlAnnotatedText
import com.nsai.notes.presentation.ai.getUrlAt
import com.nsai.notes.presentation.theme.LocalAnimationConfig

@Composable
fun ChatBubble(
    message: ChatMessage,
    isLastAIMessage: Boolean = false,
    onUrlClick: (String) -> Unit = {},
    onSaveAsNote: (String) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val animConfig = LocalAnimationConfig.current
    val isUser = message.role == ChatMessage.Role.USER

    if (isUser) {
        UserMessage(message)
    } else {
        AIMessage(
            message = message,
            isLastAIMessage = isLastAIMessage,
            onUrlClick = onUrlClick,
            onSaveAsNote = onSaveAsNote,
            animConfig = animConfig
        )
    }
}

@Composable
private fun UserMessage(message: ChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

@Composable
private fun AIMessage(
    message: ChatMessage,
    isLastAIMessage: Boolean,
    onUrlClick: (String) -> Unit,
    onSaveAsNote: (String) -> Unit,
    animConfig: androidx.compose.animation.core.AnimationConfig
) {
    var visibleChars by remember(message.content) { mutableStateOf(if (!isLastAIMessage) message.content.length else 0) }
    var typewriterDone by remember(message.content) { mutableStateOf(!isLastAIMessage) }

    if (isLastAIMessage && !typewriterDone) {
        LaunchedEffect(message.content) {
            val totalChars = message.content.length
            val nanosPerChar = 1_000_000_000L / 120
            var lastTime = 0L
            var charIndex = 0
            while (charIndex < totalChars) {
                withFrameNanos { frameTime ->
                    if (lastTime == 0L) lastTime = frameTime
                    val elapsed = frameTime - lastTime
                    val charsToAdd = (elapsed / nanosPerChar).toInt()
                    if (charsToAdd > 0) {
                        charIndex = (charIndex + charsToAdd).coerceAtMost(totalChars)
                        visibleChars = charIndex
                        lastTime = frameTime
                    }
                }
            }
            typewriterDone = true
        }
    }

    val displayText = if (typewriterDone) message.content else message.content.take(visibleChars)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(0.9f)) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(IntrinsicSize.Min)
                    .background(
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(1.dp)
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!message.reasoningContent.isNullOrBlank()) {
                    ReasoningBlock(message.reasoningContent, animConfig)
                    Spacer(Modifier.height(8.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp)
                ) {
                    if (typewriterDone) {
                        val annotated = remember(displayText) { buildUrlAnnotatedText(displayText) }
                        androidx.compose.foundation.text.ClickableText(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.7f
                            ),
                            onClick = { offset ->
                                getUrlAt(displayText, offset)?.let(onUrlClick)
                            }
                        )
                    } else {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.7f
                        )
                    }
                }

                if (typewriterDone) {
                    Spacer(Modifier.height(4.dp))
                    AIReplyActions(message.content, onSaveAsNote)
                }
            }
        }
    }
}

@Composable
private fun ReasoningBlock(
    reasoning: String,
    animConfig: androidx.compose.animation.core.AnimationConfig
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.height(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (expanded) "收起思考过程" else "查看思考过程",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(animConfig.normal)) + expandVertically(tween(animConfig.normal)),
            exit = fadeOut(tween(animConfig.fast)) + shrinkVertically(tween(animConfig.fast))
        ) {
            Text(
                text = reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun AIReplyActions(content: String, onSaveAsNote: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = { /* copy to clipboard handled by parent */ }) {
            Text("复制", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { onSaveAsNote(content) }) {
            Text("插入笔记", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL（可能有调用方报错，Task 6统一修复）

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/nsai/notes/presentation/ai/components/ChatBubble.kt
git commit -m "refactor: rewrite ChatBubble as document-flow style (no bubble, left border)"
```

---

### Task 6: 重写 ThinkingIndicator 和 SearchResultCard

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/components/ThinkingIndicator.kt`
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/components/SearchResultCard.kt`

- [ ] **Step 1: 重写ThinkingIndicator.kt**

```kotlin
package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.nsai.notes.presentation.theme.LocalAnimationConfig
import androidx.compose.material3.MaterialTheme

@Composable
fun ThinkingIndicator(modifier: Modifier = Modifier) {
    val animConfig = LocalAnimationConfig.current
    val transition = rememberInfiniteTransition(label = "thinking")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(24.dp)
                .background(
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(1.dp)
                )
        )
        Spacer(Modifier.width(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            listOf(0, 150, 300).forEach { delay ->
                val scale by transition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, delayMillis = delay),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot$delay"
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline)
                )
                if (delay != 300) Spacer(Modifier.width(4.dp))
            }
        }
    }
}
```

- [ ] **Step 2: 重写SearchResultCard.kt**

```kotlin
package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nsai.notes.data.remote.search.SearchResult
import com.nsai.notes.presentation.theme.LocalAnimationConfig
import java.net.URI

@Composable
fun SearchResultCard(
    query: String,
    results: List<SearchResult>,
    onResultClick: (String) -> Unit
) {
    val animConfig = LocalAnimationConfig.current
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) "▼" else "▶",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "联网搜索 · ${results.size}条结果",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(animConfig.normal)) + expandVertically(tween(animConfig.normal)),
            exit = fadeOut(tween(animConfig.fast)) + shrinkVertically(tween(animConfig.fast))
        ) {
            Column {
                results.take(5).forEachIndexed { index, result ->
                    val domain = remember(result.url) {
                        try { URI(result.url).host } catch (_: Exception) { result.url }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResultClick(result.url) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = result.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (index < results.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/nsai/notes/presentation/ai/components/ThinkingIndicator.kt
git add app/src/main/java/com/nsai/notes/presentation/ai/components/SearchResultCard.kt
git commit -m "refactor: simplify ThinkingIndicator and SearchResultCard for Flow design"
```

---

### Task 7: 更新 AIContent 和 ChatContainer

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/components/AIContent.kt`
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/components/ChatContainer.kt`

- [ ] **Step 1: 重写AIContent.kt**

```kotlin
package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nsai.notes.data.remote.search.SearchResult
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.presentation.theme.LocalAnimationConfig

@Composable
fun AIContent(
    showChat: Boolean,
    messages: List<ChatMessage>,
    isLoading: Boolean,
    searchResults: List<SearchResult>,
    generatedImage: String?,
    onUrlClick: (String) -> Unit,
    onSaveAsNote: (String) -> Unit,
    onRetry: () -> Unit,
    onSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val animConfig = LocalAnimationConfig.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Crossfade(
            targetState = showChat,
            animationSpec = tween(animConfig.normal),
            label = "content"
        ) { isChat ->
            Box(
                modifier = Modifier.widthIn(max = 640.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (isChat) {
                    ChatContainer(
                        messages = messages,
                        isLoading = isLoading,
                        searchResults = searchResults,
                        generatedImage = generatedImage,
                        onUrlClick = onUrlClick,
                        onSaveAsNote = onSaveAsNote,
                        onRetry = onRetry
                    )
                } else {
                    WelcomeView(onSuggestion = onSuggestion)
                }
            }
        }
    }
}
```

- [ ] **Step 2: 重写ChatContainer.kt**

```kotlin
package com.nsai.notes.presentation.ai.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nsai.notes.data.remote.search.SearchResult
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.presentation.theme.LocalAnimationConfig

@Composable
fun ChatContainer(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    searchResults: List<SearchResult>,
    generatedImage: String?,
    onUrlClick: (String) -> Unit,
    onSaveAsNote: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animConfig = LocalAnimationConfig.current
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisible ->
                if (lastVisible != null && lastVisible >= messages.size - 2) {
                    listState.animateScrollToItem(messages.size + 2)
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val lastAIMessageIndex = messages.indexOfLast { it.role == ChatMessage.Role.ASSISTANT }

        items(messages, key = { it.id }) { message ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(animConfig.fast)) + slideInVertically(
                    tween(animConfig.fast),
                    initialOffsetY = { it / 4 }
                )
            ) {
                ChatBubble(
                    message = message,
                    isLastAIMessage = messages.indexOf(message) == lastAIMessageIndex,
                    onUrlClick = onUrlClick,
                    onSaveAsNote = onSaveAsNote,
                    onRetry = onRetry
                )
            }
        }

        if (generatedImage != null) {
            item("generated_image") {
                GeneratedImagePreview(generatedImage)
            }
        }

        if (searchResults.isNotEmpty()) {
            item("search_results") {
                SearchResultCard(
                    query = messages.lastOrNull { it.role == ChatMessage.Role.USER }?.content ?: "",
                    results = searchResults,
                    onResultClick = onUrlClick
                )
            }
        }

        if (isLoading) {
            item("thinking") {
                ThinkingIndicator()
            }
        }
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/nsai/notes/presentation/ai/components/AIContent.kt
git add app/src/main/java/com/nsai/notes/presentation/ai/components/ChatContainer.kt
git commit -m "refactor: update AIContent and ChatContainer for Flow design"
```

---

### Task 8: 重写 AIHomeScreen

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/AIHomeScreen.kt`

- [ ] **Step 1: 重写AIHomeScreen.kt**

```kotlin
package com.nsai.notes.presentation.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.nsai.notes.domain.model.AIMode
import com.nsai.notes.presentation.ai.components.AIContent
import com.nsai.notes.presentation.ai.components.ConversationHistoryDrawer
import com.nsai.notes.presentation.ai.components.FlowInputBar
import com.nsai.notes.presentation.ai.components.FlowTab
import com.nsai.notes.presentation.ai.components.FlowTopBar
import com.nsai.notes.presentation.ai.components.SettingsSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIHomeScreen(
    onNavigateToNoteChat: (Long) -> Unit,
    onNavigateToModelSettings: () -> Unit,
    onNavigateToMCPSkill: () -> Unit = {},
    onNavigateToActivation: () -> Unit = {},
    viewModel: AIHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBrowser by remember { mutableStateOf(false) }
    var browserUrl by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    val selectedTab = when {
        uiState.isAgentMode -> FlowTab.AGENT
        uiState.isRagMode -> FlowTab.RAG
        else -> FlowTab.CHAT
    }

    val hasConversation = uiState.messages.size > 1

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(AIHomeEvent.ClearError)
        }
    }

    if (showSettings) {
        SettingsSheet(
            onDismiss = { showSettings = false },
            selectedProvider = uiState.selectedProvider,
            onProviderChange = { viewModel.onEvent(AIHomeEvent.SelectProvider(it)) },
            searchEngine = uiState.searchEngine,
            onSearchEngineChange = { viewModel.onEvent(AIHomeEvent.SetSearchEngine(it)) },
            onClearHistory = {
                uiState.conversationHistory.forEach { conv ->
                    viewModel.onEvent(AIHomeEvent.DeleteConversation(conv.id))
                }
                showSettings = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            FlowTopBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    when (tab) {
                        FlowTab.CHAT -> {
                            if (uiState.isAgentMode) viewModel.onEvent(AIHomeEvent.ToggleAgentMode)
                            if (uiState.isRagMode) viewModel.onEvent(AIHomeEvent.ToggleRagMode)
                        }
                        FlowTab.AGENT -> {
                            if (!uiState.isAgentMode) viewModel.onEvent(AIHomeEvent.ToggleAgentMode)
                        }
                        FlowTab.RAG -> {
                            if (!uiState.isRagMode) viewModel.onEvent(AIHomeEvent.ToggleRagMode)
                        }
                    }
                },
                onSettingsClick = { showSettings = true }
            )

            AIContent(
                showChat = hasConversation,
                messages = uiState.messages,
                isLoading = uiState.isLoading,
                searchResults = uiState.searchResults,
                generatedImage = uiState.generatedImage,
                onUrlClick = { url ->
                    browserUrl = url
                    showBrowser = true
                },
                onSaveAsNote = { content -> viewModel.onEvent(AIHomeEvent.SaveAsNote(content)) },
                onRetry = { viewModel.onEvent(AIHomeEvent.SendMessage) },
                onSuggestion = { prompt ->
                    viewModel.onEvent(AIHomeEvent.UpdateInput(prompt))
                    viewModel.onEvent(AIHomeEvent.SendMessage)
                },
                modifier = Modifier.weight(1f)
            )

            FlowInputBar(
                text = uiState.inputText,
                onTextChange = { viewModel.onEvent(AIHomeEvent.UpdateInput(it)) },
                onSend = {
                    if (uiState.isLoading) {
                        // cancel not yet implemented in ViewModel, just ignore
                    } else {
                        viewModel.onEvent(AIHomeEvent.SendMessage)
                    }
                },
                isLoading = uiState.isLoading,
                placeholder = when (selectedTab) {
                    FlowTab.AGENT -> "描述你要执行的任务..."
                    FlowTab.RAG -> "搜索笔记..."
                    else -> "输入问题..."
                },
                isWebSearchEnabled = uiState.isWebSearchMode,
                onToggleWebSearch = { viewModel.onEvent(AIHomeEvent.ToggleWebSearch) }
            )
        }
    }

    ConversationHistoryDrawer(
        visible = uiState.showHistory,
        conversations = uiState.conversationHistory,
        currentId = uiState.currentConversationId,
        onSelect = { viewModel.onEvent(AIHomeEvent.LoadConversation(it)) },
        onDelete = { viewModel.onEvent(AIHomeEvent.DeleteConversation(it)) },
        onNew = { viewModel.onEvent(AIHomeEvent.NewConversation) },
        onDismiss = { viewModel.onEvent(AIHomeEvent.ToggleHistory) }
    )

    if (showBrowser) {
        WebBrowserDialog(
            url = browserUrl,
            onDismiss = { showBrowser = false },
            onAddBookmark = { title, url -> viewModel.onEvent(AIHomeEvent.AddBookmark(title, url)) },
            bookmarks = uiState.bookmarks,
            onRemoveBookmark = { url -> viewModel.onEvent(AIHomeEvent.RemoveBookmark(url)) },
            searchHistory = uiState.searchHistory,
            onAddSearchHistory = { q -> viewModel.onEvent(AIHomeEvent.AddSearchHistory(q)) },
            onClearSearchHistory = { viewModel.onEvent(AIHomeEvent.ClearSearchHistory) }
        )
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/nsai/notes/presentation/ai/AIHomeScreen.kt
git commit -m "refactor: rewrite AIHomeScreen with Flow design (pill topbar + input bar)"
```

---

### Task 9: 删除废弃组件

**Files:**
- Delete: `app/src/main/java/com/nsai/notes/presentation/ai/components/CollapsibleTopBar.kt`
- Delete: `app/src/main/java/com/nsai/notes/presentation/ai/components/ElevatedInputIsland.kt`
- Delete: `app/src/main/java/com/nsai/notes/presentation/ai/components/WorkspaceTabs.kt`
- Delete: `app/src/main/java/com/nsai/notes/presentation/ai/components/WorkspaceBar.kt`
- Delete: `app/src/main/java/com/nsai/notes/presentation/ai/components/SuggestionGrid.kt`
- Delete: `app/src/main/java/com/nsai/notes/presentation/ai/components/MoreModesSheet.kt`

- [ ] **Step 1: 删除6个废弃组件文件**

```bash
git rm app/src/main/java/com/nsai/notes/presentation/ai/components/CollapsibleTopBar.kt
git rm app/src/main/java/com/nsai/notes/presentation/ai/components/ElevatedInputIsland.kt
git rm app/src/main/java/com/nsai/notes/presentation/ai/components/WorkspaceTabs.kt
git rm app/src/main/java/com/nsai/notes/presentation/ai/components/WorkspaceBar.kt
git rm app/src/main/java/com/nsai/notes/presentation/ai/components/SuggestionGrid.kt
git rm app/src/main/java/com/nsai/notes/presentation/ai/components/MoreModesSheet.kt
```

- [ ] **Step 2: 验证编译**

Run: `gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL（如果有其他文件引用了这些组件，需要修复引用）

- [ ] **Step 3: 修复可能的编译错误**

如果其他文件引用了被删除的组件（如AIChatScreen.kt引用WorkspaceTab），需要：
- 移除相关import
- 将WorkspaceTab替换为FlowTab或移除引用

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "chore: remove deprecated components (CollapsibleTopBar, WorkspaceBar, etc.)"
```

---

### Task 10: 构建验证与安装测试

- [ ] **Step 1: 完整构建Debug APK**

```bash
$env:JAVA_HOME = "D:\jbr-temp"
$env:ANDROID_HOME = "D:\androidSDK"
$env:Path = "D:\jbr-temp\bin;D:\androidSDK\platform-tools;$env:Path"
gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 安装到设备**

```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Expected: Success

- [ ] **Step 3: 提交最终版本**

```bash
git add -A
git commit -m "feat: AI Tab Flow design complete - build verified"
```
