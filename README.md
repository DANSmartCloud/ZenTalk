# ZenTalk - Java实时通讯服务器
### Version: 1.5.1


## 项目概述

ZenTalk是一个基于Java的实时通讯服务器。项目提供了完整的即时通讯解决方案，包括用户管理、好友系统、实时聊天等功能。服务端采用模块化设计，支持高并发连接，并提供完善的安全机制和API接口。

## 功能特性

- 用户注册、登录、密码重置
- 好友关系管理（添加、删除、查找）
- 实时文本聊天
- 系统通知
- 命令行管理控制台
- 可配置的权限控制
- WebSocket实时通信
- JWT身份验证
- 心跳检测机制
- 消息确认与重试机制
- 离线消息存储与同步
- 控制台命令：
  - 用户管理 (封禁/解封/冻结/解冻/删除/创建/重置密码/发送验证码)
  - 好友管理 (请求/同意/拒绝/删除/备注/搜索)
  - 用户信息 (查看/编辑资料/设置在线状态/查询在线状态)
  - 用户设置 (头像/隐身/标签/改密码/夜间模式)

## 技术栈

- **核心语言**: Java 8+
- **数据库**: MongoDB + Redis
- **网络框架**: 自定义HTTP服务器 + Tyrus WebSocket实现 (v2.1.3)
- **安全**: JWT认证 (jjwt v0.11.5) + BCrypt密码加密
- **消息序列化**: Protocol Buffers (v3.22.2)
- **构建工具**: Gradle (v8.14)
- **依赖管理**: Gradle
- **日志系统**: SLF4J + Logback

## 构建说明

### 环境依赖

1. JDK 1.8+
2. Gradle 8.14+
3. MongoDB 4.0+
4. Redis 6.0+

*注意：Protocol Buffers编译器由Gradle插件自动管理，无需手动安装。*

### 构建步骤

1. 安装Protocol Buffers编译器:
   
   ```bash
   # Windows用户可通过Chocolatey安装
   choco install protobuf
   ```

# Linux用户可通过apt安装

sudo apt-get install protobuf-compiler

# macOS用户可通过Homebrew安装

brew install protobuf

```
2. 构建项目 (Gradle会自动处理Protocol Buffers代码生成):

```bash
# Windows
.\gradlew.bat build

# Linux/macOS
./gradlew build
```

3. 运行测试:
   
   ```bash
   ./gradlew build
   ```

   ```bash
# Windows
.\gradlew.bat test

# Linux/macOS
./gradlew test
   ```

## 运行指南

### 前置条件

1. 确保MongoDB服务已启动
2. 确保Redis服务已启动
3. 配置文件已正确设置

### 启动服务器

```bash
java -jar build/libs/ZenTalk-1.5.1.jar
```

*注意：自定义配置文件路径启动的功能当前版本暂未实现。请直接修改 `src/main/resources/config.properties` 文件。*

## 配置说明

配置文件位于`src/main/resources/config.properties`，主要配置项包括：

```properties
# HTTP服务器配置
server.port=8080          # HTTP服务器端口
server.ip=127.0.0.1       # HTTP服务器绑定IP (注意：默认为本地回环地址)

# 功能开关
allow.login=true          # 是否允许登录
allow.register=true       # 是否允许注册

# MongoDB配置
mongo.uri=mongodb://localhost:27017  # MongoDB连接URI
mongo.db=chat                     # 数据库名称 (注意：默认为 chat)

# Redis配置
redis.host=localhost      # Redis服务器地址
redis.port=6379           # Redis服务器端口
redis.password=           # Redis密码（可选）

# 数据库类型 (当前仅支持mongodb)
db.type=mongodb
```

**注意:**
- WebSocket服务器端口 (`websocket.port`) 当前硬编码在 `Config.java` 中，默认为 8081，未在 `config.properties` 中配置。
- JWT密钥 (`jwt.secret`) 由 `JWTSecretManager.java` 管理，当前使用默认密钥，**生产环境必须修改**。

## 项目起源

本项目最初由TYUES开发，后由HodTechStudio进行二次开发并维护当前分支版本。当前版本号为1.5.1。

## 架构设计

项目采用模块化设计，主要包结构位于 `src/main/java/com/ZenTalk/server/` 下：

- `chat`: 处理聊天消息、文件、表情和通知
- `command`: 实现命令行控制台功能，包括用户管理、好友管理、设置等
- `config`: 加载和管理系统配置
- `db`: 数据库服务，封装MongoDB和Redis操作
- `drawer`: (用途待确认，可能与UI或特定功能相关)
- `friend`: 处理好友请求、搜索和通知
- `http`: 实现HTTP服务器，处理REST API请求
- `security`: 处理JWT认证和密钥管理
- `user`: 处理用户注册、登录、密码重置、个人资料、设置和状态
- `util`: 提供日志、国际化、音频、图像等通用工具类
- `ws`: 实现WebSocket服务器，处理实时连接、消息编解码和心跳机制

### 通信架构

系统采用双通道通信架构：

1. **HTTP通道**：处理用户认证、注册等非实时操作
2. **WebSocket通道**：处理实时消息传递和状态同步

### 数据存储架构

- **MongoDB**：存储用户信息、消息历史、好友关系等持久化数据
- **Redis**：存储会话信息、验证码、在线状态等临时数据

### 安全架构

- **认证层**：基于JWT的用户身份验证
- **传输层**：WebSocket连接要求有效的JWT令牌
- **应用层**：操作权限验证和资源访问控制
- **存储层**：密码使用BCrypt加密存储

## WebSocket通信机制

### 连接建立流程

1. 客户端通过HTTP API获取JWT令牌
2. 客户端使用JWT令牌建立WebSocket连接，在请求头中包含`Authorization: Bearer <token>`
3. 服务器验证JWT令牌，提取用户ID并关联到WebSocket会话
4. 连接成功建立，服务器通知客户端连接状态

### 心跳机制

- 服务器每30秒检查客户端活跃状态
- 如果客户端超过30秒未发送消息，服务器发送ping帧
- 客户端收到ping后应回复pong帧
- 如果客户端超过90秒未响应，服务器关闭连接

### 消息类型

- **TEXT**: 文本消息，用于常规聊天
- **IMAGE**: 图片消息，同时用于心跳处理
- **COMMAND**: 命令消息，用于特殊操作如消息确认

### 消息确认机制

1. 发送方发送消息并等待确认
2. 接收方收到消息后发送ACK命令
3. 如未收到确认，系统自动重试发送

## API接口规范

所有API请求必须使用JSON格式，Content-Type设置为`application/json`。

### 通用响应格式

#### 成功响应

```json
{
  "status": "success",
  "data": {
    // 接口特定数据
  }
}
```

#### 错误响应

```json
{
  "status": "error",
  "error": {
    "code": "错误代码",
    "message": "错误描述"
  }
}
```

### 用户注册

- 路径: `POST /api/register`

- 请求参数:
  
  ```json
  {
    "username": "用户名(4-16位字母数字)",
    "password": "密码(8-32位)",
    "email": "有效邮箱地址",
    "phone": "11位手机号"
  }
  ```

- 成功响应:
  
  ```json
  {
    "status": "success",
    "data": {
      "userId": "用户ID",
      "username": "用户名"
    }
  }
  ```

- 错误代码:
  
  - `4001`: 参数格式错误
  - `4002`: 用户名已存在
  - `4003`: 邮箱已注册
  - `5001`: 服务器内部错误

### 用户登录

- 路径: `POST /api/login`

- 请求参数:
  
  ```json
  {
    "username": "用户名",
    "password": "密码"
  }
  ```

- 成功响应:
  
  ```json
  {
    "status": "success",
    "data": {
      "token": "认证令牌(JWT)",
      "expiresIn": 3600
    }
  }
  ```

- 错误代码:
  
  - `4001`: 参数格式错误
  - `4004`: 用户名或密码错误
  - `4005`: 账户被冻结

### 发送消息

- 路径: `POST /api/chat`

- 请求头:
  
  - `Authorization: Bearer <token>`

- 请求参数:
  
  ```json
  {
    "userId": "接收方用户ID",
    "message": "消息内容(1-1000字符)"
  }
  ```

- 成功响应:
  
  ```json
  {
    "status": "success",
    "data": {
      "messageId": "消息ID",
      "timestamp": 1640995200000
    }
  }
  ```

- 错误代码:
  
  - `4001`: 参数格式错误
  - `4006`: 接收方不存在
  - `4007`: 消息内容过长
  - `4010`: 未授权访问

### 发送验证码

- 路径: `POST /api/verify/send`

- 请求参数:
  
  ```json
  {
    "account": "邮箱或手机号"
  }
  ```

- 成功响应:
  
  ```json
  {
    "status": "success",
    "message": "Verification code sent"
  }
  ```

- 错误代码:
  
  - `4001`: 参数格式错误
  - `4008`: 账户不存在

### 验证验证码

- 路径: `POST /api/verify/check`

- 请求参数:
  
  ```json
  {
    "account": "邮箱或手机号",
    "code": "6位验证码"
  }
  ```

- 成功响应:
  
  ```json
  {
    "status": "success",
    "valid": true/false
  }
  ```

- 错误代码:
  
  - `4001`: 参数格式错误

### 二维码生成

- 路径: `GET /api/qrcode`

- 请求参数:
  
  - `content`: 要编码为二维码的内容(必填)
  - `size`: 二维码尺寸(可选，默认200)

- 响应:
  
  ```json
  {
    "status": "success",
    "data": {
      "image": "Base64编码的PNG图片数据"
    }
  }
  ```

- 示例请求:
  
  ```
  GET /api/qrcode?content=https://zentalk.example.com/login&size=250
  ```

- 错误代码:
  
  - `4001`: 缺少content参数
  - `4006`: 二维码内容过长
  - `5003`: 二维码生成失败

### 密码重置流程

1. 调用`/api/verify/send`发送验证码
2. 调用`/api/verify/check`验证验证码
3. 调用`/api/user/reset-pwd`重置密码

## WebSocket API

### 连接建立

- WebSocket URL: `ws://<host>:<port>/chat`
- 请求头:
  - `Authorization: Bearer <token>`

### 消息格式

客户端发送的消息应使用以下JSON格式：

```json
{
  "type": "TEXT|IMAGE|COMMAND",
  "content": "消息内容",
  "to": "接收方用户ID",
  "timestamp": 1640995200000
}
```

服务器响应的消息格式：

```json
{
  "type": "TEXT|IMAGE|COMMAND|SYSTEM",
  "content": "消息内容",
  "from": "发送方用户ID",
  "timestamp": 1640995200000,
  "messageId": "唯一消息ID"
}
```

### 特殊命令

- 消息确认: `{"type":"COMMAND","content":"ACK:<messageId>"}`
- 历史消息请求: `{"type":"COMMAND","content":"HISTORY_REQUEST"}`

## 命令行管理控制台

服务器启动后可以通过命令行执行管理操作。命令区分大小写，参数之间使用空格分隔。

### 核心命令

- `help`: 显示所有可用命令及其用法。
- `stop`: 安全地停止服务器。

### 用户管理 (基础)

- `ban <用户名或用户ID>`: 封禁指定用户。
- `unban <用户名或用户ID>`: 解封指定用户。
- `freeze <用户名或用户ID>`: 冻结指定用户账户。
- `unfreeze <用户名或用户ID>`: 解冻指定用户账户。
- `create <用户名> <密码> <邮箱> <手机号>`: 创建一个新用户。
- `deleteuser <用户名或用户ID>`: 删除指定用户及其相关数据。
- `sendcode <邮箱或手机号>`: 向指定账户发送验证码（用于密码重置）。
- `resetpwd <邮箱或手机号> <验证码> <新密码>`: 使用验证码重置用户密码。

### 好友管理 (`friend`)

- `friend request <发送方用户名/ID> <接收方用户名/ID> [可选消息]`: 发送好友请求。
- `friend accept <请求ID>`: 同意指定的好友请求。
- `friend reject <请求ID>`: 拒绝指定的好友请求。
- `friend delete <用户ID> <好友ID>`: 删除指定用户的好友关系。
- `friend remark <用户ID> <好友ID> <备注名>`: 为好友设置备注名。
- `friend search <关键词>`: 根据关键词（用户名、昵称、邮箱、手机号）搜索用户。

### 用户信息 (`user`)

- `user profile <用户ID>`: 查看指定用户的详细资料。
- `user editprofile <用户ID> <昵称> <头像URL> <性别> <生日>`: 编辑用户资料。对于不想修改的字段，请使用 `null` 作为占位符。
- `user setonline <用户ID> <true|false>`: 手动设置用户的在线状态（主要用于调试）。
- `user isonline <用户ID>`: 查询指定用户是否在线。

### 用户设置 (`settings`)

- `settings avatar <用户ID> <头像URL>`: 更新用户的头像。
- `settings invisible <用户ID> <true|false>`: 设置用户是否对他人隐身。
- `settings tags <用户ID> <标签1> [标签2] ...`: 更新用户的个人标签。
- `settings changepwd <用户ID> <旧密码> <新密码>`: 修改用户密码。
- `settings nightmode <用户ID> <true|false>`: 设置用户的夜间模式偏好。

**注意:** `<占位符>` 表示需要替换的参数。`[可选参数]` 表示该参数是可选的。

**示例:**

```
> help
# ... 显示帮助信息 ...

> create Alice pAsswOrd123 alice@email.com 13800138000
用户 'Alice' 创建成功。

> ban Bob
用户 'Bob' 已被封禁。

> friend request Alice Bob 加个好友吧
好友请求已发送。

> user profile Alice
用户 'Alice' 的资料:
  Nickname: Alice
  Avatar: default_avatar.png
  ...

> settings tags Alice 游戏 编程 音乐
用户 'Alice' 的标签已更新。
```

## 数据库结构

### MongoDB集合

- **users**: 用户信息
  
  - `_id`: 用户ID
  - `username`: 用户名
  - `password`: 加密密码
  - `email`: 邮箱
  - `phone`: 手机号
  - `status`: 状态(active/banned/frozen)
  - `created_at`: 创建时间
  - `last_login`: 最后登录时间

- **messages**: 消息记录
  
  - `_id`: 消息ID
  - `from`: 发送方ID
  - `to`: 接收方ID
  - `type`: 消息类型
  - `content`: 消息内容
  - `timestamp`: 发送时间
  - `status`: 消息状态(sent/delivered/read)

- **friends**: 好友关系
  
  - `_id`: 关系ID
  - `user_id`: 用户ID
  - `friend_id`: 好友ID
  - `status`: 关系状态(pending/accepted/blocked)
  - `created_at`: 创建时间

### Redis键值结构

- **会话令牌**: `session:<userId>` → JWT令牌
- **验证码**: `verify:<account>` → 验证码
- **在线状态**: `online:<userId>` → 上线时间戳
- **消息确认**: `ack:<messageId>` → 确认状态

## 客户端开发

### Electron客户端

1. 在项目根目录有`client`文件夹为客户端
2. 客户端为Material Design规范的Electron项目
3. 实现与服务端的API交互

### 客户端集成指南

1. **HTTP API集成**:
   
   - 使用标准HTTP客户端库
   - 遵循API文档中的请求/响应格式
   - 保存JWT令牌用于后续请求

2. **WebSocket集成**:
   
   - 建立WebSocket连接时在头部包含JWT令牌
   - 实现心跳响应机制
   - 处理各类消息类型
   - 实现消息确认机制

## 性能优化

- WebSocket连接使用心跳机制保持活跃
- 使用Redis缓存减轻数据库负担
- 消息确认机制确保可靠传递
- 使用Protocol Buffers高效序列化消息

## 故障排除

### 常见问题

1. **无法连接到服务器**
   
   - 检查服务器IP和端口配置
   - 确认防火墙设置允许连接

2. **WebSocket连接失败**
   
   - 验证JWT令牌是否有效
   - 检查Authorization头格式

3. **消息发送失败**
   
   - 确认接收方用户存在且在线
   - 检查消息格式是否正确

### 日志位置

- 服务器日志位于`logs/`目录
- 错误日志: `logs/error.log`
- 访问日志: `logs/access.log`

## 贡献指南

欢迎提交Pull Request或Issue。提交代码前请确保：

1. 通过所有测试
2. 遵循现有代码风格
3. 更新相关文档
4. 添加必要的注释
5. 编写单元测试（如适用）

### 开发环境设置

1. 克隆仓库
2. 安装依赖
3. 配置开发环境变量
4. 运行测试确保环境正常

## 版本历史

- **1.5.1** (当前): WebSocket心跳机制优化，修复消息确认bug
- **1.5.0**: 添加离线消息存储与同步
- **1.4.0**: 引入Protocol Buffers消息序列化
- **1.3.0**: 添加Redis会话管理
- **1.2.0**: 实现WebSocket实时通信
- **1.1.0**: 添加MongoDB数据持久化
- **1.0.0**: 初始版本，基本HTTP API

## 许可证

[MIT License](LICENSE)
