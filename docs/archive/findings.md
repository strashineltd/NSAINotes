# Findings & Decisions

## Requirements
- 修复 LicenseManager 主线程 ANR (C1/C2)
- 修复 FrameMonitor 卡顿阈值 (P1)
- 修复 FluidityManager SharedFlow 无人消费 (P2)
- 空 catch 块添加日志 (C4)

## Research Findings
- LicenseManager.activate() 被 ActivationScreen ViewModel 的 onClick 触发（UI 线程）
- LicenseService.validate() 使用同步 OkHttp client.newCall(request).execute()
- FrameMonitor 在 sampleInterval > 400 时使用延迟回调降采样
- FluidityManager._navigationEvents 只在 onScreenChange() 中 tryEmit，无显式 collector
- AIChatScreen/AIHomeScreen 的 LaunchedEffect 触发 animateScrollToItem

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| LicenseManager suspend + withContext(Dispatchers.IO) | 标准 Android 协程模式 |
| LicenseService.validate() 改为 suspend | 配合 LicenseManager，OkHttp async |
| FrameMonitor 阈值 30→22ms | 16.6ms 基准 + 1 帧容错 = ~20ms |
| FluidityManager 在 NSAIApp 中 collect | Application 级别全局事件处理 |
| 空 catch 用 Log.w(TAG, msg, e) | 标准日志模式 |

## Technical Discoveries (Wave 4: animateItem)
- W1-W3 (动画预算、导航过渡、列表入场) 已在前序会话完成
- 8 个 LazyColumn 缺失 animateItem()，现已全部添加
- AIChatScreen/AIHomeScreen 消息列表用 Box + animateItem() 包裹
- NoteListScreen/TagManageScreen 的 Animated*Item 辅助函数添加 modifier 参数
- MemoryView/MCPSkill/FileList 的 AnimatedVisibility 已有 modifier 参数，直接追加 animateItem()
- SettingsScreen 日志行 Text 直接加 Modifier.animateItem()

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| explore agent 空返回 | 切换到 bash grep 直接搜索 |
| AIChatScreen edit 工具匹配了错误位置的 `}\n                }` | 手动修复 brace mismatch |
| JAVA_HOME not set in shell | 临时 $env:JAVA_HOME = "D:\安卓开发\jbr" |

## Resources
- 项目路径: D:\NSAI笔记
- ADB 设备: KJXCDY8D9DCAUWNB
- JDK: D:\安卓开发\jbr
