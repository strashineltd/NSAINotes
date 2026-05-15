# AI区 v1.1 — 生产力工具风格重设计

**日期**: 2026-05-14
**状态**: approved

## 概述

将AI区从7模式芯片+弹窗覆盖层的聊天应用风格，重新设计为生产力工具风格（类似Notion/Craft），核心特性：统一工作区、Agent可视化、对话→笔记一键转换。

## 整体架构

### 布局（上→下）

1. **顶栏**: 返回 + NSAI AI + 模型下拉 + 历史侧边栏/插件/设置图标
2. **工作区标签**: [💬对话] [🤖Agent] [📚知识库] — 3标签切换
3. **统一工作区**: LazyColumn，所有内容以"块"为单位组织
4. **上下文指示器**: 当前模式+模型+状态信息
5. **输入栏**: [+]附件菜单 + 文本输入 + 语音/发送

### 工作区标签

| 标签 | AI行为 | 工作区内容 |
|------|--------|-----------|
| 💬 对话 | 直接聊天 | 聊天气泡 + 回复块（含操作栏） |
| 🤖 Agent | ReActLoop | Agent步骤卡片 + 最终回复块 |
| 📚 知识库 | RAG检索→AI回答 | 检索来源标注 + 回复块 |

### 块类型（Block System）

- **ChatBubble**: 用户消息气泡
- **AIReply**: AI回复，底部操作栏 [📝保存为笔记] [📋复制] [🔄重试]
- **AgentStep**: 可折叠步骤卡片（💭Thought / 🔧ToolCall / ✓Observation）
- **SearchSource**: 知识库检索来源标注
- **GeneratedImage**: AI生成图片
- **Divider**: 话题分隔线

### Agent可视化

每个 Agent 步以卡片展示，默认展开，完成后可折叠：
- 💭 思考卡片 — 淡蓝底
- 🔧 工具调用 — 淡绿底，显示工具名+参数
- ✓ 观察结果 — 淡灰底，成功绿勾/失败红叉
- ✅ 最终回答 — 正常回复块样式

### 对话→笔记转换

每条AI回复的操作栏包含：
- **保存为笔记**: 创建笔记(title=AI回复前20字, content=完整回复)
- **复制**: 复制到剪贴板
- **重试**: 重新发送上一条消息

### 输入栏

```
[+] [📎文件] [输入框...] [🎤] [📤]
```

- 多行自动扩展（最大4行）
- 切换模式时提示文本变化
- 附件菜单(BottomSheet): 拍照OCR/相册/AI描述图片/文件/浏览器/语音

### 对话历史侧边栏

- 左侧推入式侧边栏（工作区右移，非覆盖）
- 按日期分组（今天/昨天/更早）
- 顶部搜索框
- 点击对话加载

## 文件变更

### 新建文件
- `ai/components/AIWorkspace.kt` — 统一工作区（Block列表）
- `ai/components/AIReplyBlock.kt` — AI回复块+操作栏
- `ai/components/AgentStepCard.kt` — Agent步骤卡片
- `ai/components/AttachmentMenu.kt` — 附件菜单
- `ai/components/HistorySidebar.kt` — 对话历史侧边栏
- `ai/components/WorkspaceTabs.kt` — 工作区标签

### 修改文件
- `AIHomeScreen.kt` — 重构为新布局（约300行）
- `AIHomeViewModel.kt` — 添加保存为笔记、块状态管理
- `NavGraph.kt` — 侧边栏不影响路由

## 不改变
- AIChatScreen（笔记关联对话独立保留）
- AIModelSettingsScreen
- MCPSkillManageScreen
- WebBrowserScreen
- AI适配器/Agent工具链
