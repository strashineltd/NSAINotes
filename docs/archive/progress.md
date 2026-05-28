# V1.3 稳定性执行进度

## Wave 1: 稳定性底座 (9项) ✅
| # | 文件 | 修复 | 状态 |
|---|------|------|------|
| H1 | CrashLogService.kt | synchronizedList + @Volatile | ✅ |
| H2 | InputThrottler.kt | ConcurrentHashMap | ✅ |
| H3 | FrameMonitor.kt | @Volatile + @Synchronized | ✅ |
| H4 | FluidityManager.kt | @Volatile | ✅ |
| H5 | SettingsViewModel.kt | bufferedReader .use {} | ✅ |
| H6 | SecurityChecker.kt | bufferedReader .use {} + process destroy | ✅ |
| H7 | NoteEditScreen.kt | getBitmap → withContext(IO) | ✅ |
| H8 | CrashLogService.kt | (已随 H1 修复) | ✅ |
| H9 | proguard-rules.pro | 移除过宽 keep 规则 | ✅ |

## Wave 2: 性能+构建 (8项) ✅
| # | 文件 | 修复 | 状态 |
|---|------|------|------|
| M1 | NoteEditViewModel.kt | OCR/AI Job 取消 | ✅ |
| M2 | AIHomeViewModel.kt | (已有序列化) | ✅ |
| M3 | ConversationRepositoryImpl.kt | flowOn(Default) | ✅ |
| M4 | DescribeImageUseCase.kt | Bitmap recycle | ✅ |
| M5 | NoteEditScreen.kt | (跳过，需较大重构) | ⏭️ |
| M7 | libs.versions.toml | biometric 保持 1.1.0 (最新稳定) | ✅ |
| M8 | — | (项归类到其他) | — |
| M9 | — | (已随 H9) | ✅ |
| M11 | gradle.properties | -Xmx4096m | ✅ |

## Wave 3: 低优先级+备份 (6项) ✅
| # | 文件 | 修复 | 状态 |
|---|------|------|------|
| L1 | AIChatScreen.kt | reasoningContent ! → ?: | ✅ |
| L2 | SettingsScreen.kt | licenseProductName ! → ?: | ✅ |
| L5 | libs.versions.toml | 移除 composeCompiler | ✅ |
| M10 | AndroidManifest.xml | allowBackup=true + backup_rules | ✅ |
| — | res/xml/backup_rules.xml | 新建，排除 api_keys | ✅ |

## 构建验证 ✅
| 检查 | 结果 |
|------|------|
| compileDebugKotlin | ✅ 通过 |
| assembleDebug | ✅ 成功 |
| adb install | ✅ 已装到设备 |

## Wave 4: animateItem() 全覆盖 (8项) ✅
| # | 文件 | 修改 | 状态 |
|---|------|------|------|
| D1 | NoteListScreen.kt | AnimatedNoteItem 添加 modifier + animateItem() | ✅ |
| D2 | TagManageScreen.kt | AnimatedTagItem 添加 modifier + animateItem() | ✅ |
| D3 | MemoryViewScreen.kt | AnimatedVisibility 添加 Modifier.animateItem() | ✅ |
| D4 | MCPSkillManageScreen.kt | servers + skills 列表 AnimatedVisibility add animateItem() | ✅ |
| D5 | AIChatScreen.kt | 消息列表 items 包裹 Box + animateItem() | ✅ |
| D6 | AIHomeScreen.kt | ChatView + ConversationHistoryPanel items 添加 animateItem() | ✅ |
| D7 | SettingsScreen.kt | 日志行 Text 添加 Modifier.animateItem() | ✅ |
| D8 | FileListScreen.kt | 文件列表 AnimatedVisibility 添加 Modifier.animateItem() | ✅ |

## 构建验证 (Wave 4)
| 检查 | 结果 |
|------|------|
| compileDebugKotlin | ✅ 通过 |
| assembleDebug | ✅ 成功 (49s) |
| adb install | ✅ 已装到设备 KJXCDY8D9DCAUWNB |

## 捐赠功能（微信/支付宝收款码）✅
| # | 文件 | 修改 | 状态 |
|---|------|------|------|
| — | res/drawable/wechat_qr.png | 微信收款码图片 | ✅ |
| — | res/drawable/alipay_qr.png | 支付宝收款码图片 | ✅ |
| — | SettingsScreen.kt | 「支持作者」按钮 + 弹窗 + QrCodeColumn | ✅ |
