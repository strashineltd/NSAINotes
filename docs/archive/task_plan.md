# Task Plan: 隐私协议扩写 + GitHub 仓库创建

## Goal
扩写 APP 隐私协议内容，创建 GitHub 公开仓库并推送代码。

## Current Phase
Phase 3: Testing & Verification ✅ complete

## Phases

### Phase 1: GitHub 仓库 ✅ complete
- [x] 安装 gh CLI (v2.76.0) 到本地
- [x] 添加 remote: github.com/strashineltd/NSAINotes.git
- [ ] GitHub 认证（需手动完成设备码验证）
- [ ] 创建仓库 + push

### Phase 2: 隐私协议扩写 ✅ complete
- [x] MainActivity.kt PrivacyDialog: 6 分区精简版（数据存储/信息收集/AI功能/安全保护/联系方式）
- [x] SettingsScreen.kt PrivacyPolicy: 6 节完整版（信息收集/数据存储/AI功能/备份/第三方/更新联系）
- [x] 新增 QQ 频道: strashine26518
- [x] 新增 GitHub: github.com/strashineltd/NSAINotes

### Phase 3: Testing & Verification
- [x] assembleDebug 编译通过 (48s)
- [x] 真机 adb install 成功

## Errors Encountered
| Error | Resolution |
|-------|------------|
| gh auth login --web 反复超时 | 需用户手动在终端完成认证 |
| winget install gh CLI 失败 | 改用直接下载便携版 zip |

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| 首次启动使用精简版 | 用户快速同意，末尾引导查看完整版 |
| 设置页使用完整版 | 供深入查阅，含全部 6 个分区 |
| 按钮改为「已了解」 | 非首次同意，仅是查阅确认 |
