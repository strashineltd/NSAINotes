# AI Model Settings Redesign + Qwen3.6Max

**Date**: 2026-05-04
**Status**: approved

## Problem

1. Navigation to AI model settings screen has visible lag (5 sequential DataStore reads + 5 KeyStore decryptions in ViewModel init)
2. UI uses `Column + forEach` with all cards eagerly composed — no lazy loading, no composition keys
3. Text input triggers full DataStore round-trip on every keystroke
4. Only 4 AI providers — need to add Qwen3.6Max

## Root Cause

`AIModelSettingsViewModel.loadConfigs()` calls `settingsDataStore.getProviderConfig(it)` for each of 5 providers. Each call does `dataStore.data.first()` (blocking disk read) + `keyStore.decryptFromString()`. That's 5 I/O operations before the screen renders.

## Design

### Data Layer

- Add `AIProvider.QWEN` enum entry with OpenAI-compatible defaults
- Create `QwenAdapter` extending `BaseAIAdapter` (no image support)
- Register in `AIModule` via `@IntoSet @Singleton @Binds`

### Performance Fix

- Replace 5 `getProviderConfig()` calls with single `getAllProviderConfigs().first()` — 1 I/O instead of 5
- Replace `Column + forEach` with `LazyColumn + key(provider.name)` — lazy composition
- Optimistic UI updates: change local State immediately, persist to DataStore async (fire-and-forget with rollback on failure)
- Switch toggle: flip `isEnabled` in StateFlow immediately, write to DataStore in background

### UI Redesign

```
Scaffold
├── TopAppBar: "AI 模型设置"
├── Current-model indicator (compact row showing enabled providers)
└── LazyColumn (key = provider.name)
    └── ModelCard (collapsible)
        ├── Collapsed: icon + name + enable toggle + connection status dot
        └── Expanded: API Key field + Base URL field + test button + result
```

- Each provider gets a colored icon circle (D/K/G/M/Q initial)
- Green status dot for enabled, gray for disabled
- Connected cards slightly elevated; disabled cards semi-transparent
- Test result: green checkmark / red exclamation icon (no full text that causes layout jump)

### ViewModel

- `loadConfigs()` at init: single `getAllProviderConfigs().first()` call
- `updateApiKey/updateBaseUrl/toggleEnabled`: optimistic StateFlow update, async DataStore write
- No blocking reads on user input
- `testConnection`: delegates to `ConnectionTester` (already implemented)

### Files to Change

| File | Change |
|------|--------|
| `domain/model/AIProvider.kt` | Add QWEN enum entry |
| `data/remote/ai/QwenAdapter.kt` | New file |
| `di/AIModule.kt` | Add QwenAdapter binding |
| `presentation/ai/AIModelSettingsScreen.kt` | Rewrite UI |
| `presentation/ai/AIModelSettingsViewModel.kt` | Optimistic updates, single load |
| `presentation/ai/AIHomeScreen.kt` | Update model list text |

### API Defaults for Qwen

```
displayName = "Qwen3.6Max"
defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1"
quickModel = "qwen3-max"
thinkModel = "qwen3-max"
imageModel = null
```
