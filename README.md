# ZenTalk - Java 实时通讯服务器

### 版本：1.5.1


---

项目概述

ZenTalk 是一个基于 Java 的实时通讯服务器，提供完整的即时通讯解决方案，包括用户管理、好友系统、实时聊天等功能。服务端采用模块化设计，支持高并发连接，并提供完善的安全机制和 API 接口。


---

功能特性

用户管理：注册、登录、密码重置

好友关系管理：添加、删除、查找

实时文本聊天

系统通知

命令行管理控制台

权限控制（可配置）

WebSocket 实时通信

JWT 身份验证

心跳检测机制

消息确认与重试机制

离线消息存储与同步



---

技术栈


---

构建说明

环境依赖

1. JDK 1.8+


2. Gradle 8.14+


3. MongoDB 4.0+


4. Redis 6.0+



> Note: Protocol Buffers 编译器由 Gradle 插件自动管理，无需手动安装。



安装 Protocol Buffers 编译器

# Windows (Chocolatey)
choco install protobuf

# Linux (apt)
sudo apt-get install protobuf-compiler

# macOS (Homebrew)
brew install protobuf

构建项目

# Windows
./gradlew.bat build

# Linux/macOS
./gradlew build

运行测试

# Windows
./gradlew.bat test

# Linux/macOS
./gradlew test


---

运行指南

前置条件

MongoDB 服务已启动

Redis 服务已启动

src/main/resources/config.properties 已正确配置


启动服务器

java -jar build/libs/ZenTalk-1.5.1.jar

> 注意： 自定义配置文件路径功能尚未实现，请直接修改 config.properties。




---

配置说明

配置文件：src/main/resources/config.properties

# HTTP 服务器配置
server.port=8080
server.ip=127.0.0.1

# 功能开关
allow.login=true
allow.register=true

# MongoDB
mongo.uri=mongodb://localhost:27017
mongo.db=chat

# Redis
redis.host=localhost
redis.port=6379
redis.password=

# 数据库类型
db.type=mongodb

> 提示：

WebSocket 端口 (websocket.port) 默认为 8081，硬编码在 Config.java。

JWT 密钥由 JWTSecretManager.java 管理，生产环境务必修改默认密钥。





---

项目起源

最初由 TYUES 开发，后由 HodTechStudio 二次开发并维护当前分支。


---

架构设计

模块路径：src/main/java/com/ZenTalk/server/

chat：处理聊天消息、文件、表情和通知

command：命令行控制台（用户管理、好友管理等）

config：加载与管理系统配置

db：封装 MongoDB 与 Redis 操作

friend：好友请求与通知

http：REST API 实现

security：JWT 认证与密钥管理

user：用户注册、登录、资料管理

ws：WebSocket 服务与心跳机制

util：日志、国际化、音频、图像等通用工具



通信架构

1. HTTP 通道：非实时操作（认证、注册）


2. WebSocket 通道：实时消息与状态同步



数据存储

MongoDB：持久化用户信息、消息历史、好友关系

Redis：缓存会话、验证码、在线状态、消息确认状态


安全架构

1. 认证层：JWT 验证


2. 传输层：WebSocket 需携带有效 JWT


3. 应用层：权限校验


4. 存储层：BCrypt 密码存储




---

WebSocket 通信机制

连接流程

1. 客户端通过 HTTP 获取 JWT


2. 客户端使用 JWT 建立 WebSocket 连接（Authorization: Bearer <token>）


3. 服务器验证并关联用户会话


4. 连接成功，通知客户端



心跳机制

服务端每 30 秒检测活跃状态

未收消息则发送 ping，客户端应回复 pong

超过 90 秒未响应则关闭连接


消息类型

TEXT：文本消息

IMAGE：图片消息 & 心跳

COMMAND：命令（ACK、历史请求）


消息确认

1. 发送方发送消息并等待 ACK


2. 接收方回复 ACK:<messageId>


3. 超时重试




---

REST API

所有接口均使用 application/json。

通用响应

成功：

{"status":"success","data":{...}}

失败：

{"status":"error","error":{"code":"","message":""}}


用户注册

路径：POST /api/register

参数：

{
  "username":"4-16 位字母数字",
  "password":"8-32 位",
  "email":"有效邮箱",
  "phone":"11 位手机号"
}

成功：

{"status":"success","data":{"userId":"...","username":"..."}}

错误码：

4001：参数格式错误

4002：用户名已存在

4003：邮箱已注册

5001：服务器内部错误



用户登录

路径：POST /api/login

参数：

{"username":"...","password":"..."}

成功：

{"status":"success","data":{"token":"<JWT>","expiresIn":3600}}

错误码：

4001：参数格式错误

4004：用户名或密码错误

4005：账户被冻结



发送消息

路径：POST /api/chat

头部：Authorization: Bearer <token>

参数：

{"userId":"接收方 ID","message":"1-1000 字符"}

成功：

{"status":"success","data":{"messageId":"...","timestamp":1640995200000}}

错误码：

4001：参数格式错误

4006：接收方不存在

4007：消息内容过长

4010：未授权访问



验证码接口

发送验证码：

POST /api/verify/send

参数：{"account":"邮箱或手机号"}

错误码：4001、4008


验证验证码：

POST /api/verify/check

参数：{"account":"...","code":"6 位"}

成功：{"status":"success","valid":true/false}

错误码：4001



二维码生成

路径：GET /api/qrcode?content=<内容>&size=<尺寸>

响应：

{"status":"success","data":{"image":"Base64 PNG"}}

错误码：

4001：缺少 content

4006：内容过长

5003：生成失败




---

命令行管理控制台

启动后在控制台输入命令，区分大小写，参数间以空格分隔。

核心命令

help：显示所有命令

stop：安全停止服务器


用户管理

ban <用户名/ID>       // 封禁用户
unban <用户名/ID>     // 解封用户
freeze <用户名/ID>     // 冻结账户
unfreeze <用户名/ID>   // 解冻账户
create <用户名> <密码> <邮箱> <手机号>  // 创建用户
deleteuser <用户名/ID> // 删除用户
sendcode <账号>       // 发送验证码
resetpwd <账号> <验证码> <新密码>  // 重置密码

好友管理

friend request <发> <收> [消息]   // 发送好友请求
friend accept <请求ID>          // 同意请求
friend reject <请求ID>          // 拒绝请求
friend delete <用户ID> <好友ID> // 删除好友
friend remark <用户ID> <好友ID> <备注>
friend search <关键词>         // 搜索用户

用户信息

user profile <用户ID>            // 查看资料
user editprofile <用户ID> <昵称> <头像URL> <性别> <生日>
user setonline <用户ID> <true|false>
user isonline <用户ID>

设置

settings avatar <用户ID> <URL>
settings invisible <用户ID> <true|false>
settings tags <用户ID> <标签1> [标签2]...
settings changepwd <用户ID> <旧密码> <新密码>
settings nightmode <用户ID> <true|false>


---

数据库结构

MongoDB 集合

users：

_id、username、password、email、phone

status (active/banned/frozen)

created_at、last_login


messages：

_id、from、to、type、content

timestamp、status (sent/delivered/read)


friends：

_id、user_id、friend_id, status (pending/accepted/blocked)

created_at



Redis 键值

session:<userId> → JWT

verify:<account> → 验证码

online:<userId> → 上线时间戳

ack:<messageId> → 确认状态



---

客户端开发

Electron 客户端：位于 client/，基于 Material Design

集成指南：

1. HTTP API 调用、保存 JWT


2. WebSocket 连接附带 JWT


3. 心跳与消息确认处理





---

性能优化

心跳机制保持连接

Redis 缓存减轻数据库负担

消息确认机制保证可靠性

Protocol Buffers 提高序列化效率



---

故障排除

常见问题

1. 无法连接：检查 IP/端口、


2. WebSocket 失败：验证 JWT、检查头格式


3. 消息发送失败：确认接收方在线、消息格式



日志

logs/error.log：错误日志

logs/access.log：访问日志



---

贡献指南

欢迎提交 PR 或 Issue：

1. 通过全部测试


2. 遵循代码风格


3. 更新文档


4. 添加注释


5. 编写单元测试（如适用）




---

版本历史


---

许可证

本项目使用 MIT License，详见 LICENSE。
