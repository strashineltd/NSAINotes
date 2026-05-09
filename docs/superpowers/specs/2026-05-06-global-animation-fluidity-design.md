# Global Animation Fluidity Improvement

**Date**: 2026-05-06
**Status**: draft

## Problem

The app has a `FluidityManager` that monitors frame drops and emits adaptive `FluidityConfig` (animation budget, speed multiplier, content skipping flags), but **no UI component consumes it**. All animation specs — `spring()`, `tween()`, staggered delays — are hardcoded in each screen. Frame monitoring only runs in Debug builds. The result: the app cannot adapt animations to device performance, and there's no mechanism to prevent animation-induced jank on low-end devices.

## Design

Three phases. Each is independently verifiable and mergable.

### Phase 1: Animation Token System

Introduce a `LocalAnimationConfig` CompositionLocal that carries adaptive animation parameters. The `FluidityManager` becomes the single source of truth; every animation in the app reads from this config instead of using hardcoded values.

**New file: `presentation/theme/AnimationConfig.kt`**

```kotlin
@Stable
data class AnimationTokens(
    val normalDuration: Int,       // Default tween duration (ms)
    val fastDuration: Int,         // Quick transitions
    val springDamping: Float,      // Spring.DampingRatio
    val springStiffness: Float,    // Spring.Stiffness
    val staggeredDelay: Int,       // Per-item stagger in lists (ms)
    val enableContentTransitions: Boolean, // Skip fade/slide on minimal budget
)

// Defaults derived from FluidityConfig.animationBudget
// FULL   → normal=300 fast=150 spring=(NoBouncy, MediumLow) stagger=50
// REDUCED→ normal=180 fast=100 spring=(LowBouncy, Medium)    stagger=0
// MINIMAL→ normal=0   fast=0   spring=(HighDamping, High)    stagger=0
// Damping increases with lower budget: 0.55 → 0.7 → 0.9 (less bounce, faster settle)
```

CompositionLocal provided in `NSAINotesTheme`, defaulting to FULL tokens. `MainActivity` collects `fluidityManager.config` and maps `AnimationBudget → AnimationTokens`.

**Files changed:**
- Create: `presentation/theme/AnimationConfig.kt`
- Modify: `MainActivity.kt` — wire FluidityConfig → AnimationTokens
- Modify: `presentation/theme/Theme.kt` — provide CompositionLocal
- Modify: `presentation/navigation/NavGraph.kt` — consume tokens for all transitions
- Modify: `presentation/notes/NoteListScreen.kt` — consume tokens for staggered animation
- Modify: `presentation/ai/AIHomeScreen.kt` — consume tokens for all internal animations

### Phase 2: Screen-by-Screen Optimization

Each screen gets targeted fixes for recomposition and rendering bottlenecks.

**NavGraph (N+1 of Phase 1 wiring):**
- Replace all hardcoded `spring()`, `tween()` with `animationTokens.*` values
- Replace `AnimatedContentTransitionScope.SlideDirection` with `graphicsLayer`-based translation for tab switches (avoids layout recalc)
- Use `Modifier.graphicsLayer { alpha = ... }` instead of `fadeIn/fadeOut` where possible

**NoteListScreen:**
- Add `contentType` to LazyColumn for type-based composition reuse
- Move `AnimatedNoteItem` delay calculation into `derivedStateOf` to avoid recomposition
- Replace `itemsIndexed` with `items(key = ...)` + pre-computed animation visibility

**AIHomeScreen:**
- `ChatBubble` — extract content into a stable `@Composable` to reduce Lambda captures
- `ThinkingDots` — use `rememberInfiniteTransition` already; ensure label strings are unique
- ModeSelector — make `AIMode.entries.forEach` stable by hoisting the list
- InputBar — debounce `onTextChange` in ViewModel (already optimistic in AIModelSettings, not here)

**NoteEditScreen:**
- Title `BasicTextField` — wrap in `remember` to prevent cursor reset on recomposition
- Content `BasicTextField` — same
- Formatting toolbar — use `graphicsLayer` for show/hide instead of `AnimatedVisibility` (expensive recomposition)
- `MarkdownPreview` — add `@Immutable` annotation to data class

**SettingsScreen / TagManageScreen / FileListScreen:**
- Lightweight screens, no animation-heavy code. Only wire AnimationTokens where applicable.

### Phase 3: Polish

**Coil ImageLoader:**
- Add `crossfade(300)` → make duration controlled by AnimationTokens
- Enable `allowHardware(false)` for list scrolling to reduce GPU contention

**FrameMonitor release mode:**
- Enable in release builds but with reduced sampling (every 600 frames instead of 300)
- Log aggregate metrics on session end for post-hoc analysis

**Scrolling-aware image loading:**
- `NoteListScreen` — pause Coil requests during fling via `LazyListState.isScrollInProgress`

## Files to Change

| Phase | File | Change |
|-------|------|--------|
| 1 | `presentation/theme/AnimationConfig.kt` | **Create** — AnimationTokens + CompositionLocal |
| 1 | `MainActivity.kt` | Wire FluidityConfig → AnimationTokens in NSAINotesMainFrame |
| 1 | `presentation/theme/Theme.kt` | Provide LocalAnimationConfig |
| 1 | `presentation/navigation/NavGraph.kt` | Replace hardcoded anim specs with tokens |
| 1 | `presentation/notes/NoteListScreen.kt` | Consume tokens for stagger delays |
| 1 | `presentation/ai/AIHomeScreen.kt` | Consume tokens for mode/animation specs |
| 2 | `presentation/navigation/NavGraph.kt` | graphicsLayer for tab transitions |
| 2 | `presentation/notes/NoteListScreen.kt` | contentType, derivedStateOf, key stability |
| 2 | `presentation/ai/AIHomeScreen.kt` | ChatBubble/ThinkingDots/InputBar optimizations |
| 2 | `presentation/notes/NoteEditScreen.kt` | TextField stability, toolbar graphicsLayer |
| 2 | `presentation/notes/components/MarkdownPreview.kt` | @Immutable |
| 2 | `presentation/settings/SettingsScreen.kt` | Wire tokens |
| 2 | `presentation/tags/TagManageScreen.kt` | Wire tokens |
| 2 | `presentation/files/FileListScreen.kt` | Wire tokens |
| 3 | `di/PerformanceModule.kt` | Coil crossfade from tokens |
| 3 | `performance/FrameMonitor.kt` | Release-mode reduced sampling |
| 3 | `NSAIApp.kt` | Start FrameMonitor in release too |

## Edge Cases

- **Token update during active animation**: Since `AnimationTokens` is `@Stable` and only changes on budget transitions (infrequent), active animations won't stutter mid-flight
- **FrameMonitor in release**: Must not affect battery. Sampling every 600 frames (~10s at 60fps) with no UI callback
- **graphicsLayer on API 26+**: All target devices support RenderNode; `graphicsLayer` is safe

## Testing

- Unit: `FluidityConfig → AnimationTokens` mapping function
- Manual: Navigate all screens on both high-end and low-end emulator
- Regression: Existing 7 unit tests must pass; no behavior change to non-animation logic
