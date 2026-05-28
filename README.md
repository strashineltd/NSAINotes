# NSAI笔记 — 本地优先的智能笔记应用

[![Android](https://img.shields.io/badge/Android-8.0%2B-green)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

[![Download](https://img.shields.io/badge/Download-v1.10%20Stable-blue?logo=android)](https://github.com/strashineltd/NSAINotes/releases/download/v1.10-stable/app-v1.10-stable.apk)
[![Release](https://img.shields.io/github/v/release/strashineltd/NSAINotes?label=Release)](https://github.com/strashineltd/NSAINotes/releases/latest)

**NSAI笔记** 是一款以本地存储为核心、集成多模型 AI 对话能力的 Android 笔记应用。所有数据默认存储在您的设备上，您拥有数据的完全控制权。

---

## ✨ 功能特性

### 📝 笔记管理
- Markdown 写作，支持实时预览
- 笔记加密（密码锁 + 生物识别）
- 标签分类、置顶、全文搜索
- 语音输入（本地引擎，无需联网）
- 图片文字提取（OCR）和 AI 描述

### 📁 文件管理
- 本地文件浏览与组织
- 文件夹创建、重命名、删除
- 隐私文件区（密码保护）
- 系统文件浏览模式

### 🤖 AI 智能助手
- 多模型支持：DeepSeek / Kimi / GLM / Qwen / MiniMax / MiMo
- AI 对话（快速 / 思考 / 图片生成模式）
- Agent 多步推理（自主调用工具完成任务）
- 知识库检索（RAG，从您的笔记中检索）
- 对话历史管理
- 打字机效果（流式文本显示）

### 🔌 插件与扩展
- **Skill 插件**：自定义 AI 行为和提示词模板
  - 内置技能：代码助手、翻译、润色、灵感...等
  - 支持外部 API / Webhook 类型
- **MCP 服务器**：Model Context Protocol 扩展
  - 支持 SSE / Streamable HTTP 传输

### 🌐 内置浏览器
- 多搜索引擎切换（Google / 百度 / Bing / DuckDuckGo / 自定义）
- 书签管理、搜索历史
- 反 WebView 检测（UA 伪装、navigator.webdriver 清除）
- 桌面版 / 移动版切换
- 边缘滑动返回手势

### 🔒 隐私与安全
- 所有数据默认本地存储
- API Key 使用 Android Keystore 加密
- 隐私笔记支持 4-6 位 PIN + 指纹保护
- 系统云备份自动排除 API Key
- 零用户追踪、零分析 SDK

### ⚡ 性能优化
- 帧率监控（FrameMonitor）
- 自适应动画降级（FluidityManager）
- 列表项动画（animateItem）
- 导航过渡动画
- 内存压力响应

---

## 🏗️ 技术架构

| 层级 | 技术栈 |
|------|--------|
| **UI** | Jetpack Compose (Material3) |
| **导航** | Jetpack Navigation Compose |
| **DI** | Hilt (Dagger) |
| **存储** | Room (SQLite) + DataStore (Preferences) |
| **嵌入** | ONNX Runtime (Bert 分词器) |
| **AI** | OkHttp + OpenAI 兼容 API |
| **安全** | Android Keystore + Biometric |
| **构建** | Gradle Kotlin DSL + KSP |

### 项目结构

```
app/src/main/java/com/nsai/notes/
├── di/                    # 依赖注入模块
├── data/
│   ├── local/             # Room DAO、DataStore、加密
│   ├── remote/            # AI API、Web 搜索、更新检查
│   └── repository/        # 数据仓库实现
├── domain/
│   ├── agent/             # AI Agent（ReAct 循环、工具注册）
│   ├── model/             # 数据模型
│   ├── memory/            # 记忆提取
│   ├── multimodal/        # OCR、图片识别
│   ├── repository/        # 仓库接口
│   └── usecase/           # 用例层
└── presentation/
    ├── ai/                # AI 对话、模型设置、MCP/Skill 管理
    ├── common/            # 公共组件（生物识别门禁等）
    ├── files/             # 文件管理
    ├── memory/            # 记忆查看
    ├── navigation/        # 导航图 + 底部导航栏
    ├── notes/             # 笔记列表、编辑
    ├── rag/               # 知识库检索
    ├── settings/          # 设置页面
    ├── tags/              # 标签管理
    └── theme/             # 主题、颜色、字体、动画配置
```

---

## 📱 系统要求

- **最低版本**：Android 8.0 (API 26)
- **目标版本**：Android 14 (API 35)
- **推荐设备**：4GB RAM 以上

---

## 🔨 构建指南

### 环境要求

- **JDK**：17+
- **Android Studio**：Hedgehog (2023.1) 或更高
- **Gradle**：8.9（使用 wrapper，无需手动安装）

### 构建步骤

```bash
# 1. 克隆项目
git clone https://github.com/strashineltd/NSAINotes.git
cd NSAINotes

# 2. 配置 local.properties（或复制模板）
cp local.properties.template local.properties
# 编辑 local.properties，设置 SDK 路径

# 3. 构建 Debug APK
./gradlew assembleDebug     # macOS / Linux
gradlew.bat assembleDebug    # Windows

# 4. 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

### AI 模型配置

应用安装后，在「设置 → AI 模型设置」中配置各 AI 服务商的 API Key：

| 服务商 | API Key 获取地址 |
|--------|------------------|
| DeepSeek | https://platform.deepseek.com |
| Kimi (月之暗面) | https://platform.moonshot.cn |
| GLM (智谱) | https://open.bigmodel.cn |
| Qwen (通义千问) | https://dashscope.aliyun.com |
| MiniMax | https://platform.minimaxi.com |
| MiMo | 由服务商提供 |

---

## 🔒 隐私协议

完整隐私协议请在应用内「设置 → 隐私协议」查看。

**核心承诺：**
- 所有数据默认存储在您的设备本地
- 不收集任何个人身份信息
- AI 对话直连您配置的服务商，本应用不中转
- API Key 使用设备级加密存储
- 零分析 SDK、零广告、零追踪

---

## 📢 联系与反馈

- **QQ 频道**：strashine26518
- **GitHub Issues**：https://github.com/strashineltd/NSAINotes/issues
- **开源许可**：MIT License

欢迎提交 Issue、PR 或加入 QQ 频道交流！

---

## 📄 License

```
MIT License

Copyright (c) 2025 NSAI

Permission is hereby granted, free of charge, to any person...
```

---

*Made with ❤️ for privacy-first note-taking.*
