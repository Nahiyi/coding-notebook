# Spring Boot + Netty 集成示例

本项目展示如何在Spring Boot中集成和使用Netty框架，涵盖了Netty最常见的三种使用场景。

## 📚 目录

- [什么是Netty？](#什么是netty)
- [项目概述](#项目概述)
- [Netty vs Tomcat：本质区别](#netty-vs-tomcat本质区别)
- [Netty应用金字塔](#netty应用金字塔)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [使用场景详解](#使用场景详解)
- [测试指南](#测试指南)
- [配置说明](#配置说明)
- [核心概念](#核心概念)
- [常见问题](#常见问题)

## 什么是Netty？

**Netty** 是一个异步的、基于事件驱动的网络应用框架，用于快速开发可维护、高性能的网络服务器和客户端。

### 核心特性

- ⚡ **高性能**：比传统Java NIO更快的性能
- 🔄 **异步非阻塞**：基于Future和Callback机制
- 🎯 **事件驱动**：基于Reactor模式
- 🔧 **易于使用**：提供了丰富的编解码器
- 🌐 **协议支持**：HTTP、WebSocket、TCP、UDP等

### 应用场景

| 场景 | 说明 | 示例 |
|------|------|------|
| **Web应用** | 替代Tomcat提供HTTP服务 | Spring WebFlux、Spring Cloud Gateway |
| **实时通信** | 双向实时数据传输 | 聊天室、在线游戏、实时推送 |
| **RPC框架** | 远程过程调用的底层通信 | Dubbo、gRPC |
| **物联网** | 设备与服务器之间的通信 | MQTT、设备管理平台 |
| **消息推送** | 向客户端推送实时数据 | 股票行情、系统通知 |

## 项目概述

本模块实现了三种最常见的Netty服务器：

1. **TCP服务器** - 最基础的Socket通信
2. **WebSocket服务器** - 实时双向通信
3. **HTTP服务器** - 替代Tomcat的高性能HTTP服务

### 架构图

```
┌─────────────────────────────────────────────────────┐
│              Spring Boot Application                │
│  ┌───────────────────────────────────────────────┐  │
│  │           REST API Controller (9060)          │  │
│  │  - /netty/health                              │  │
│  │  - /netty/info                                │  │
│  │  - /netty/guide                               │  │
│  └───────────────────────────────────────────────┘  │
│                                                     │
│  ┌─────────┐  ┌────────────┐  ┌──────────────┐      │
│  │ TCP     │  │ WebSocket  │  │ HTTP         │      │
│  │ Server  │  │ Server     │  │ Server       │      │
│  │ :9001   │  │ :9002      │  │ :9003        │      │
│  └─────────┘  └────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────┘
```

## ⚡ Netty vs Tomcat：本质区别

### 核心区别

```
┌─────────────────────────────────────────────────────────────┐
│                        Tomcat                                │
├─────────────────────────────────────────────────────────────┤
│  • 专注HTTP/HTTPS协议                                        │
│  • Servlet容器标准实现                                       │
│  • 基于请求-响应模型（半双工）                               │
│  ❌ 无法直接使用TCP/UDP（必须通过HTTP）                       │
│  ❌ 无法实现WebSocket原生性能（需要额外配置）                 │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                        Netty                                 │
├─────────────────────────────────────────────────────────────┤
│  • 从TCP/UDP传输层开始（最底层）                             │
│  • 可以自定义任何协议                                        │
│  • 事件驱动模型（全双工）                                     │
│  ✅ 零协议开销，极致性能                                      │
│  ✅ 支持所有网络协议（HTTP/WebSocket/TCP/UDP）               │
└─────────────────────────────────────────────────────────────┘
```

### 性能对比：文件传输场景

**场景：传输100MB文件**

#### 使用Tomcat（HTTP协议）
```
客户端发送：
┌─────────────────────────────────┐
│ HTTP Header: 2KB                │  ← 协议开销
│ Content-Type: multipart/form... │
│ Boundary: ...                   │
├─────────────────────────────────┤
│ 文件内容: 100MB                   │
└─────────────────────────────────┘

传输总量：100MB + HTTP协议开销 ≈ 100.2MB
耗时：~5秒（包含HTTP编解码）
```

#### 使用Netty（纯TCP）
```
客户端发送：
┌─────────────────────────────────┐
│ 文件长度: 8字节                    │  ← 极小的自定义头
│ 文件内容: 100MB                   │
└─────────────────────────────────┘

传输总量：100MB + 8字节 ≈ 100.000008MB
耗时：~3秒（纯TCP，零编解码开销）
```

**性能提升：40%↑** 🚀

### 为什么Netty的HTTP比Tomcat快？

```
Tomcat:
├── Servlet容器（重量级）
├── 多层过滤器
├── 传统阻塞I/O
└── 线程池模型（每个请求一个线程）

Netty:
├── 轻量级事件驱动
├── 零拷贝技术
├── 异步非阻塞I/O
└── 少量线程处理大量连接（Reactor模式）
```

## 🎯 Netty应用金字塔

```
                    ↑
                    │
            ┌───────┴───────┐
            │               │
         应用层协议        传输层协议
         (基于HTTP)      (纯TCP/UDP)
            │                │
            │                │
        ┌───┴────┐      ┌────┴────┐
        │        │      │         │
       HTTP   WebSocket TCP      UDP
        │        │      │         │
        │        │      │         │
     WebFlux  即时通讯  RPC        游戏
     API网关   聊天室   Dubbo     物联网
```

### 三大应用方向

#### 1️⃣ TCP服务器 - 最底层、最灵活

```java
适用场景：
✅ 自定义协议（如Redis、Dubbo、MySQL协议）
✅ 高性能文件传输
✅ 游戏服务器（实时性要求极高）
✅ 物联网设备通信（MQTT等）
✅ RPC框架底层通信

优势：
• 零协议开销
• 可以自定义任何协议
• 性能极致
```

**实际生产案例**：
- **Dubbo**（RPC框架）- 基于TCP的自定义协议
- **Redis** - 基于TCP的RESP协议
- **MySQL** - 基于TCP的自定义二进制协议

#### 2️⃣ WebSocket服务器 - HTTP升级 + 全双工

```java
适用场景：
✅ 聊天室、即时通讯
✅ 股票行情推送
✅ 在线协作（多人编辑）
✅ 实时监控告警

优势：
• 建立连接后，无需HTTP头开销
• 服务器可以主动推送消息
• 比HTTP轮询高效100倍
```

**与HTTP的区别**：
```
HTTP（半双工）：
客户端 ──请求──→ 服务器
        ←─响应──

WebSocket（全双工）：
客户端 ←───双向实时─────→ 服务器
```

#### 3️⃣ HTTP服务器 - 替代Tomcat

```java
适用场景：
✅ 高性能REST API
✅ API网关（Spring Cloud Gateway）
✅ 微服务架构
✅ 反向代理

优势：
• 性能比Tomcat高30%-50%
• 内存占用更低
• 支持HTTP/2（Tomcat默认不支持）
• 更灵活的定制能力
```

### 生产环境架构演进

```
早期架构：
┌──────────┐
│  Tomcat  │  ← 处理HTTP请求
└──────────┘

问题：高并发性能瓶颈、无法支持自定义协议

现代架构（全Netty）：
┌──────────────────────────────────────┐
│  Spring Cloud Gateway (Netty HTTP)   │ ← 网关层
├──────────────────────────────────────┤
│  Dubbo (Netty TCP)                   │ ← RPC调用
├──────────────────────────────────────┤
│  RocketMQ (Netty)                    │ ← 消息队列
└──────────────────────────────────────┘

性能提升：10倍！
```

## 技术栈

- **Spring Boot** 2.7.18
- **Netty** (由Spring Boot管理版本)
- **Lombok** (简化代码)
- **Maven** (构建工具)

## 快速开始

### 前置条件

- JDK 1.8+
- Maven 3.x

### 启动步骤

1. **编译项目**
```bash
cd springboot-netty
mvn clean compile
```

2. **启动应用**
```bash
mvn spring-boot:run
```

3. **查看启动日志**
```
========================================
Spring Boot + Netty 应用启动成功！
服务端口: 9060
访问地址: http://localhost:9060
REST API: http://localhost:9060/netty
========================================

Netty服务说明：
- TCP服务器: localhost:9001 (Echo服务)
- WebSocket服务器: ws://localhost:9002/ws
- HTTP服务器: localhost:9003
========================================
```

## 使用场景详解

### 1️⃣ TCP服务器（端口：9001）

**功能**：实现基础的TCP Socket通信，提供Echo服务（将客户端发送的消息原样返回）

**适用场景**：

- 🔧 自定义协议通信
- 🌐 RPC框架底层通信
- 🎮 游戏服务器
- 📡 物联网设备通信

**核心代码位置**：

- `cn.clazs.netty.tcp.TcpServer` - 服务器启动类
- `cn.clazs.netty.tcp.TcpServerHandler` - 业务处理器

**技术要点**：
```java
// EventLoopGroup：处理I/O操作的事件循环组
EventLoopGroup bossGroup = new NioEventLoopGroup(1);    // 接收客户端连接
EventLoopGroup workerGroup = new NioEventLoopGroup();    // 处理I/O操作

// ServerBootstrap：服务器启动引导类
ServerBootstrap bootstrap = new ServerBootstrap();
bootstrap.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) {
                 ch.pipeline().addLast(new TcpServerHandler());
             }
         });
```

### 2️⃣ WebSocket服务器（端口：9002）

**功能**：实现WebSocket协议的实时双向通信

**适用场景**：
- 💬 聊天室、即时通讯
- 📊 实时数据推送（股票行情、游戏状态）
- 👥 在线协作（多人编辑）
- 🚨 实时监控告警

**核心代码位置**：
- `cn.clazs.netty.websocket.WebSocketServer` - 服务器启动类
- `cn.clazs.netty.websocket.WebSocketServerHandler` - 业务处理器

**Handler Pipeline说明**：
```java
pipeline.addLast(new HttpServerCodec());              // HTTP编解码器
pipeline.addLast(new HttpObjectAggregator(65536));    // HTTP消息聚合器
pipeline.addLast(new ChunkedWriteHandler());          // 支持大文件传输
pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));  // WebSocket协议处理器
pipeline.addLast(new WebSocketServerHandler());       // 自定义业务处理器
```

### 3️⃣ HTTP服务器（端口：9003）

**功能**：实现HTTP协议服务器，可替代Tomcat提供HTTP服务

**适用场景**：

- 🚀 高性能REST API
- 🌉 API网关
- 🔀 微服务网关（如Spring Cloud Gateway）
- ⚡ 反向代理

**核心代码位置**：
- `cn.clazs.netty.http.HttpServer` - 服务器启动类
- `cn.clazs.netty.http.HttpServerHandler` - 业务处理器

**优势对比Tomcat**：
- ✅ 更高的性能和吞吐量
- ✅ 更低的内存占用
- ✅ 更灵活的定制能力
- ✅ 支持HTTP/2、WebSocket等多种协议

## 测试指南

### REST API测试

**健康检查**
```bash
curl http://localhost:9060/netty/health
```

**获取服务信息**

```bash
curl http://localhost:9060/netty/info
```

**获取测试指南**
```bash
curl http://localhost:9060/netty/guide
```

### TCP服务器测试

**方法1：使用telnet**
```bash
telnet localhost 9001
# 输入任意文本，服务器会原样返回
```

**方法2：使用nc**

```bash
nc localhost 9001
# 输入任意文本，服务器会原样返回
```

**方法3：Java代码**
```java
Socket socket = new Socket("localhost", 9001);
OutputStream out = socket.getOutputStream();
out.write("Hello Netty!\n".getBytes());
out.flush();
```

### WebSocket服务器测试

**方法1：在线工具**
访问：http://www.websocket-test.com/
连接：`ws://localhost:9002/ws`

**方法2：浏览器JavaScript**
```javascript
const ws = new WebSocket('ws://localhost:9002/ws');

// 连接成功
ws.onopen = () => {
    console.log('已连接到服务器');
    ws.send('Hello Netty WebSocket!');
};

// 接收消息
ws.onmessage = (event) => {
    console.log('收到消息:', event.data);
};

// 连接关闭
ws.onclose = () => {
    console.log('连接已关闭');
};
```

**方法3：Postman**
1. 新建WebSocket请求
2. 连接到 `ws://localhost:9002/ws`
3. 发送消息

### HTTP服务器测试

**方法1：浏览器访问**

```
http://localhost:9003
```

**方法2：使用curl**
```bash
curl http://localhost:9003
```

**方法3：使用Postman**
GET请求：`http://localhost:9003`

## 配置说明

### application.yml配置

```yaml
server:
  port: 9060  # Spring Boot REST API端口

netty:
  # TCP服务器配置
  tcp:
    enabled: true   # 是否启用TCP服务器
    port: 9001      # TCP监听端口

  # WebSocket服务器配置
  websocket:
    enabled: true   # 是否启用WebSocket服务器
    port: 9002      # WebSocket监听端口
    path: /ws       # WebSocket访问路径

  # HTTP服务器配置
  http:
    enabled: true   # 是否启用HTTP服务器
    port: 9003      # HTTP监听端口
```

### 禁用某个服务器

如果不需要某个服务器，可以在配置文件中设置 `enabled: false`：

```yaml
netty:
  tcp:
    enabled: false  # 禁用TCP服务器
```

## 核心概念

### Netty的核心组件

1. **Channel**：网络连接的抽象，相当于一个Socket
2. **EventLoop**：事件循环，处理I/O操作
3. **EventLoopGroup**：事件循环组，管理多个EventLoop
4. **Bootstrap**：启动引导类，用于配置和启动服务器/客户端
5. **ChannelHandler**：业务处理器，处理入站和出站数据
6. **ChannelPipeline**：处理器管道，管理多个Handler

### Handler的工作流程

```
客户端请求 → ChannelPipeline → ChannelHandler[] → 业务处理
                                       ↓
                                  ChannelRead()
                                  channelReadComplete()
                                  exceptionCaught()
```

### EventLoopGroup模型

```
┌────────────────────────────────────┐
│         BossGroup (1个线程)         │  ← 接收客户端连接
│  ┌──────────────────────────────┐  │
│  │   NioEventLoop (Acceptor)    │  │
│  └──────────────────────────────┘  │
└────────────────────────────────────┘
              ↓ 注册
┌────────────────────────────────────┐
│     WorkerGroup (N个线程)          │  ← 处理I/O操作
│  ┌──────────┐ ┌──────────┐        │
│  │ EventLoop│ │ EventLoop│  ...   │
│  └──────────┘ └──────────┘        │
└────────────────────────────────────┘
```

## 常见问题

### Q1: Netty和Spring Boot是什么关系？

**A**: Netty和Spring Boot是互补关系：
- **Spring Boot** 是上层框架，负责快速搭建应用
- **Netty** 是底层网络通信框架，负责处理TCP/UDP网络通信
- Spring Boot可以通过依赖引入Netty，也可以使用基于Netty的组件（如WebFlux）

### Q2: 什么时候需要学习Netty？

**A**:
- ✅ **必须掌握**：高并发、实时通信、自定义协议、RPC服务、微服务网关
- ❌ **不需要**：普通CRUD接口、简单的Web应用

### Q3: Spring Boot默认使用Netty吗？

**A**:
- ❌ 默认使用 **Tomcat** 作为嵌入式容器
- ✅ 使用 `spring-boot-starter-webflux` 时，默认使用 **Netty**
- ✅ 可以手动引入 `netty-all` 依赖自己实现Netty服务

### Q4: 如何选择Tomcat还是Netty？

**A**:
| 对比项 | Tomcat | Netty |
|--------|--------|-------|
| 性能 | 中等 | 高 |
| 适用场景 | Servlet应用 | 异步非阻塞应用 |
| 学习曲线 | 低 | 中 |
| 协议支持 | HTTP/HTTPS | HTTP/WebSocket/TCP/UDP等 |
| 生态系统 | 成熟 | 快速发展中 |

**推荐**：
- 普通Web应用 → Tomcat
- 高性能API、实时通信 → Netty

### Q5: 本示例中的端口如何规划？

**A**:
- `9060` - Spring Boot REST API（管理接口）
- `9001` - Netty TCP服务器
- `9002` - Netty WebSocket服务器
- `9003` - Netty HTTP服务器

### Q6: Tomcat只能处理HTTP，Netty可以从TCP开始是什么意思？

**A**: 这是核心区别！

```
Tomcat的工作方式：
应用程序
    ↓
Servlet/JSP
    ↓
HTTP协议 ← 只能在这里！
    ↓
TCP/IP（操作系统）

Netty的工作方式：
应用程序
    ↓
可以直接选择：
    ├─ TCP协议 ← 最底层，零开销
    ├─ UDP协议
    ├─ HTTP协议（基于TCP）
    ├─ WebSocket（基于HTTP升级）
    └─ 自定义协议（如Redis、Dubbo）
    ↓
TCP/IP（操作系统）
```

**举例说明**：

**Tomcat文件上传（必须用HTTP）**：
```java
// 客户端必须用HTTP multipart/form-data格式
POST /upload HTTP/1.1
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary
------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="test.jpg"
Content-Type: image/jpeg
[文件二进制数据]
------WebKitFormBoundary--
```

**Netty文件上传（纯TCP）**：
```java
// 客户端直接发TCP字节流
[文件长度: 4字节]
[文件名长度: 2字节]
[文件名: 变长]
[文件数据: 直接二进制]
// 零HTTP开销！性能提升40%+
```

**本质区别**：
- Tomcat = **应用层框架**（只能HTTP）
- Netty = **传输层框架**（可以从TCP/UDP开始，也可以做HTTP）

这就是为什么：
- Spring Cloud Gateway用Netty（高性能HTTP网关）
- Dubbo用Netty TCP（高性能RPC）
- RocketMQ用Netty（高性能消息队列）
- Redis用Netty（高性能缓存）

## 项目结构

```
springboot-netty/
├── src/main/java/cn/clazs/netty/
│   ├── NettyApplication.java           # 主启动类
│   ├── tcp/                            # TCP服务器
│   │   ├── TcpServer.java              # TCP服务器启动类
│   │   └── TcpServerHandler.java       # TCP业务处理器
│   ├── websocket/                      # WebSocket服务器
│   │   ├── WebSocketServer.java        # WebSocket服务器启动类
│   │   └── WebSocketServerHandler.java # WebSocket业务处理器
│   ├── http/                           # HTTP服务器
│   │   ├── HttpServer.java             # HTTP服务器启动类
│   │   └── HttpServerHandler.java      # HTTP业务处理器
│   └── controller/                     # REST控制器
│       └── NettyController.java        # Netty服务管理API
├── src/main/resources/
│   └── application.yml                 # 配置文件
└── pom.xml                             # Maven配置
```

## 学习资源

- [Netty官方文档](https://netty.io/wiki/user-guide.html)
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Reactor Netty文档](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-reactor-netty)

## 作者

clazs

## 许可证

本项目仅用于学习目的。
