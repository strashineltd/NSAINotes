# Global Animation Fluidity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire FluidityManager into all UI animations, making every animation in the app adapt to device frame-rate budget, then optimize per-screen recomposition bottlenecks.

**Architecture:** Create `AnimationTokens` data class + `LocalAnimationConfig` CompositionLocal. FluidityManager's `AnimationBudget` maps to token values. Every screen reads `LocalAnimationConfig.current` instead of hardcoding `spring()`/`tween()` params. Phase 2 tightens recomposition and rendering per screen.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Choreographer (existing patterns)

---

### Task 1: Create AnimationConfig.kt

**Files:**
- Create: `app/src/main/java/com/nsai/notes/presentation/theme/AnimationConfig.kt`

- [ ] **Step 1: Create the AnimationTokens + CompositionLocal file**

```kotlin
package com.nsai.notes.presentation.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import com.nsai.notes.performance.AnimationBudget

@Stable
data class AnimationTokens(
    val normalDuration: Int,
    val fastDuration: Int,
    val springDamping: Float,
    val springStiffness: Float,
    val staggeredDelay: Int,
    val enableContentTransitions: Boolean
) {
    companion object {
        val FULL = AnimationTokens(
            normalDuration = 300, fastDuration = 150,
            springDamping = 0.55f, springStiffness = 250f,
            staggeredDelay = 50, enableContentTransitions = true
        )
        val REDUCED = AnimationTokens(
            normalDuration = 180, fastDuration = 100,
            springDamping = 0.7f, springStiffness = 400f,
            staggeredDelay = 0, enableContentTransitions = true
        )
        val MINIMAL = AnimationTokens(
            normalDuration = 0, fastDuration = 0,
            springDamping = 0.9f, springStiffness = 800f,
            staggeredDelay = 0, enableContentTransitions = false
        )

        fun fromBudget(budget: AnimationBudget): AnimationTokens = when (budget) {
            AnimationBudget.FULL -> FULL
            AnimationBudget.REDUCED -> REDUCED
            AnimationBudget.MINIMAL -> MINIMAL
        }
    }
}

val LocalAnimationConfig = compositionLocalOf { AnimationTokens.FULL }
```

- [ ] **Step 2: Build to verify compilation**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1
```
Expected: BUILD SUCCESSFUL

---

### Task 2: Wire FluidityConfig → AnimationTokens in Theme + MainActivity

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/theme/Theme.kt`
- Modify: `app/src/main/java/com/nsai/notes/MainActivity.kt`

- [ ] **Step 1: Modify NSAINotesTheme to accept and provide AnimationTokens**

In `app/src/main/java/com/nsai/notes/presentation/theme/Theme.kt`, after line 54 change the signature and add CompositionLocalProvider:

```kotlin
@Composable
fun NSAINotesTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontScale: Float = 1.0f,
    animationTokens: AnimationTokens = AnimationTokens.FULL,
    content: @Composable () -> Unit
) {
    // ... existing theme setup code unchanged ...

    CompositionLocalProvider(LocalAnimationConfig provides animationTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography,
            content = content
        )
    }
}
```

The rest of the body (lines 57-100) stays the same, just wrapping the `MaterialTheme` call.

- [ ] **Step 2: Modify NSAINotesMainFrame in MainActivity to pass AnimationTokens**

In `app/src/main/java/com/nsai/notes/MainActivity.kt`, line 124:

Add after `fontScale` collection (line 112):

```kotlin
val fluidityConfig by fluidityManager.config.collectAsState(initial = FluidityConfig())
val animationTokens = AnimationTokens.fromBudget(fluidityConfig.animationBudget)
```

Change line 124 from:
```kotlin
    NSAINotesTheme(themeMode = themeMode, fontScale = fontScale) {
```
To:
```kotlin
    NSAINotesTheme(themeMode = themeMode, fontScale = fontScale, animationTokens = animationTokens) {
```

Add import at top:
```kotlin
import com.nsai.notes.performance.FluidityConfig
import com.nsai.notes.presentation.theme.AnimationTokens
```

- [ ] **Step 3: Build to verify compilation**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1
```
Expected: BUILD SUCCESSFUL

---

### Task 3: Rewire NavGraph transitions to use AnimationTokens

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/navigation/NavGraph.kt`

- [ ] **Step 1: Replace all hardcoded animation specs with tokens**

In `app/src/main/java/com/nsai/notes/presentation/navigation/NavGraph.kt`:

Add import at top:
```kotlin
import com.nsai.notes.presentation.theme.LocalAnimationConfig
```

At the top of `NSAINavGraph` composable (after line 61, inside the function):

```kotlin
val tokens = LocalAnimationConfig.current
```

Then replace the following hardcoded values:

**Line 52-53** — `animDuration` (now unused, remove these two lines):
```kotlin
// Remove:
// private val animDuration = 300
// private val tabRoutes = ...
```

**In the NavHost transitions**, replace all `tween(X)` with `tween(tokens.normalDuration)` when X is 250-300, and `tween(150-220)` with `tween(tokens.fastDuration)`. Replace spring params:

- `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)` → `spring(dampingRatio = tokens.springDamping, stiffness = tokens.springStiffness)`
- `spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)` → `spring(dampingRatio = tokens.springDamping, stiffness = tokens.springStiffness)`
- `spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)` → `spring(dampingRatio = tokens.springDamping, stiffness = tokens.springStiffness)`

Specifically replace (use tokens.normalDuration for all tween(X) where X in [180, 200, 220, 250, 300]):

```kotlin
// Line 155: fadeIn(tween(180)) → fadeIn(tween(tokens.fastDuration))
// Line 163: fadeIn(tween(animDuration)) → fadeIn(tween(tokens.normalDuration))
// Line 174: tween(200) → tween(tokens.fastDuration)
// Line 177: fadeOut(tween(150)) → fadeOut(tween(tokens.fastDuration))
// Line 185: tween(250) → tween(tokens.normalDuration)
// Line 187: fadeOut(tween(200)) → fadeOut(tokens.fastDuration)
// Line 193: fadeIn(tween(animDuration)) → fadeIn(tween(tokens.normalDuration))
// Line 196: fadeIn(animationSpec = tween(animDuration)) → fadeIn(animationSpec = tween(tokens.normalDuration))
// Line 200: tween(250) → tween(tokens.normalDuration)
// Line 201: fadeOut(tween(200)) → fadeOut(tokens.fastDuration)

// AIModelSettings enterTransition (lines 221-228):
// tween(250) → tween(tokens.normalDuration)
// exitTransition: tween(250) → tween(tokens.normalDuration), tween(200) → tween(tokens.fastDuration)
// popEnterTransition: tween(250) → tween(tokens.normalDuration)
// popExitTransition: tween(250) → tween(tokens.normalDuration), tween(200) → tween(tokens.fastDuration)

// NoteEdit enterTransition (lines 268-275):
// tween(200) → tween(tokens.fastDuration)
// exitTransition: tween(220) → tween(tokens.normalDuration), tween(180) → tween(tokens.fastDuration)
// popEnterTransition: tween(250) → tween(tokens.normalDuration)
// popExitTransition: tween(200) → tween(tokens.fastDuration)
```

All `spring(...)` in the NavHost transitions are replaceable with the token spring params.

- [ ] **Step 2: Build to verify**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1
```
Expected: BUILD SUCCESSFUL

---

### Task 4: Rewire NoteListScreen + TagManageScreen + FileListScreen staggered animations

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/notes/NoteListScreen.kt`
- Modify: `app/src/main/java/com/nsai/notes/presentation/tags/TagManageScreen.kt`
- Modify: `app/src/main/java/com/nsai/notes/presentation/files/FileListScreen.kt`

- [ ] **Step 1: Rewire NoteListScreen AnimatedNoteItem**

In `app/src/main/java/com/nsai/notes/presentation/notes/NoteListScreen.kt`:

Add import:
```kotlin
import com.nsai.notes.presentation.theme.LocalAnimationConfig
```

In the `AnimatedNoteItem` composable (lines 318-330), replace the hardcoded fade/slide:

```kotlin
@Composable
private fun AnimatedNoteItem(
    delay: Int,
    content: @Composable () -> Unit
) {
    val tokens = LocalAnimationConfig.current
    val effectiveDelay = tokens.staggeredDelay.takeIf { delay > 0 }?.let { it * (delay / 50).coerceAtMost(6) } ?: 0
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(delayMillis = effectiveDelay)) +
                slideInVertically(animationSpec = tween(delayMillis = effectiveDelay)) { it / 4 }
    ) {
        content()
    }
}
```

- [ ] **Step 2: Rewire TagManageScreen AnimatedTagItem**

In `app/src/main/java/com/nsai/notes/presentation/tags/TagManageScreen.kt`:

Add import:
```kotlin
import com.nsai.notes.presentation.theme.LocalAnimationConfig
```

Replace the `AnimatedTagItem` composable (lines 139-151):

```kotlin
@Composable
private fun AnimatedTagItem(
    delay: Int,
    content: @Composable () -> Unit
) {
    val tokens = LocalAnimationConfig.current
    val effectiveDelay = tokens.staggeredDelay.takeIf { delay > 0 }?.let { it * (delay / 50).coerceAtMost(6) } ?: 0
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(delayMillis = effectiveDelay)) +
                slideInVertically(animationSpec = tween(delayMillis = effectiveDelay)) { it / 4 }
    ) {
        content()
    }
}
```

- [ ] **Step 3: Rewire FileListScreen staggered animations**

In `app/src/main/java/com/nsai/notes/presentation/files/FileListScreen.kt`:

Add import:
```kotlin
import com.nsai.notes.presentation.theme.LocalAnimationConfig
```

In the `LazyColumn` itemsIndexed block (around lines 202-213), replace the hardcoded `AnimatedVisibility`:

```kotlin
val tokens = LocalAnimationConfig.current
itemsIndexed(
    items = uiState.files,
    key = { _, f -> f.path }
) { index, file ->
    val staggeredDelay = tokens.staggeredDelay * index.coerceAtMost(6)
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(delayMillis = staggeredDelay)) +
                slideInVertically(tween(delayMillis = staggeredDelay)) { it / 6 }
    ) {
        FileRow(...)
    }
}
```

- [ ] **Step 4: Build to verify**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1
```
Expected: BUILD SUCCESSFUL

---

### Task 5: Rewire AIHomeScreen animations to use AnimationTokens

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/AIHomeScreen.kt`

- [ ] **Step 1: Replace all hardcoded animation values**

In `app/src/main/java/com/nsai/notes/presentation/ai/AIHomeScreen.kt`:

Add import:
```kotlin
import com.nsai.notes.presentation.theme.LocalAnimationConfig
```

At the top of `AIHomeScreen` composable (after line 125):
```kotlin
val tokens = LocalAnimationConfig.current
```

Replace everywhere:

| Original | Replace with |
|----------|-------------|
| `tween(250)` | `tween(tokens.normalDuration)` |
| `tween(200)` | `tween(tokens.fastDuration)` |
| `tween(150)` | `tween(tokens.fastDuration)` |
| `tween(100)` | `tween(tokens.fastDuration)` |
| `tween(600, delayMillis = d)` | unchanged (ThinkingDots animation, internal) |
| `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)` | `spring(dampingRatio = tokens.springDamping, stiffness = tokens.springStiffness)` |

Specifically in:

- `AnimatedContent` transitionSpec (line 199): `fadeIn(tween(250))` → `fadeIn(tween(tokens.normalDuration))`, `fadeOut(tween(200))` → `fadeOut(tween(tokens.fastDuration))`
- `AnimatedVisibility` for ImageGenBar (lines 231-232): `expandVertically(tween(200))` → `expandVertically(tween(tokens.fastDuration))`, `fadeIn/fadeOut` same
- `AnimatedVisibility` for thinking process (line 445): `expandVertically(tween(150))` → `expandVertically(tween(tokens.fastDuration))`, `shrinkVertically(tween(100))` → `shrinkVertically(tween(tokens.fastDuration))`

- [ ] **Step 2: Build to verify**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1
```
Expected: BUILD SUCCESSFUL

---

### Task 6: Rewire AIChatScreen + AIModelSettingsScreen + WebBrowser animations

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/AIChatScreen.kt`
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/AIModelSettingsScreen.kt`
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/WebBrowserScreen.kt`

- [ ] **Step 1: Wire AIChatScreen**

In `app/src/main/java/com/nsai/notes/presentation/ai/AIChatScreen.kt`:

Add import:
```kotlin
import com.nsai.notes.presentation.theme.LocalAnimationConfig
```

At top of AIChatScreen composable (after line 80):
```kotlin
val tokens = LocalAnimationConfig.current
```

Replace in `NoteThinkingSection`:
- `expandVertically(tween(200))` → `expandVertically(tween(tokens.fastDuration))`
- `shrinkVertically(tween(150))` → `shrinkVertically(tween(tokens.fastDuration))`

- [ ] **Step 2: Wire AIModelSettingsScreen**

In `app/src/main/java/com/nsai/notes/presentation/ai/AIModelSettingsScreen.kt`:

Add import:
```kotlin
import com.nsai.notes.presentation.theme.LocalAnimationConfig
```

At top of `ExpandableModelCard` (line 337):
```kotlin
val tokens = LocalAnimationConfig.current
```

Replace:
- `tween(200)` for card elevation → `tween(tokens.fastDuration)`
- `tween(250)` for expand/collapse → `tween(tokens.normalDuration)`
- `tween(200)` for shrink/fadeOut → `tween(tokens.fastDuration)`

- [ ] **Step 3: Wire WebBrowserScreen**

In `app/src/main/java/com/nsai/notes/presentation/ai/WebBrowserScreen.kt`:

Add import:
```kotlin
import com.nsai.notes.presentation.theme.LocalAnimationConfig
```

At top of `WebBrowserDialog` composable (after line 83):
```kotlin
val tokens = LocalAnimationConfig.current
```

Replace:
- `fadeIn(tween(100))` → `fadeIn(tween(tokens.fastDuration))`
- `fadeOut(tween(200))` → `fadeOut(tween(tokens.fastDuration))`
- Note: the `animateFloatAsState` spring for swipe indicator keeps its own spring params (edge gesture feedback needs specific feel, not adaptive)

- [ ] **Step 4: Build to verify**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1
```
Expected: BUILD SUCCESSFUL

---

### Task 7: Phase 2 — NavGraph graphicsLayer for tab transitions

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/navigation/NavGraph.kt`

- [ ] **Step 1: Replace slideIntoContainer with graphicsLayer-based clip translation**

In the NavHost block within `NSAINavGraph`, replace the tab-to-tab enter/exit transitions with `graphicsLayer`-backed fade-only + clip translation, which avoids layout recalculation:

Replace the enterTransition block (lines 137-165) with:

```kotlin
enterTransition = {
    val initial = initialState.destination.route
    val target = targetState.destination.route
    if (target in tabRoutes && initial in tabRoutes) {
        val idx = tabRouteOrder.indexOf(target)
        val prevIdx = tabRouteOrder.indexOf(initial)
        val direction = if (idx > prevIdx) 1 else -1
        slideIntoContainer(
            towards = if (direction > 0) AnimatedContentTransitionScope.SlideDirection.Start
            else AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = spring(
                dampingRatio = tokens.springDamping,
                stiffness = tokens.springStiffness
            ),
            initialOffset = { it / 4 }
        ) + fadeIn(animationSpec = tween(tokens.fastDuration))
    } else {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = spring(
                dampingRatio = tokens.springDamping,
                stiffness = tokens.springStiffness
            )
        ) + fadeIn(animationSpec = tween(tokens.normalDuration))
    }
},
exitTransition = {
    val initial = initialState.destination.route
    val target = targetState.destination.route
    if (target in tabRoutes && initial in tabRoutes) {
        val idx = tabRouteOrder.indexOf(target)
        val prevIdx = tabRouteOrder.indexOf(initial)
        val direction = if (idx > prevIdx) 1 else -1
        slideOutOfContainer(
            towards = if (direction > 0) AnimatedContentTransitionScope.SlideDirection.Start
            else AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(tokens.fastDuration)
        ) + fadeOut(animationSpec = tween(tokens.fastDuration))
    } else {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(tokens.normalDuration)
        ) + fadeOut(animationSpec = tween(tokens.fastDuration))
    }
},
popEnterTransition = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = spring(
            dampingRatio = tokens.springDamping,
            stiffness = tokens.springStiffness
        )
    ) + fadeIn(animationSpec = tween(tokens.normalDuration))
},
popExitTransition = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(tokens.normalDuration)
    ) + fadeOut(animationSpec = tween(tokens.fastDuration))
}
```

- [ ] **Step 2: Build to verify**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1
```
Expected: BUILD SUCCESSFUL

---

### Task 8: Phase 2 — NoteListScreen contentType + LazyColumn optimization

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/notes/NoteListScreen.kt`

- [ ] **Step 1: Add contentType to LazyColumn**

In `NoteListScreen.kt`, find the `LazyColumn` in the note list (around line 263). Add `contentType` parameter:

```kotlin
LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    itemsIndexed(
        items = uiState.notes,
        key = { _, note -> "${note.id}_${note.updatedAt}" },
        contentType = { _, _ -> "note_card" }
    ) { index, note ->
        val delay = (index * 50).coerceAtMost(300)
        AnimatedNoteItem(delay = delay) {
            NoteCardWithMenu(...)
        }
    }
}
```

- [ ] **Step 2: Build to verify**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1
```
Expected: BUILD SUCCESSFUL

---

### Task 9: Phase 2 — AIHomeScreen recomposition optimization

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/ai/AIHomeScreen.kt`

- [ ] **Step 1: Stabilize ModeSelector — hoist AIMode.entries**

In `ModeSelector` composable (line 269), replace `AIMode.entries.forEach` with a pre-remembered list:

```kotlin
@Composable
private fun ModeSelector(
    currentMode: AIMode, isWebSearch: Boolean, isAgent: Boolean, isDocGen: Boolean,
    onSelectMode: (AIMode) -> Unit, onToggleWeb: () -> Unit, onToggleAgent: () -> Unit, onToggleDocGen: () -> Unit
) {
    val modes = remember { AIMode.entries }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        modes.forEach { mode ->
            // ... rest unchanged
```

- [ ] **Step 2: Move ModeChip to stable composable — wrap icon mappings in remember**

In `ModeChip` calls (lines 273-283), the icon mapping (when block) creates a new ImageVector each recomposition. Hoist:

```kotlin
modes.forEach { mode ->
    val (icon, label) = remember(mode) {
        when (mode) {
            AIMode.QUICK -> Icons.Default.Speed to "快速"
            AIMode.THINK -> Icons.Default.Psychology to "思考"
            AIMode.IMAGE -> Icons.Default.Image to "图片"
        }
    }
    val selected = currentMode == mode
    ModeChip(icon = icon, label = label, selected = selected, onClick = { onSelectMode(mode) })
}
```

- [ ] **Step 3: Build to verify**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1
```
Expected: BUILD SUCCESSFUL

---

### Task 10: Phase 2 — NoteEditScreen TextField stability + toolbar graphicsLayer

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/presentation/notes/NoteEditScreen.kt`

- [ ] **Step 1: Wrap format toolbar with graphicsLayer instead of AnimatedVisibility**

In `NoteEditScreen.kt`, replace the `AnimatedVisibility` for the formatting toolbar (lines 328-361) with `Modifier.graphicsLayer`:

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .graphicsLayer {
            alpha = if (!uiState.isPreview) 1f else 0f
            translationY = if (!uiState.isPreview) 0f else 12f
        }
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolButton(Icons.Default.Title, "标题") { viewModel.onEvent(NoteEditEvent.InsertText("# ")) }
        ToolButton(Icons.Default.FormatBold, "粗体") { viewModel.onEvent(NoteEditEvent.InsertText("**粗体**")) }
        ToolButton(Icons.Default.FormatItalic, "斜体") { viewModel.onEvent(NoteEditEvent.InsertText("*斜体*")) }
        ToolButton(Icons.AutoMirrored.Filled.FormatListBulleted, "列表") { viewModel.onEvent(NoteEditEvent.InsertText("\n- ")) }
        ToolButton(Icons.Default.FormatListNumbered, "编号") { viewModel.onEvent(NoteEditEvent.InsertText("\n1. ")) }
        ToolButton(Icons.Default.FormatQuote, "引用") { viewModel.onEvent(NoteEditEvent.InsertText("\n> ")) }
        ToolButton(Icons.Default.Code, "代码") { viewModel.onEvent(NoteEditEvent.InsertText("\n```\n\n```")) }
    }
}
```

Add import:
```kotlin
import androidx.compose.ui.draw.alpha
```

Actually keep the existing import structure. Need `import androidx.compose.ui.graphics.graphicsLayer`.

- [ ] **Step 2: Build to verify**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1
```
Expected: BUILD SUCCESSFUL

---

### Task 11: Phase 3 — FrameMonitor release mode

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/performance/FrameMonitor.kt`
- Modify: `app/src/main/java/com/nsai/notes/MainActivity.kt`

- [ ] **Step 1: Enable FrameMonitor in release builds with reduced sampling**

In `MainActivity.kt` line 61, change from:
```kotlin
if (BuildConfig.DEBUG) frameMonitor.start()
```
To:
```kotlin
frameMonitor.start()
```

- [ ] **Step 2: Change FrameMonitor sampling rate based on build type**

In `FrameMonitor.kt`, change the constant `300` in line 58 to be build-type-aware. Since we can't use BuildConfig in a platform-agnostic manner, just reduce the sampling window from 300 to 600 frames for release:

Actually, simpler approach: don't start FrameMonitor in release at all (it's a Choreographer callback that costs CPU). Instead, wire the `FrameMetrics` collection only through the existing debug path, and make `FluidityManager` gracefully handle no-metrics state (which it already does — it only downgrades on jank report, never upgrades unless jank clears).

Better plan: Keep the debug-only start, but add an `onResume` hook that starts monitoring on ALL builds and stops in `onPause`:

In `MainActivity.kt`:

```kotlin
override fun onResume() {
    super.onResume()
    lockRefreshRate60Hz()
    frameMonitor.start()
}

override fun onPause() {
    frameMonitor.stop()
    super.onPause()
}
```

Remove the `onCreate` start:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // ... remove frameMonitor.start() from here
}
```

- [ ] **Step 3: Build to verify**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1
```
Expected: BUILD SUCCESSFUL

---

### Task 12: Phase 3 — Unit test for AnimationTokens mapping

**Files:**
- Create: `app/src/test/java/com/nsai/notes/presentation/theme/AnimationTokensTest.kt`

- [ ] **Step 1: Write the test**

```kotlin
package com.nsai.notes.presentation.theme

import com.nsai.notes.performance.AnimationBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationTokensTest {

    @Test
    fun `FULL budget maps to full animation values`() {
        val tokens = AnimationTokens.fromBudget(AnimationBudget.FULL)
        assertEquals(300, tokens.normalDuration)
        assertEquals(150, tokens.fastDuration)
        assertTrue(tokens.enableContentTransitions)
        assertEquals(50, tokens.staggeredDelay)
    }

    @Test
    fun `REDUCED budget cuts durations and disables stagger`() {
        val tokens = AnimationTokens.fromBudget(AnimationBudget.REDUCED)
        assertEquals(180, tokens.normalDuration)
        assertEquals(100, tokens.fastDuration)
        assertEquals(0, tokens.staggeredDelay)
        assertTrue(tokens.enableContentTransitions)
    }

    @Test
    fun `MINIMAL budget disables animations`() {
        val tokens = AnimationTokens.fromBudget(AnimationBudget.MINIMAL)
        assertEquals(0, tokens.normalDuration)
        assertEquals(0, tokens.fastDuration)
        assertEquals(0, tokens.staggeredDelay)
        assertTrue(!tokens.enableContentTransitions)
    }

    @Test
    fun `spring stiffness increases as budget decreases`() {
        val full = AnimationTokens.fromBudget(AnimationBudget.FULL)
        val reduced = AnimationTokens.fromBudget(AnimationBudget.REDUCED)
        val minimal = AnimationTokens.fromBudget(AnimationBudget.MINIMAL)
        assertTrue(full.springStiffness <= reduced.springStiffness)
        assertTrue(reduced.springStiffness <= minimal.springStiffness)
    }

    @Test
    fun `AnimationTokens is Stable data class`() {
        val tokens1 = AnimationTokens.FULL
        val tokens2 = AnimationTokens.FULL
        assertEquals(tokens1, tokens2)
        assertEquals(tokens1.hashCode(), tokens2.hashCode())
    }
}
```

- [ ] **Step 2: Run tests**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat testDebugUnitTest --tests *AnimationTokensTest*" 2>&1
```
Expected: 5 tests PASS

---

### Task 13: Phase 3 — Coil loading optimization

**Files:**
- Modify: `app/src/main/java/com/nsai/notes/di/PerformanceModule.kt`
- Modify: `app/src/main/java/com/nsai/notes/presentation/notes/NoteListScreen.kt`

- [ ] **Step 1: Increase Coil crossfade duration**

In `app/src/main/java/com/nsai/notes/di/PerformanceModule.kt`, line 33 change from:
```kotlin
.crossfade(100)
```
To:
```kotlin
.crossfade(250)
```

- [ ] **Step 2: Pause Coil image loading during fling in NoteListScreen**

In `app/src/main/java/com/nsai/notes/presentation/notes/NoteListScreen.kt`, add to the `LazyColumn` block:

After the existing `LazyColumn` definition, add `flingBehavior` or use `LaunchedEffect` watching scroll state. Simpler approach: add a `LaunchedEffect` observing scroll state near the `LazyColumn`:

At the top of `NoteListScreen` composable, add:
```kotlin
import coil.compose.AsyncImage
```
(already imported via existing usage — skip if present)

Actually, the simplest Coil pause-on-fling is to use the `ImageLoader` directly. But since we use `AsyncImage` composable everywhere, a simpler approach: increase crossfade already handles visual smoothness. Skip the fling pause — it adds complexity for marginal gain on modern devices.

- [ ] **Step 3: Build to verify**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat compileDebugKotlin" 2>&1
```
Expected: BUILD SUCCESSFUL

---

### Task 14: Full build + install and verify

**Files:** (none, verification only)

- [ ] **Step 1: Full debug build**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat assembleDebug" 2>&1
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run all existing unit tests**

Run:
```
cd "D:/NSAI笔记" && JAVA_HOME="/d/安卓开发/jbr" PATH="/d/安卓开发/jbr/bin:$PATH" cmd.exe //c "gradlew.bat testDebugUnitTest" 2>&1
```
Expected: All tests PASS

- [ ] **Step 3: Install on device**

Run:
```
"/d/androidSDK/platform-tools/adb.exe" -s KJXCDY8D9DCAUWNB install -r "D:/NSAI笔记/app/build/outputs/apk/debug/app-debug.apk"
```

- [ ] **Step 4: Launch and verify**

Run:
```
"/d/androidSDK/platform-tools/adb.exe" -s KJXCDY8D9DCAUWNB shell am start -n com.nsai.notes/.MainActivity
```

Manual verification:
- [ ] Navigate between tabs — transitions feel smooth, no stutter
- [ ] Open note list — staggered card animations play
- [ ] Open AI settings — collapsible cards expand/collapse smoothly
- [ ] AI chat — messages appear, thinking indicator animates
- [ ] Note editor — toolbar toggle via preview button, no layout jump
- [ ] Settings — font slider, theme toggle, all work
- [ ] Tags — staggered list animation
- [ ] Files — staggered list animation
- [ ] Run all existing unit tests — no regressions
