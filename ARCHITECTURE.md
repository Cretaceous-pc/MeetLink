# MeetLink 架构设计文档

> **MeetLink**（linyumini二改）—— 轻量级全栈实时在线聊天室系统

---

## 目录

1. [项目概述](#1-项目概述)
2. [系统架构总览](#2-系统架构总览)
3. [后端架构](#3-后端架构)
4. [前端架构](#4-前端架构)
5. [实时通信设计](#5-实时通信设计)
6. [数据库设计](#6-数据库设计)
7. [安全设计](#7-安全设计)
8. [部署架构](#8-部署架构)
9. [技术选型说明](#9-技术选型说明)

---

## 1. 项目概述

MeetLink 是一款面向中小团队的全栈即时通讯系统，提供文字消息、群组聊天、WebRTC 音视频通话、文件传输和 AI 聊天机器人等完整功能。系统采用前后端分离架构，后端以 Spring Boot + Netty 为核心，前端基于 Vue 3 + Vite 构建，支持多种数据库（MariaDB/PostgreSQL/SQLite）和 Docker 一键部署。

### 核心能力

| 能力 | 说明 |
|------|------|
| 实时消息 | 基于 Netty WebSocket，支持文字、图片、引用回复、@提及 |
| 群组管理 | 创建/加入/解散房间，邀请码机制，成员角色管理 |
| WebRTC 通话 | 点对点音视频通话，信令通过 WebSocket 中继 |
| 文件传输 | WebRTC DataChannel 点对点传文件 |
| AI 机器人 | 接入豆包（Doubao）和 DeepSeek 大模型 |
| 多数据库 | 自动识别 MariaDB/PostgreSQL/SQLite，启动时建表 |
| Docker 部署 | docker-compose 三服务编排，开箱即用 |

---

## 2. 系统架构总览

```
                               ┌──────────────────────────────┐
                               │       Nginx 反向代理           │
                               │   :80 → web / :9200 → server   │
                               └──────────┬───────────────────┘
                                          │
                  ┌───────────────────────┼───────────────────────┐
                  │                       │                       │
          ┌───────▼───────┐     ┌────────▼────────┐     ┌───────▼───────┐
          │  meetlink-web  │     │  meetlink-server │     │    Database    │
          │  (Vue 3 SPA)   │     │  (Spring Boot)   │     │ MariaDB/PG/   │
          │                │     │                  │     │ SQLite        │
          │  Port: 80      │     │  HTTP :9200      │     └───────────────┘
          │  nginx:alpine  │     │  WS   :9100      │
          └───────┬───────┘     │  SSH  :9222(可选) │
                  │             └────────┬─────────┘
                  │                      │
                  │  HTTP REST API       │ WebSocket
                  │  /api/v1/*           │ ws://host:9100/ws
                  │                      │
                  └──────────────────────┘
```

### 通信模型

```
用户浏览器
  │
  ├──(1)── HTTP REST :9200 ──→ JWT Filter ──→ Controller ──→ Service ──→ MyBatis-Plus ──→ DB
  │        登录/房间/用户/消息等 CRUD
  │
  ├──(2)── WebSocket :9100 ──→ Netty Server ──→ WebSocketService ──→ 消息广播/存储
  │        实时聊天、在线状态、通知推送
  │
  ├──(3)── WebRTC P2P ──→ VideoController / FileController（仅信令中继）
  │        音视频流 / 文件数据直传
  │
  └──(4)── SSH :9222（可选）──→ Apache SSHD ──→ 运维命令行管理
```

---

## 3. 后端架构

### 3.1 分层结构

```
┌─────────────────────────────────────────────────────┐
│                     Controller 层                     │
│  LoginController  RoomController  UserController     │
│  MessageController  FileController  VideoController  │
│  ChatListController  NotifyController  AdminController│
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                     Service 层                        │
│  LoginService  RoomService  MessageService           │
│  WebSocketService  AiChatService  FileService        │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                     Mapper 层                         │
│  UserMapper  GroupMapper  MessageMapper              │
│  ChatListMapper  RoomMemberMapper                    │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                     Database                          │
│  MariaDB / PostgreSQL / SQLite（自动识别）            │
└─────────────────────────────────────────────────────┘
```

### 3.2 核心模块

#### 3.2.1 WebSocket 模块 (`websocket/`)

基于 Netty 实现的高性能 WebSocket 服务器，是实时通信的核心。

- **启动方式**: `@PostConstruct` 在 Spring 容器初始化后自动启动，监听端口 `9100`
- **连接生命周期**:
  - 握手阶段通过 `HttpHeadersHandler` 从 URL 参数提取 JWT Token 和 RoomId 并校验
  - `NettyWebSocketServerHandler` 管理 `channelActive`（上线）、`channelInactive`（下线）、`userEventTriggered`（心跳超时）
  - 心跳检测使用 Netty `IdleStateHandler`，30 秒读空闲断开
- **连接管理**: 4 个线程安全的 `ConcurrentHashMap`：
  - `Online_User` — 用户ID → 用户信息
  - `Online_Channel` — 用户ID → Channel
  - `Room_Users` — 房间ID → 用户ID集合
  - `User_Room` — 用户ID → 房间ID
- **消息处理**: 统一的 JSON 消息协议，`type` 字段区分 `msg`（聊天）、`notify`（通知）、`video`（WebRTC 信令）、`file`（文件传输）、`disband`（解散房间）

#### 3.2.2 安全模块

- **认证**: JWT 无状态认证，`AuthenticationTokenFilter` 拦截所有请求，通过 `@UrlFree` 注解标记无需认证的 URL
- **加密**: RSA 非对称加密保护密码传输，前端 `jsencrypt` 加密 → 后端 `SecurityUtil` 解密
- **限流**: `@UrlLimit` 注解 + AOP 切面实现接口级别限流
- **敏感词**: 基于 `sensitive-word` 库的内容过滤

#### 3.2.3 AI 对话模块 (`service/impl/`)

支持两种大模型接入：

- **豆包 (Doubao)**: 通过火山引擎 SDK 调用 `DoubaoAiService`
- **DeepSeek**: 通过火山引擎 SDK 调用 `DeepSeekAiService`
- 统一的 `AiChatService` 接口，在 WebSocket 消息流中以机器人身份回复

#### 3.2.4 SSH 模块 (`ssh/`)

基于 Apache SSHD 实现的可选运维终端：

- 命令注册机制 `CommandManager`，支持 `help`、`msg`、`message` 等命令
- `InteractionConnect` 处理交互式命令行会话

### 3.3 请求处理流程

```
HTTP Request
  │
  ▼
AuthenticationTokenFilter（JWT 校验）
  │
  ├── @UrlFree 标记 → 跳过认证
  │
  ▼
UrlLimitAspect（限流检查）
  │
  ▼
Controller（参数绑定、校验）
  │
  ▼
Service（业务逻辑 + Caffeine 缓存）
  │
  ▼
Mapper（MyBatis-Plus + XML）
  │
  ▼
Database
```

### 3.4 启动初始化

`MeetLinkApplication` 启动后执行：

1. **NettyWebSocketServer** — 启动 WebSocket 服务（端口 9100）
2. **DatabaseInitializer** — 检测数据库类型 → 执行对应 DDL 脚本建表
3. **UrlPassRunner** — 扫描 `@UrlFree` 注解，构建免认证 URL 白名单
4. **SshServerService** — 按配置决定是否启动 SSH 终端（端口 9222）

---

## 4. 前端架构

### 4.1 技术栈

```
Vue 3.5 (Composition API)  +  Vite 6  +  TailwindCSS 3
        │
        ├── Pinia 2.3 (状态管理 + 持久化)
        ├── Vue Router 4 (SPA 路由)
        ├── axios (HTTP 请求)
        ├── mitt (组件事件总线)
        ├── highlight.js / markdown-it (消息渲染)
        └── gsap (动画)
```

### 4.2 路由设计

| 路径 | 页面 | 说明 |
|------|------|------|
| `/login` | `LoginPage.vue` | 登录页，RSA 加密传输密码 |
| `/rooms` | `RoomSelectPage.vue` | 房间列表，创建/加入房间 |
| `/chat/:roomId` | `ChatPage.vue` | 聊天主界面（46KB 核心页面） |

### 4.3 状态管理 (Pinia)

| Store | 职责 |
|-------|------|
| `useUserInfoStore` | 用户信息、Token、持久化登录态 |
| `useGlobalStore` | 全局 UI 状态 |
| `useThemeStore` | 主题切换（浅色/深色） |
| `useChatMsgStore` | 聊天消息状态管理 |
| `useGroupStore` | 房间/群组信息 |

### 4.4 组件架构

```
App.vue
├── LoginPage.vue
└── RoomSelectPage.vue
    └── ChatPage.vue
        ├── Msg/               (消息展示组件族)
        │   ├── TextMsg.vue
        │   ├── ImageMsg.vue
        │   └── SystemMsg.vue
        ├── VideoChat.vue      (WebRTC 视频通话)
        ├── FileTransfer.vue   (WebRTC 文件传输)
        ├── ChatInput.vue      (消息输入 + 表情/图片)
        ├── RoomSidebar.vue    (房间信息侧栏)
        └── ThemeToggle.vue    (主题切换)
```

### 4.5 WebSocket 客户端 (`utils/ws.js`)

- **自动重连**: 断线后指数退避重连，最多 200 次
- **心跳**: 客户端每 9.9 秒发送心跳包，匹配服务端 30 秒超时
- **消息分发**: 根据 `type` 字段路由到 Pinia Store 或 mitt 事件总线

---

## 5. 实时通信设计

### 5.1 WebSocket 消息协议

所有 WebSocket 消息采用统一 JSON 格式：

```json
{
  "type": "msg",
  "data": {
    "fromId": "xxx",
    "toId": "yyy",
    "message": "Hello",
    "messageType": "text",
    "referenceMsg": null,
    "atUser": null
  }
}
```

### 5.2 消息类型

| type | 方向 | 说明 |
|------|------|------|
| `msg` | 双向 | 聊天消息，含文字/图片/引用/at |
| `notify` | 服务端→客户端 | 系统通知（上线/下线/入群/退群） |
| `video` | 双向 | WebRTC 信令（offer/answer/ICE candidate） |
| `file` | 双向 | 文件传输信令 |
| `disband` | 服务端→客户端 | 房间被解散通知 |

### 5.3 消息流转

```
发送方浏览器 ──WS──→ Netty Server
                       │
                       ▼
                 WebSocketService
                       │
                  ┌────┴────┐
                  ▼         ▼
             存储到DB    广播到房间内其他用户
             (Message      (遍历 Room_Users Map
             Mapper)        → writeAndFlush)
```

### 5.4 WebRTC 信令中继

MeetLink 不转发媒体流，仅中继 WebRTC 信令：

```
客户端A ──WS(video offer)──→ Netty ──WS(video offer)──→ 客户端B
客户端A ←──WS(video answer)── Netty ←──WS(video answer)── 客户端B
客户端A ←──WS(ICE)──────────→ Netty ←──WS(ICE)──────────→ 客户端B

                    ※ 媒体流 P2P 直连，不经过服务器
```

---

## 6. 数据库设计

### 6.1 ER 图

```
┌──────────┐       ┌──────────────┐       ┌──────────┐
│   user   │       │  room_member │       │  group   │
├──────────┤       ├──────────────┤       ├──────────┤
│ id (PK)  │──┐    │ user_id (FK) │    ┌──│ id (PK)  │
│ name     │  │    │ group_id(FK) │    │  │ name     │
│ type     │  │    │ role         │    │  │ avatar   │
│ avatar   │  │    │ muted_until  │    │  │ owner_id │
│ email    │  ├───→│ ...          │←───┘  │ password │
│ badge    │  │    └──────────────┘       │ max_mem  │
│ role     │  │                           └──────────┘
└──────────┘  │
              │    ┌──────────────┐       ┌──────────────┐
              │    │   message    │       │  chat_list   │
              │    ├──────────────┤       ├──────────────┤
              ├───→│ from_id (FK) │       │ user_id (FK) │
              │    │ to_id (FK)   │       │ target_id    │
              │    │ message      │       │ unread_count │
              │    │ reference_msg│       │ last_message │
              │    │ at_user      │       │ type         │
              │    │ type         │       └──────────────┘
              │    └──────────────┘
              │    ┌──────────────┐       ┌──────────────┐
              │    │   notify     │       │ invite_code  │
              │    ├──────────────┤       ├──────────────┤
              └───→│ user_id (FK) │       │ code         │
                   │ content      │       │ group_id (FK)│
                   │ is_read      │       │ expire_time  │
                   └──────────────┘       └──────────────┘
```

### 6.2 数据表说明

| 表名 | 核心字段 | 说明 |
|------|---------|------|
| `user` | id, name, type, avatar, email, badge, role | 用户账户 |
| `group` | id, name, avatar, password, description, max_members, owner_id | 聊天房间/群组 |
| `room_member` | user_id, group_id, role, muted_until | 多对多成员关系 + 禁言 |
| `message` | from_id, to_id, from_info, message, reference_msg, at_user, type, source | 聊天消息（支持引用和@） |
| `chat_list` | user_id, target_id, unread_count, last_message, type | 用户聊天列表 |
| `notify` | user_id, content, is_read | 系统通知 |
| `invite_code` | code, group_id, expire_time | 房间邀请码 |

### 6.3 多数据库策略

`DatabaseInitializer` 在应用启动时通过 JDBC URL 前缀自动识别数据库类型：

| URL 前缀 | 数据库 | DDL 脚本 |
|----------|--------|----------|
| `jdbc:mariadb` 或 `jdbc:mysql` | MariaDB/MySQL | `meetlink-mysql.sql` |
| `jdbc:postgresql` | PostgreSQL | `meetlink-pg.sql` |
| `jdbc:sqlite` | SQLite | `meetlink-sqlite.sql` |

---

## 7. 安全设计

### 7.1 认证流程

```
┌─────────┐                    ┌──────────────┐               ┌──────────┐
│  Browser │                    │  meetlink-   │               │ Database │
│ (Vue 3) │                    │  server      │               │          │
└────┬────┘                    └──────┬───────┘               └────┬─────┘
     │                               │                            │
     │  GET /public-key              │                            │
     │──────────────────────────────→│                            │
     │                               │                            │
     │  RSA 公钥                     │                            │
     │←──────────────────────────────│                            │
     │                               │                            │
     │  POST /verify                 │                            │
     │  {email, password(加密)}      │                            │
     │──────────────────────────────→│                            │
     │                               │  RSA 解密 → BCrypt 比对    │
     │                               │───────────────────────────→│
     │                               │←───────────────────────────│
     │  JWT Token + 用户信息         │                            │
     │←──────────────────────────────│                            │
     │                               │                            │
     │  后续请求 Header:             │                            │
     │  Authorization: Bearer <JWT>  │                            │
     │──────────────────────────────→│  JWT 校验 → 放行           │
```

### 7.2 安全措施总结

| 层面 | 措施 | 实现 |
|------|------|------|
| 传输安全 | HTTPS（生产环境 nginx 配置） | `deploy/compose/https/` |
| 密码传输 | RSA 非对称加密 | `SecurityUtil` + `jsencrypt` |
| 密码存储 | BCrypt 哈希 | Spring Security Crypto |
| 身份认证 | JWT 无状态令牌 | `JwtUtil` + `AuthenticationTokenFilter` |
| 接口限流 | 注解驱动的 AOP 限流 | `@UrlLimit` + `UrlLimitAspect` |
| 内容安全 | 敏感词过滤 | `sensitive-word` 库 |
| 数据清理 | 定时清理过期数据 | `ExpiredClearTask` 每天 0 点 |

---

## 8. 部署架构

### 8.1 Docker Compose 部署

```
┌──────────────────────────────────────────────────┐
│                  Docker Host                      │
│                                                   │
│  ┌─────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │  nginx  │  │ meetlink-web │  │   MariaDB    │  │
│  │ (proxy) │  │  :80         │  │   :3306      │  │
│  │  :443   │  │  nginx:alpine│  │              │  │
│  └────┬────┘  └──────┬───────┘  └──────┬───────┘  │
│       │              │                  │          │
│       │  ┌───────────▼──────────────────┤          │
│       │  │    meetlink-server            │          │
│       │  │    HTTP :9200 / WS :9100      │          │
│       │  │    spring.profiles: docker    │          │
│       │  └──────────────────────────────┘          │
│       │                                            │
│       └── 外部流量 :80/:443                         │
└──────────────────────────────────────────────────┘
```

### 8.2 容器化构建

| 服务 | 基础镜像 | 构建方式 |
|------|---------|---------|
| `meetlink-server` | `eclipse-temurin:21-jre-alpine` | 多阶段：Maven 构建 → JRE 运行 |
| `meetlink-web` | `nginx:alpine` | Vite 构建 → nginx 静态服务 |

### 8.3 环境变量注入

Docker 环境下，所有配置通过环境变量注入 `application-docker.yml`：

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — 数据库连接
- `MEETLINK_PASSWORD`, `MEETLINK_ADMIN_EMAIL` — 管理员初始化
- `MEETLINK_ONLINE_NUM` — 在线人数限制
- `MEETLINK_JWT_KEY` — JWT 签名密钥
- `MEETLINK_FILE_SIZE` — 文件大小限制
- `MEETLINK_AI_API_KEY` — AI 服务 API Key

---

## 9. 技术选型说明

### 9.1 为什么选择 Netty 而非 Spring WebSocket？

| 维度 | Netty | Spring WebSocket |
|------|-------|------------------|
| 并发模型 | NIO + EventLoop，单机十万连接 | 基于 Servlet 容器，受线程池限制 |
| 内存管理 | 零拷贝、池化 ByteBuf | 标准 JVM 堆内存 |
| 协议扩展 | 原生支持 HTTP/WS/TCP/UDP 多协议 | 仅 WS over HTTP |
| 资源控制 | 精确的 I/O 超时和背压控制 | 依赖容器配置 |

对于聊天室场景，Netty 的非阻塞 I/O 模型更适合高并发长连接。

### 9.2 为什么支持多种数据库？

- **SQLite**: 单机零配置部署，降低试用门槛
- **MariaDB/MySQL**: 中小规模生产环境首选
- **PostgreSQL**: 大规模部署、需要高级查询功能

通过 `DatabaseInitializer` 自动检测并执行对应 DDL，用户只需修改连接 URL。

### 9.3 为什么前后端分离？

- **独立开发**: 前端 Vue 生态热更新，后端 Java 无需重启
- **独立部署**: 前端静态文件 CDN/nginx，后端容器化
- **技术演进**: 前端框架可替换而不影响后端 API

### 9.4 技术版本矩阵

| 组件 | 版本 | 选型理由 |
|------|------|---------|
| Java | 21 LTS | 虚拟线程预备、长期支持 |
| Spring Boot | 2.7.18 | 2.x 最后稳定版，生态成熟 |
| Netty | 4.1.108 | 稳定版，性能经过大量验证 |
| Vue | 3.5 | Composition API + 更好的 TS 支持 |
| Vite | 6.0 | 极速冷启动和 HMR |
| TailwindCSS | 3.4 | 原子化 CSS，开发效率高 |
| MyBatis-Plus | 3.5.3 | 简化 CRUD，Lambda 查询 |
| Caffeine | 2.9.3 | JVM 本地缓存最优选择 |

---

## 附录 A. 项目结构

```
MeetLink/
├── LICENSE                     # MIT 开源协议（根项目）
├── ARCHITECTURE.md             # 本文档
├── .gitignore
├── meetlink-server/            # Java 后端
│   ├── LICENSE                 # Apache 2.0（服务端原始协议）
│   ├── pom.xml
│   ├── Dockerfile
│   ├── deploy/
│   │   ├── compose/            # docker-compose 编排
│   │   └── pg/                 # PostgreSQL 部署配置
│   └── src/main/
│       ├── java/com/cheng/meetlink/
│       │   ├── MeetLinkApplication.java
│       │   ├── configs/        # Spring 配置
│       │   ├── controller/     # REST API (9个)
│       │   ├── service/        # 业务逻辑
│       │   ├── websocket/      # Netty WS 服务
│       │   ├── filter/         # JWT 认证过滤
│       │   ├── aop/            # 限流切面
│       │   ├── ssh/            # SSH 终端
│       │   ├── schedule/       # 定时任务
│       │   ├── mapper/         # MyBatis Mapper
│       │   ├── entity/         # 数据库实体
│       │   ├── dto/vo/         # 数据传输对象
│       │   └── utils/          # 工具类
│       └── resources/
│           ├── application.yml
│           ├── application-docker.yml
│           └── *.sql           # 建表脚本
│
└── meetlink-web/               # Vue 3 前端
    ├── package.json
    ├── Dockerfile
    ├── vite.config.js
    └── src/
        ├── main.js
        ├── App.vue
        ├── api/                # API 封装 (7个模块)
        ├── stores/             # Pinia 状态管理 (5个)
        ├── views/              # 页面 (3个)
        ├── components/         # 通用组件 (25+)
        ├── router/             # 路由配置
        └── utils/              # 工具函数 + WS 客户端
```

---

## 附录 B. 端口清单

| 端口 | 服务 | 说明 |
|------|------|------|
| 80 | meetlink-web (nginx) | 前端静态页面 |
| 443 | nginx (HTTPS) | 生产环境 HTTPS |
| 9200 | meetlink-server (HTTP) | REST API |
| 9100 | meetlink-server (WS) | WebSocket 实时通信 |
| 9222 | meetlink-server (SSH) | SSH 终端（可选） |
| 3306 | MariaDB | 数据库 |

---

## 附录 C. 开源协议

本项目采用双协议授权：

- **根项目（`LICENSE`）**
- **服务端（`meetlink-server/LICENSE`）**

使用者可根据需要选择适用的协议条款。服务端代码同时受 Apache 2.0 专利授权条款保护。

