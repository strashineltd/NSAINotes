# AI Section UI Redesign

**Date**: 2026-05-10
**Status**: approved

## Problem

AIHomeScreen is 612 lines, 7 modes crammed into one row, mode conflicts, missing RAG mode button in UI, separate image input bar, browser as fullscreen overlay, welcome view disappears permanently.

## Design

Single-screen minimalist layout. 3 core modes visible, advanced modes in "+" expandable menu. One unified input bar for all modes.

### Layout (top to bottom)

1. **TopAppBar**: "NSAI AI" title + model name chip + history button + MCP button + settings gear
2. **Content area** (fills remaining space):
   - No messages → WelcomeView (suggestions + recent notes)
   - Has messages → ChatView (message bubbles + thinking indicator)
3. **Active mode indicator**: Small label showing current mode (e.g. "当前: Agent")
4. **Mode selector row**: [快速] [思考] [联网] [+] — 3 core chips + expand trigger
5. **"+" BottomSheet**: Agent / 知识库搜索 / 图片生成 / 文档生成
6. **Input bar**: Text field + mic button + browser button + send button

### Mode Behavior

| Mode | Input | On Send |
|------|-------|---------|
| QUICK | Any text | Direct AI chat |
| THINK | Any text | AI with reasoning |
| SEARCH | Any text | Web search → inject → AI answer |
| AGENT | Task command | ReActLoop multi-step |
| RAG | Question | RAG retrieve → inject context → AI answer |
| IMAGE | Description | AI image generation |
| DOC | Topic | AI doc generation → auto-save as note |

### Key Changes

- Remove `ImageGenBar` (separate prompt field) — image mode uses main input bar
- Remove `ModeSelector` with 7 chips — replace with 3+1 design
- Add visible RAG mode chip in "+" menu (was missing from UI)
- Add `currentModeLabel` showing active mode above input bar
- All modes are mutually exclusive (selecting one clears others)
- "+" menu auto-dismisses on selection
- WelcomeView shows on new conversation (not blank)

### Files to Change

| File | Change |
|------|--------|
| `AIHomeScreen.kt` | Full rewrite: new layout, 3+1 mode selector, remove ImageGenBar, add RAG chip |
| `AIHomeViewModel.kt` | Fix mode toggle logic (mutual exclusion), remove legacy agent commands |
| `AIHomeViewModel.kt` | Remove unused `executeAgentCommand` and helper methods |

### No Changes To

- AIChatScreen, AIChatViewModel (note-specific chat, separate from home)
- AIModelSettingsScreen, AIModelSettingsViewModel
- WebBrowserScreen (still opens as overlay from browser button)
- MCPSkillManageScreen
- NavGraph (same routes)
