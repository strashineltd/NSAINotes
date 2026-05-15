# NSAI 后台管理平台设计文档

## 概述

为 NSAI 笔记 Android 应用搭建独立后端管理平台，实现收费管理、激活码自动生成和可视化仪表盘三大核心功能。

**定价模型**: 多产品多层级（基础版/Pro 版等），¥5/年起  
**技术栈**: Rust (Axum) + PostgreSQL + React (Ant Design Pro)  
**签名体系**: RSA 2048 SHA256withRSA，与 App 端 LicenseVerifier.kt 完全兼容

---

## 1. 用户流程

```
管理员创建商品 → 生成支付二维码(含链接)
                       ↓
                用户扫码 → 完成支付
                       ↓
                支付回调 → RSA 签名 → 生成激活码
                       ↓
              付款成功页展示激活码 → 用户复制到 App
```

---

## 2. 数据库设计

### products（商品/定价方案）

| 列 | 类型 | 说明 |
|---|---|---|
| id | UUID PK | |
| name | VARCHAR(100) | 商品名称，如"NSAI笔记基础版" |
| description | TEXT | 功能说明 |
| price | BIGINT | 金额，单位：分 |
| duration_days | INT | 有效期天数 |
| features | JSONB | 功能标识列表 |
| is_active | BOOLEAN | 是否上架 |
| created_at | TIMESTAMPTZ | |

### orders（订单表）

| 列 | 类型 | 说明 |
|---|---|---|
| id | UUID PK | |
| product_id | UUID → products.id | |
| device_id | VARCHAR(24) | 用户设备码(原始) |
| amount | BIGINT | 实付金额(分) |
| status | ENUM(pending/paid/expired/refunded) | |
| payment_method | VARCHAR(20) | wechat/alipay/manual |
| payment_qr_url | TEXT | 支付二维码链接 |
| paid_at | TIMESTAMPTZ | |
| created_at | TIMESTAMPTZ | |

### activation_codes（激活码表）

| 列 | 类型 | 说明 |
|---|---|---|
| id | UUID PK | |
| order_id | UUID → orders.id | |
| code | VARCHAR(30) | 如 NSAI-XXXX-XXXX-XXXX |
| raw_device_id | VARCHAR(24) | 下单时设备码 |
| expire_timestamp | BIGINT | 到期时间戳(ms) |
| signature | TEXT | RSA 签名(hex) |
| bind_device_id | VARCHAR(24) | 实际激活绑定的设备 |
| activated_at | TIMESTAMPTZ | |
| status | ENUM(unused/active/expired) | |
| created_at | TIMESTAMPTZ | |

### payment_records（支付流水）

| 列 | 类型 | 说明 |
|---|---|---|
| id | UUID PK | |
| order_id | UUID → orders.id | |
| provider | VARCHAR(20) | wechat/alipay |
| provider_trade_no | VARCHAR(64) | 支付平台订单号 |
| amount | BIGINT | |
| status | ENUM(pending/success/failed) | |
| raw_callback | JSONB | 支付回调原始数据 |

### dashboard_metrics（预聚合统计）

| 列 | 类型 | 说明 |
|---|---|---|
| date | DATE PK | |
| new_orders | INT | 新增订单数 |
| revenue | BIGINT | 总收入(分) |
| active_users | INT | 活跃用户数 |
| expired_today | INT | 当日过期数 |
| updated_at | TIMESTAMPTZ | |

---

## 3. 后端模块

### 项目结构

```
nsai-admin/
├── src/
│   ├── main.rs
│   ├── config.rs               # 配置管理(数据库/RSA私钥/支付密钥)
│   ├── router.rs                # 路由注册
│   ├── db/
│   │   ├── mod.rs
│   │   ├── products.rs
│   │   ├── orders.rs
│   │   ├── activation_codes.rs
│   │   ├── payment_records.rs
│   │   └── metrics.rs
│   ├── api/
│   │   ├── mod.rs
│   │   ├── products.rs          # 商品 CRUD
│   │   ├── orders.rs            # 订单管理
│   │   ├── activation.rs        # 激活码生成/查询
│   │   ├── payment.rs           # 支付二维码/回调
│   │   ├── dashboard.rs         # 仪表盘数据
│   │   └── auth.rs              # 管理员 JWT 登录
│   ├── services/
│   │   ├── mod.rs
│   │   ├── license_signer.rs    # RSA 签名生成
│   │   ├── payment_gateway.rs   # 支付对接抽象层
│   │   └── qr_generator.rs      # 二维码生成
│   └── models/
│       ├── mod.rs
│       ├── product.rs
│       ├── order.rs
│       ├── activation_code.rs
│       └── payment.rs
├── migrations/
├── frontend/
└── Cargo.toml
```

### API 路由

```
# 认证
POST /api/admin/login
POST /api/admin/refresh

# 商品管理
GET    /api/products
POST   /api/products
PUT    /api/products/:id
DELETE /api/products/:id

# 订单管理
GET    /api/orders
GET    /api/orders/:id
POST   /api/orders
POST   /api/orders/:id/cancel

# 支付
POST /api/payment/notify/alipay
POST /api/payment/notify/wechat
POST /api/payment/manual/:id

# 激活码
POST /api/activation/generate
GET  /api/activation/:code
GET  /api/activation

# 仪表盘
GET /api/dashboard/overview
GET /api/dashboard/revenue
GET /api/dashboard/activations
GET /api/dashboard/products
```

### RSA 签名兼容性

App 端 `LicenseVerifier.kt` 的验证逻辑:
- 算法: SHA256withRSA
- 数据结构: `deviceId(12B) + expireTimestamp(8B) + signature(256B)` → Base64
- 激活码格式: `NSAI` + Base64 载荷，展示为 `NSAI-XXXX-XXXX-XXXX`

后端 `license_signer.rs` 使用私钥签名，生成完全兼容的格式。

---

## 4. 前端布局

### 页面结构

```
侧栏菜单:               主内容区:
├── 📊 仪表盘           顶部: 概览卡片(今日收入/订单/激活/用户)
├── 📦 商品管理          中部: 收入趋势 + 激活趋势图表
├── 📋 订单管理          底部: 商品销量排行 + 最近订单列表
├── 🔑 激活码管理
├── 💰 支付记录
└── ⚙️ 系统设置
```

### 关键技术选型

- React 18 + TypeScript
- Ant Design 5.x 组件库
- @ant-design/charts 图表库（基于 G2Plot）
- React Router 6 路由
- MSAL/JWT 认证

---

## 5. 部署方案

```
Rust 编译为单一二进制 → 直接部署到 Linux 服务器
前端构建为静态文件 → 由 Rust 二进制内嵌提供服务 (使用 rust-embed)
或 Nginx 反向代理

依赖: PostgreSQL 数据库 (可独立部署或用云服务 RDS)
环境变量: 数据库 URL、RSA 私钥、支付密钥
```
