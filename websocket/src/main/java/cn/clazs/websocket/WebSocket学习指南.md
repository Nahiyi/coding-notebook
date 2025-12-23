# WebSocket 学习指南

## 📚 目录

1. [WebSocket基础概念](#1-websocket基础概念)
2. [两种实现方式对比](#2-两种实现方式对比)
3. [Spring Boot WebSocket实现](#3-spring-boot-websocket实现)
4. [核心组件详解](#4-核心组件详解)
5. [前端实现](#5-前端实现)
6. [服务端主动推送](#6-服务端主动推送)
7. [完整项目结构](#7-完整项目结构)
8. [常见问题与解决方案](#8-常见问题与解决方案)
9. [WebSocket定向推送（点对点消息）](#9-websocket定向推送点对点消息)

---

## 1. WebSocket基础概念

### 1.1 什么是WebSocket？

WebSocket是一种**全双工通信协议**，允许服务端主动向客户端推送消息。

**特点**：

- ✅ **持久连接**：建立一次连接，持续通信
- ✅ **双向通信**：客户端和服务端可以互相发送消息
- ✅ **实时性强**：无需轮询，延迟低
- ✅ **节省资源**：相比HTTP轮询，减少服务器压力

**对比HTTP**：

| 特性 | HTTP | WebSocket |
|------|------|-----------|
| **连接方式** | 短连接/请求-响应 | 长连接 |
| **通信方向** | 单向（客户端请求） | 双向 |
| **实时性** | 差（需要轮询） | 好（服务端可推送） |
| **开销** | 每次请求都有HTTP头 | 建立连接后开销小 |
| **状态** | 无状态 | 有状态 |

### 1.2 WebSocket应用场景

- 💬 **即时通讯**：聊天室、在线客服
- 📊 **实时数据**：股票行情、游戏状态
- 🔔 **消息推送**：系统通知、订单提醒
- 🎮 **多人游戏**：实时同步
- 📈 **监控告警**：实时数据展示

---

## 2. 两种实现方式对比

### 2.1 方式一：@ServerEndpoint注解（JSR-356标准）

**特点**：
- 基于Java标准API（JSR-356）
- 使用注解驱动
- 类似Servlet模式

**代码示例**：

```java
@Configuration
public class WebSocketConfiguration {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}

@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {
    private static Map<String, Session> sessionMap = new HashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        sessionMap.put(sid, session);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        // 处理消息
    }

    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        sessionMap.remove(sid);
    }
}
```

**优点**：
- ✅ 标准化，跨框架通用
- ✅ 代码简洁，注解清晰
- ✅ 适合简单的WebSocket场景

**缺点**：

- ❌ 与Spring集成稍弱
- ❌ 依赖注入需要手动处理（通常需要静态工具类）
- ❌ 拦截器等高级功能配置复杂

### 2.2 方式二：WebSocketHandler接口（Spring原生）⭐推荐

**特点**：
- Spring框架原生支持
- 完美融入Spring生态
- 更灵活的配置

**代码示例**：

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private WebSocketHandler webSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws")
                .setAllowedOrigins("*");
    }
}

@Component
public class WebSocketHandler implements org.springframework.web.socket.WebSocketHandler {
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        // 处理消息
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        sessions.remove(session);
    }
}
```

**优点**：
- ✅ **完美集成Spring**（依赖注入、AOP等）
- ✅ 更灵活的配置（拦截器、CORS等）
- ✅ 更好的异常处理
- ✅ 支持更复杂的场景（STOMP、SockJS等）
- ✅ 类型安全，编译期检查

**缺点**：

- ❌ 代码稍多
- ❌ Spring特定实现（非标准）

### 2.3 详细对比表

| 特性 | @ServerEndpoint | WebSocketHandler |
|------|----------------|------------------|
| **标准** | JSR-356 Java标准 | Spring专用 |
| **配置方式** | 注解驱动 | 配置类+Handler |
| **Spring集成** | 较弱（需要静态类） | **完美集成** |
| **依赖注入** | 需要手动处理 | 自动注入@Autowired |
| **拦截器** | 难以实现 | 内置支持 |
| **路径参数** | `@PathParam` | 手动解析 |
| **适用场景** | 简单聊天、基础功能 | 复杂业务、企业级应用 |
| **推荐度** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

### 2.4 最佳实践

**选择建议**：

1. **使用 @ServerEndpoint 当**：
   - 项目简单，只是基础WebSocket通信
   - 需要跨框架兼容
   - 快速原型开发
   - 非Spring Boot项目

2. **使用 WebSocketHandler 当**：
   - ✅ **Spring Boot项目**（强烈推荐）
   - 需要Spring的依赖注入
   - 需要拦截器、权限控制
   - 企业级应用
   - 需要更好的扩展性

**本项目选择**：WebSocketHandler，因为是Spring Boot项目，需要更好的架构设计和扩展性。

---

## 3. Spring Boot WebSocket实现

### 3.1 Maven依赖

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<dependencies>
    <!-- WebSocket核心依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <!-- Web依赖（提供REST API支持） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### 3.2 配置类（WebSocketConfig）

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册WebSocket处理器
        registry.addHandler(webSocketHandler, "/ws")
                .setAllowedOrigins("*");  // 允许跨域
    }
}
```

**关键点**：

- `@EnableWebSocket`：启用WebSocket功能
- `implements WebSocketConfigurer`：实现配置接口
- `addHandler()`：注册处理器和端点路径
- `setAllowedOrigins("*")`：允许跨域（生产环境需限制）

### 3.3 处理器类（WebSocketHandler）

```java
@Component
public class WebSocketHandler implements org.springframework.web.socket.WebSocketHandler {

    // 存储所有连接的会话（线程安全）
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("连接建立: " + session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        String payload = message.getPayload().toString();
        System.out.println("收到消息: " + payload);

        // 广播给所有客户端
        broadcastMessage("服务器回复: " + payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        sessions.remove(session);
        System.out.println("连接关闭: " + session.getId());
    }

    public void broadcastMessage(String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }
}
```

**关键点**：
- `CopyOnWriteArraySet`：线程安全的集合，适合并发场景
- `WebSocketSession`：表示一个WebSocket连接会话
- `TextMessage`：文本消息

---

## 4. 核心组件详解

### 4.1 WebSocketConfig（配置类）

**作用**：配置WebSocket端点和处理器

**核心方法**：
- `registerWebSocketHandlers()`：注册WebSocket处理器

**配置选项**：
```java
registry.addHandler(handler, "/ws")
    .setAllowedOrigins("*")        // 允许跨域
    .withSockJS();                  // 启用SockJS（降级方案）
```

### 4.2 WebSocketHandler（处理器）

**作用**：处理WebSocket连接生命周期和消息

**核心方法**：

| 方法 | 触发时机 | 用途 |
|------|---------|------|
| `afterConnectionEstablished()` | 连接建立时 | 初始化会话、发送欢迎消息 |
| `handleMessage()` | 收到消息时 | 处理客户端消息 |
| `handleTransportError()` | 发生错误时 | 错误处理、清理资源 |
| `afterConnectionClosed()` | 连接关闭时 | 清理会话、更新状态 |
| `supportsPartialMessages()` | 查询是否支持部分消息 | 通常返回false |

### 4.3 WebSocketSession（会话对象）

**作用**：代表一个WebSocket连接

**常用方法**：
```java
session.getId()                    // 获取会话ID
session.isOpen()                   // 检查是否打开
session.sendMessage(message)       // 发送消息
session.close()                    // 关闭连接
```

### 4.4 消息类型

**TextMessage**（文本消息）：

```java
TextMessage message = new TextMessage("Hello");
session.sendMessage(message);
```

**BinaryMessage**（二进制消息）：

```java
byte[] data = {0x01, 0x02, 0x03};
BinaryMessage message = new BinaryMessage(data);
session.sendMessage(message);
```

---

## 5. 前端实现

### 5.1 WebSocket API

**创建连接**：
```javascript
const ws = new WebSocket('ws://localhost:9058/ws');
```

**事件监听**：
```javascript
// 连接打开
ws.onopen = function(event) {
    console.log('连接已建立');
};

// 收到消息
ws.onmessage = function(event) {
    console.log('收到消息: ' + event.data);
};

// 连接关闭
ws.onclose = function(event) {
    console.log('连接已关闭');
};

// 发生错误
ws.onerror = function(error) {
    console.error('WebSocket错误: ' + error);
};
```

**发送消息**：
```javascript
ws.send('Hello Server!');
```

**关闭连接**：
```javascript
ws.close();
```

### 5.2 连接状态

WebSocket有4种状态：

| 常量 | 值 | 说明 |
|------|---|------|
| `WebSocket.CONNECTING` | 0 | 正在连接 |
| `WebSocket.OPEN` | 1 | 已连接 |
| `WebSocket.CLOSING` | 2 | 正在关闭 |
| `WebSocket.CLOSED` | 3 | 已关闭 |

**检查状态**：

```javascript
if (ws.readyState === WebSocket.OPEN) {
    ws.send('Message');
}
```

---

## 6. 服务端主动推送

### 6.1 场景

服务端需要主动向客户端推送消息，例如：
- 系统通知
- 订单状态更新
- 实时数据推送

### 6.2 实现方式

**方式一：直接在WebSocketHandler中广播**

```java
@Component
public class WebSocketHandler {
    public void broadcastMessage(String message) {
        for (WebSocketSession session : sessions) {
            session.sendMessage(new TextMessage(message));
        }
    }
}
```

**方式二：通过REST API触发** ⭐推荐

```java
@RestController
@RequestMapping("/api/ws")
public class WebSocketController {

    @Autowired
    private WebSocketHandler webSocketHandler;

    @PostMapping("/broadcast")
    public Map<String, Object> broadcast(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        webSocketHandler.broadcastMessage(message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "广播成功");
        return response;
    }
}
```

**前端调用**：
```javascript
// 管理后台推送消息
async function broadcastMessage() {
    const response = await fetch('http://localhost:9058/api/ws/broadcast', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: 'Hello All!' })
    });

    const data = await response.json();
    console.log(data);
}
```

### 6.3 使用场景示例

**系统通知**：
```java
@PostMapping("/notify")
public void sendNotification(@RequestBody NotificationDTO notification) {
    String message = "【系统通知】" + notification.getTitle() +
                     " - " + notification.getContent();
    webSocketHandler.broadcastMessage(message);
}
```

**订单更新**：
```java
public void onOrderStatusChanged(Order order) {
    String message = String.format("订单%s状态更新为：%s",
                                   order.getId(),
                                   order.getStatus());
    webSocketHandler.broadcastMessage(message);
}
```

---

## 7. 完整项目结构

```
websocket/
├── pom.xml                                          # Maven配置
├── src/main/
│   ├── java/cn/clazs/websocket/
│   │   ├── WebSocketApplication.java               # 启动类
│   │   ├── config/
│   │   │   └── WebSocketConfig.java                # WebSocket配置
│   │   ├── handler/
│   │   │   └── WebSocketHandler.java               # 消息处理器
│   │   └── controller/
│   │       └── WebSocketController.java            # REST控制器
│   └── resources/
│       ├── application.yml                          # 应用配置
│       └── static/
│           ├── index.html                           # 客户端页面
│           └── admin.html                           # 管理后台
```

**核心文件说明**：

1. **WebSocketApplication.java**
   - Spring Boot启动类
   - 自动扫描并注册所有Bean

2. **WebSocketConfig.java**
   - WebSocket配置类
   - 注册端点和处理器

3. **WebSocketHandler.java**
   - 核心业务逻辑
   - 管理连接、处理消息

4. **WebSocketController.java**
   - REST API控制器
   - 提供服务端推送接口

5. **index.html**
   - 客户端页面
   - WebSocket连接和消息收发

6. **admin.html**
   - 管理后台
   - 服务端主动推送消息

---

## 8. 常见问题与解决方案

### 8.1 跨域问题

**问题**：
```
Access to WebSocket at 'ws://localhost:9058/ws' from origin 'http://localhost:3000' has been blocked by CORS policy
```

**解决方案**：
```java
registry.addHandler(handler, "/ws")
        .setAllowedOrigins("*");  // 开发环境

// 生产环境指定具体域名
.setAllowedOrigins("http://your-domain.com");
```

### 8.2 连接断开问题

**问题**：WebSocket连接频繁断开

**可能原因**：

1. 网络不稳定
2. 服务端重启
3. 超时未通信

**解决方案**：
```javascript
// 心跳检测
setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send('ping');
    }
}, 30000); // 每30秒发送心跳

// 自动重连
ws.onclose = function() {
    setTimeout(() => {
        reconnect();
    }, 3000);
};

function reconnect() {
    ws = new WebSocket('ws://localhost:9058/ws');
}
```

### 8.3 消息乱码问题

**问题**：中文消息显示乱码

**解决方案**：
```java
// 服务端指定编码
server:
  port: 9058
  servlet:
    encoding:
      charset: UTF-8
      force: true
```

```javascript
// 前端确保使用UTF-8
<meta charset="UTF-8">
```

### 8.4 线程安全问题

**问题**：多线程并发操作sessions集合

**解决方案**：
```java
// 使用线程安全的集合
private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

// 或使用ConcurrentHashMap
private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
```

### 8.5 消息太大问题

**问题**：大消息发送失败

**解决方案**：
```java
// 配置消息大小限制
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
                .setAllowedOrigins("*")
                .setMaxTextMessageBufferSize(8192)      // 8KB
                .setMaxBinaryMessageBufferSize(8192);    // 8KB
    }
}
```

---

## 9. 最佳实践总结

### 9.1 会话管理

```java
// ✅ 推荐：使用线程安全的集合
private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

// ❌ 不推荐：使用ArrayList
private static final List<WebSocketSession> sessions = new ArrayList<>();
```

### 9.2 资源清理

```java
@Override
public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
    sessions.remove(session);  // 一定要清理会话
    // 释放其他资源
}
```

### 9.3 异常处理

```java
@Override
public void handleTransportError(WebSocketSession session, Throwable exception) {
    System.err.println("传输错误: " + exception.getMessage());
    if (session.isOpen()) {
        try {
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    sessions.remove(session);
}
```

### 9.4 消息验证

```java
@Override
public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
    String payload = message.getPayload().toString();

    // 验证消息
    if (payload == null || payload.trim().isEmpty()) {
        return;
    }

    if (payload.length() > 1000) {
        sendError(session, "消息太长");
        return;
    }

    // 处理消息...
}
```

### 9.5 心跳检测

```java
// 服务端
@Override
public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
    String payload = message.getPayload().toString();
    if ("ping".equals(payload)) {
        try {
            session.sendMessage(new TextMessage("pong"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }
    // 处理其他消息...
}
```

```javascript
// 客户端
setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send('ping');
    }
}, 30000);
```

---

## 9. WebSocket定向推送（点对点消息）⭐

### 9.1 什么是定向推送？

**定向推送（Unicast）**：服务端向**特定的一个客户端**发送消息，与广播（Broadcast）向所有客户端发送消息不同。

**应用场景**：
- 💬 **私聊消息**：用户A发送给用户B的私信
- 📬 **个人通知**：订单状态更新、@提醒
- 🎯 **特定操作反馈**：服务端向特定用户返回操作结果
- 📊 **个性化数据**：向用户推送其专属的数据

### 9.2 SessionId与UserId的区别

在实现定向推送之前，需要先理解两个重要概念：

| 概念 | 说明 | 示例 | 用途 |
|------|------|------|------|
| **SessionId** | WebSocket连接的唯一ID，由Spring自动生成 | `abc123xyz` | 标识一次WebSocket连接 |
| **UserId** | 业务系统的用户ID，如数据库主键 | `user_10001` | 标识具体的业务用户 |

**关键点**：
- `session.getId()` 返回的是**SessionId**（如`abc123xyz`）
- 定向推送需要根据**UserId**找到对应的**Session**
- 一个用户可能有多个连接（多设备登录），需要管理UserId与Session的映射关系

### 9.3 定向推送的实现方案

#### 方案一：使用ConcurrentHashMap存储UserId与Session的映射 ⭐推荐

**核心思路**：使用`ConcurrentHashMap<String, WebSocketSession>`，key为UserId，value为Session。

**代码实现**：

```java
@Component
public class WebSocketHandler implements org.springframework.web.socket.WebSocketHandler {

    // 存储所有连接的Session（使用SessionId作为key，便于快速删除）
    private static final CopyOnWriteArraySet<WebSocketSession> sessionSet = new CopyOnWriteArraySet<>();

    // ✅ 存储UserId与Session的映射关系（用于定向推送）
    private static final ConcurrentHashMap<String, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();

    /**
     * 连接建立时，建立UserId与Session的映射
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionSet.add(session);

        // ✅ 从Session中获取UserId（需要在握手时传递）
        String userId = getUserIdFromSession(session);
        if (userId != null && !userId.isEmpty()) {
            userSessionMap.put(userId, session);
            System.out.println("用户 [" + userId + "] 连接建立，SessionId: " + session.getId());
        }

        // 发送欢迎消息
        session.sendMessage(new TextMessage("欢迎连接到WebSocket服务器！你的ID: " + session.getId()));
    }

    /**
     * 连接关闭时，清理映射关系
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        sessionSet.remove(session);

        // ✅ 清理UserId与Session的映射
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessionMap.remove(userId);
            System.out.println("用户 [" + userId + "] 连接关闭");
        }

        System.out.println("剩余连接数: " + sessionSet.size());
    }

    /**
     * ✅ 定向推送给指定用户
     */
    public void sendMessageToUser(String userId, String message) {
        WebSocketSession session = userSessionMap.get(userId);

        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                System.out.println("发送消息给用户 [" + userId + "]: " + message);
            } catch (IOException e) {
                System.err.println("发送消息失败: " + e.getMessage());
            }
        } else {
            System.err.println("用户 [" + userId + "] 不在线或Session已关闭");
        }
    }

    /**
     * ✅ 批量发送给多个用户
     */
    public void sendMessageToUsers(List<String> userIds, String message) {
        for (String userId : userIds) {
            sendMessageToUser(userId, message);
        }
    }

    /**
     * 从Session中获取UserId
     */
    private String getUserIdFromSession(WebSocketSession session) {
        // 方式1：从URI路径参数中获取（推荐）
        // 例如：ws://localhost:9058/ws?userId=user_10001
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            return query.split("userId=")[1].split("&")[0];
        }

        // 方式2：从WebSocket握手属性中获取（需要在拦截器中设置）
        // Object userId = session.getAttributes().get("userId");
        // if (userId != null) {
        //     return userId.toString();
        // }

        // 方式3：从Session的Principal中获取（需要认证）
        // Principal principal = session.getPrincipal();
        // if (principal != null) {
        //     return principal.getName();
        // }

        return session.getId();  // 默认返回SessionId
    }
}
```

**REST API（触发定向推送）**：

```java
@RestController
@RequestMapping("/api/ws")
public class WebSocketController {

    @Autowired
    private WebSocketHandler webSocketHandler;

    /**
     * 定向推送消息给指定用户
     */
    @PostMapping("/send")
    public Map<String, Object> sendToUser(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String message = request.get("message");

        webSocketHandler.sendMessageToUser(userId, message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "消息已发送给用户: " + userId);
        return response;
    }

    /**
     * 批量推送消息给多个用户
     */
    @PostMapping("/send-batch")
    public Map<String, Object> sendToUsers(@RequestBody Map<String, Object> request) {
        List<String> userIds = (List<String>) request.get("userIds");
        String message = request.get("message").toString();

        webSocketHandler.sendMessageToUsers(userIds, message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "消息已发送给 " + userIds.size() + " 个用户");
        return response;
    }
}
```

**前端调用**：

```javascript
// 定向推送
async function sendToUser() {
    const response = await fetch('http://localhost:9058/api/ws/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            userId: 'user_10001',
            message: '你好，这是一条定向消息！'
        })
    });

    const data = await response.json();
    console.log(data);
}

// 批量推送
async function sendToUsers() {
    const response = await fetch('http://localhost:9058/api/ws/send-batch', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            userIds: ['user_10001', 'user_10002', 'user_10003'],
            message: '大家好，这是一条批量消息！'
        })
    });

    const data = await response.json();
    console.log(data);
}
```

---

#### 方案二：使用拦截器在握手时传递UserId

**优点**：更安全、更优雅，支持认证授权

**实现步骤**：

**1. 定义握手拦截器**

```java
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

            // 方式1：从URL参数中获取userId
            String userId = servletRequest.getServletRequest().getParameter("userId");

            // 方式2：从HTTP Header中获取userId（推荐）
            // String userId = servletRequest.getServletRequest().getHeader("X-User-Id");

            // 方式3：从Token中解析userId（需要JWT认证）
            // String token = servletRequest.getServletRequest().getHeader("Authorization");
            // String userId = JwtUtil.parseToken(token);

            if (userId != null && !userId.isEmpty()) {
                // ✅ 将userId存入WebSocket Session属性中
                attributes.put("userId", userId);
                System.out.println("用户 [" + userId + "] 握手成功");
                return true;
            }
        }

        System.err.println("握手失败：缺少userId参数");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            System.err.println("握手发生异常: " + exception.getMessage());
        }
    }
}
```

**2. 配置拦截器**

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Autowired
    private WebSocketHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws")
                .addInterceptors(handshakeInterceptor)  // ✅ 添加拦截器
                .setAllowedOrigins("*");
    }
}
```

**3. 修改WebSocketHandler**

```java
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    sessionSet.add(session);

    // ✅ 从Session属性中获取userId（由拦截器设置）
    String userId = (String) session.getAttributes().get("userId");

    if (userId != null && !userId.isEmpty()) {
        userSessionMap.put(userId, session);
        System.out.println("用户 [" + userId + "] 连接建立，SessionId: " + session.getId());
    }

    session.sendMessage(new TextMessage("欢迎连接到WebSocket服务器！"));
}
```

**前端连接时传递userId**：

```javascript
// 方式1：URL参数
const ws = new WebSocket('ws://localhost:9058/ws?userId=user_10001');

// 方式2：通过Header传递（需要原生WebSocket API，不支持）
// const ws = new WebSocket('ws://localhost:9058/ws', ['user_10001']);
```

---

#### 方案三：多设备登录的处理（一个UserId对应多个Session）

**场景**：一个用户同时在手机、电脑、平板登录，需要向所有设备推送消息。

**实现代码**：

```java
@Component
public class WebSocketHandler implements org.springframework.web.socket.WebSocketHandler {

    // ✅ 一个UserId对应多个Session（支持多设备）
    private static final ConcurrentHashMap<String, Set<WebSocketSession>> userSessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);

        if (userId != null) {
            // ✅ 将Session添加到用户的Session集合中
            userSessionMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
            System.out.println("用户 [" + userId + "] 连接建立，当前设备数: " +
                userSessionMap.get(userId).size());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String userId = getUserIdFromSession(session);

        if (userId != null) {
            Set<WebSocketSession> sessions = userSessionMap.get(userId);
            if (sessions != null) {
                sessions.remove(session);

                // 如果用户的所有连接都关闭了，移除映射
                if (sessions.isEmpty()) {
                    userSessionMap.remove(userId);
                }
            }
            System.out.println("用户 [" + userId + "] 连接关闭，剩余设备数: " +
                (userSessionMap.containsKey(userId) ? userSessionMap.get(userId).size() : 0));
        }
    }

    /**
     * ✅ 向用户的所有设备发送消息
     */
    public void sendMessageToUser(String userId, String message) {
        Set<WebSocketSession> sessions = userSessionMap.get(userId);

        if (sessions != null && !sessions.isEmpty()) {
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                        System.out.println("发送消息给用户 [" + userId + "] 设备 [" + session.getId() + "]");
                    }
                } catch (IOException e) {
                    System.err.println("发送消息失败: " + e.getMessage());
                }
            }
        } else {
            System.err.println("用户 [" + userId + "] 不在线");
        }
    }
}
```

---

### 9.4 完整示例：私聊功能实现

**场景**：用户A向用户B发送私聊消息。

**前端代码（发送私聊消息）**：

```javascript
// 客户端A发送私聊消息
ws.onopen = function() {
    // 发送私聊消息（JSON格式）
    const privateMessage = {
        type: 'private',       // 消息类型：私聊
        from: 'user_10001',    // 发送者
        to: 'user_10002',      // 接收者
        content: '你好，这是一条私聊消息！'
    };

    ws.send(JSON.stringify(privateMessage));
};
```

**WebSocketHandler处理私聊消息**：

```java
@Override
public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
    String payload = message.getPayload().toString();

    try {
        // 解析JSON消息
        JSONObject json = JSON.parseObject(payload);
        String type = json.getString("type");
        String from = json.getString("from");
        String to = json.getString("to");
        String content = json.getString("content");

        if ("private".equals(type)) {
            // ✅ 私聊消息：只发送给接收者
            String messageToSend = "[" + from + "] 说: " + content;
            sendMessageToUser(to, messageToSend);

            // 可选：给发送者回执
            session.sendMessage(new TextMessage("消息已发送给 " + to));
        } else {
            // 其他消息类型...
        }
    } catch (Exception e) {
        System.err.println("消息格式错误: " + e.getMessage());
    }
}
```

---

### 9.5 定向推送与广播的对比

| 特性 | 定向推送（Unicast） | 广播（Broadcast） |
|------|-------------------|-----------------|
| **接收者** | 单个用户 | 所有在线用户 |
| **使用场景** | 私聊、个人通知 | 系统公告、群聊 |
| **实现方法** | `sendMessageToUser(userId, message)` | `broadcastMessage(message)` |
| **性能消耗** | 低（只发送一次） | 高（遍历所有Session） |
| **数据结构** | `ConcurrentHashMap<UserId, Session>` | `CopyOnWriteArraySet<Session>` |

---

### 9.6 最佳实践总结

**1. Session管理**
```java
// ✅ 推荐：使用ConcurrentHashMap存储UserId与Session映射
private static final ConcurrentHashMap<String, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();

// ❌ 不推荐：使用CopyOnWriteArraySet，无法快速查找
private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
```

**2. UserId传递方式**
- **URL参数**：`ws://localhost:9058/ws?userId=user_10001`（简单、方便测试）
- **HTTP Header**：`X-User-Id: user_10001`（推荐、更安全）
- **JWT Token**：`Authorization: Bearer <token>`（最安全、适合生产）

**3. 多设备处理**
- 使用`Map<UserId, Set<Session>>`存储一个用户的多个连接
- 向用户的所有设备推送消息

**4. 异常处理**
```java
public void sendMessageToUser(String userId, String message) {
    WebSocketSession session = userSessionMap.get(userId);

    if (session == null) {
        System.err.println("用户 [" + userId + "] 不在线");
        return;
    }

    if (!session.isOpen()) {
        System.err.println("Session已关闭，清理映射");
        userSessionMap.remove(userId);
        return;
    }

    try {
        session.sendMessage(new TextMessage(message));
    } catch (IOException e) {
        System.err.println("发送失败: " + e.getMessage());
    }
}
```

---

## 10. 扩展知识

### 10.1 STOMP协议

STOMP（Simple Text Oriented Messaging Protocol）是一种简单的文本定向消息协议。

**优点**：
- 支持发布-订阅模式
- 支持消息确认
- 支持事务

**Spring Boot集成**：
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
}
```

### 10.2 SockJS降级

SockJS是一个WebSocket降级方案，当浏览器不支持WebSocket时，会自动降级为轮询。

**配置**：
```java
@Override
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler, "/ws")
            .setAllowedOrigins("*")
            .withSockJS();  // 启用SockJS
}
```

**前端使用**：
```html
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script>
    var ws = new SockJS('http://localhost:9058/ws');
</script>
```

---

## 11. 大规模IM场景架构演进 ⭐

### 11.1 当前方案的局限性分析

**当前方案：单机内存存储Session**

```java
// 当前实现：所有Session存储在JVM堆内存中
private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
```

**存在的问题**：

| 问题 | 说明 | 影响 |
|------|------|------|
| **内存压力** | 10万在线用户 × 每个Session约1KB = 100MB+ 内存 | JVM堆内存压力大 |
| **单机瓶颈** | 单机最大连接数受限（约1-5万） | 无法水平扩展 |
| **消息广播效率低** | 遍历所有Session逐个发送，O(n)复杂度 | 大量用户时耗时长 |
| **单点故障** | 一台服务器宕机，所有连接丢失 | 高可用性差 |
| **写性能差** | CopyOnWriteArraySet每次修改都会复制整个数组 | 并发写入性能差 |

**适用场景**：
- ✅ 系统消息通知（低频、小规模）
- ✅ 小型客服系统（< 1000在线）
- ✅ 学习、演示项目
- ❌ 中大型IM应用（> 1万在线）
- ❌ 高并发实时聊天系统

---

### 11.2 方案一：应用层优化（中小规模，< 10万在线）

**核心思路**：通过优化代码结构和算法，提升单机性能，无需引入额外中间件。

#### 1. Session存储优化

```java
@Component
public class WebSocketHandler implements org.springframework.web.socket.WebSocketHandler {

    // ❌ 不推荐：CopyOnWriteArraySet（写性能差）
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    // ✅ 推荐：ConcurrentHashMap（读写性能都好）
    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // ✅ 推荐：分片存储（降低锁竞争）
    private static final ConcurrentHashMap<String, WebSocketSession>[] shards;
    private static final int SHARD_NUM = 16;

    static {
        shards = new ConcurrentHashMap[SHARD_NUM];
        for (int i = 0; i < SHARD_NUM; i++) {
            shards[i] = new ConcurrentHashMap<>();
        }
    }

    /**
     * 根据用户ID计算分片索引
     */
    private int getShardIndex(String userId) {
        int hash = userId.hashCode();
        return Math.abs(hash % SHARD_NUM);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserIdFromSession(session);
        int shardIndex = getShardIndex(userId);
        shards[shardIndex].put(userId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        String userId = getUserIdFromSession(session);
        int shardIndex = getShardIndex(userId);
        shards[shardIndex].remove(userId);
    }
}
```

#### 2. 异步消息发送

```java
@Component
public class WebSocketHandler implements org.springframework.web.socket.WebSocketHandler {

    @Autowired
    private ThreadPoolTaskExecutor asyncTaskExecutor;  // 异步线程池

    /**
     * 同步发送（阻塞当前线程）
     */
    public void broadcastSync(String message) {
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));  // 阻塞IO
                }
            } catch (IOException e) {
                log.error("发送消息失败", e);
            }
        }
    }

    /**
     * 异步发送（不阻塞当前线程）⭐推荐
     */
    public void broadcastAsync(String message) {
        asyncTaskExecutor.execute(() -> {
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                    }
                } catch (IOException e) {
                    log.error("发送消息失败", e);
                }
            }
        });
    }
}
```

**配置异步线程池**：

```java
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Bean("asyncTaskExecutor")
    public ThreadPoolTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);                    // 核心线程数
        executor.setMaxPoolSize(50);                     // 最大线程数
        executor.setQueueCapacity(1000);                 // 队列容量
        executor.setKeepAliveSeconds(60);                // 空闲线程存活时间
        executor.setThreadNamePrefix("ws-async-");       // 线程名称前缀
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());  // 拒绝策略
        executor.initialize();
        return executor;
    }
}
```

#### 3. 批量发送优化

```java
/**
 * 批量发送（减少IO次数）
 */
public void broadcastBatch(String message, int batchSize) {
    List<WebSocketSession> batch = new ArrayList<>(batchSize);

    for (WebSocketSession session : sessions) {
        if (session.isOpen()) {
            batch.add(session);

            if (batch.size() >= batchSize) {
                sendBatch(batch, message);
                batch.clear();
            }
        }
    }

    // 发送剩余的
    if (!batch.isEmpty()) {
        sendBatch(batch, message);
    }
}

/**
 * 发送一批Session
 */
private void sendBatch(List<WebSocketSession> batch, String message) {
    CompletableFuture[] futures = batch.stream()
        .map(session -> CompletableFuture.runAsync(() -> {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("发送消息失败", e);
            }
        }, asyncTaskExecutor))
        .toArray(CompletableFuture[]::new);

    // 等待所有发送完成
    CompletableFuture.allOf(futures).join();
}
```

**性能对比**：

| 方案 | 1万用户耗时 | 10万用户耗时 | 优点 | 缺点 |
|------|------------|-------------|------|------|
| 同步遍历 | ~5秒 | ~50秒 | 简单 | 阻塞线程，耗时长 |
| 异步发送 | ~1秒 | ~10秒 | 不阻塞 | 占用线程池资源 |
| 批量+异步 | ~0.5秒 | ~5秒 | 性能最优 | 代码复杂度高 |

---

### 11.3 方案二：引入消息队列（中大规模，10万-100万在线）

**核心思想**：WebSocket服务器只负责连接管理，消息推送交给MQ异步处理。

#### 架构图

```
客户端A ──┐
客户端B ──┤
客户端C ──┼──> WebSocket服务器集群 ──> Kafka集群 ──> 消费者集群 ──> 推送给客户端
...      ──┘           (只管连接)          (消息缓冲)      (异步推送)
```

**优势**：
- ✅ **解耦**：WebSocket服务器与消息推送解耦
- ✅ **削峰**：MQ缓冲消息，保护推送服务
- ✅ **扩展**：可以独立扩展推送服务实例
- ✅ **高可用**：MQ保证消息不丢失

#### 实现代码

**1. WebSocketHandler（只负责接收消息，不负责推送）**

```java
@Component
public class WebSocketHandler implements org.springframework.web.socket.WebSocketHandler {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;  // 注入Kafka模板

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;  // Redis存储在线状态

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserIdFromSession(session);

        // 存储到本地
        localSessions.put(userId, session);

        // ✅ 同步到Redis（其他服务器实例可见）
        SessionInfo sessionInfo = new SessionInfo(userId, session.getId(), getServerId());
        redisTemplate.opsForHash().put("ws:sessions", userId, JSON.toJSONString(sessionInfo));

        // ✅ 发送用户上线消息到Kafka
        UserOnlineEvent event = new UserOnlineEvent(userId, System.currentTimeMillis());
        kafkaTemplate.send("ws-user-online", userId, JSON.toJSONString(event));

        log.info("用户上线: {}, 当前在线数: {}", userId, localSessions.size());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        String userId = getUserIdFromSession(session);
        String payload = message.getPayload().toString();

        // ✅ 将消息发送到Kafka，而不是直接推送
        ChatMessage chatMsg = new ChatMessage(userId, payload, System.currentTimeMillis());
        kafkaTemplate.send("ws-chat-messages", userId, JSON.toJSONString(chatMsg));

        log.info("收到用户 [{}] 的消息，已发送到Kafka", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        String userId = getUserIdFromSession(session);

        // 从本地移除
        localSessions.remove(userId);

        // ✅ 从Redis移除
        redisTemplate.opsForHash().delete("ws:sessions", userId);

        // ✅ 发送用户下线消息到Kafka
        UserOfflineEvent event = new UserOfflineEvent(userId, System.currentTimeMillis());
        kafkaTemplate.send("ws-user-offline", userId, JSON.toJSONString(event));

        log.info("用户下线: {}", userId);
    }

    // ❌ 不再需要broadcastMessage方法！消息推送由消费者完成
}
```

**2. SessionInfo（会话信息）**

```java
@Data
@AllArgsConstructor
public class SessionInfo {
    private String userId;          // 用户ID
    private String sessionId;       // Session ID
    private String serverId;        // 服务器ID（用于集群识别）
}
```

**3. Kafka消费者（负责推送消息）**

```java
@Component
@Slf4j
public class WebSocketMessageConsumer {

    @Autowired
    private WebSocketSessionManager sessionManager;

    /**
     * 消费聊天消息
     */
    @KafkaListener(
        topics = "ws-chat-messages",
        concurrency = "10",  // 10个并发消费者
        groupId = "ws-message-consumer-group"
    )
    public void consumeChatMessage(ConsumerRecord<String, String> record) {
        String message = record.value();
        ChatMessage chatMsg = JSON.parseObject(message, ChatMessage.class);

        log.info("消费到消息: {}", chatMsg);

        // ✅ 异步推送给所有在线用户
        sessionManager.broadcastAsync(chatMsg.getContent());
    }

    /**
     * 消费用户上线事件
     */
    @KafkaListener(topics = "ws-user-online", groupId = "ws-event-consumer-group")
    public void consumeUserOnline(ConsumerRecord<String, String> record) {
        UserOnlineEvent event = JSON.parseObject(record.value(), UserOnlineEvent.class);
        log.info("用户上线事件: {}", event.getUserId());

        // 发送欢迎消息
        sessionManager.sendToUser(event.getUserId(), "欢迎加入聊天室！");
    }

    /**
     * 消费用户下线事件
     */
    @KafkaListener(topics = "ws-user-offline", groupId = "ws-event-consumer-group")
    public void consumeUserOffline(ConsumerRecord<String, String> record) {
        UserOfflineEvent event = JSON.parseObject(record.value(), UserOfflineEvent.class);
        log.info("用户下线事件: {}", event.getUserId());
    }
}
```

**4. SessionManager（支持集群的会话管理器）**

```java
@Component
@Slf4j
public class WebSocketSessionManager {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    // 本地Session缓存（快速查找）
    private final ConcurrentHashMap<String, WebSocketSession> localSessions = new ConcurrentHashMap<>();

    /**
     * 添加Session
     */
    public void addSession(String userId, WebSocketSession session) {
        localSessions.put(userId, session);

        // ✅ 同步到Redis（其他服务器实例可见）
        SessionInfo info = new SessionInfo(userId, session.getId(), getServerId());
        redisTemplate.opsForHash().put("ws:sessions", userId, JSON.toJSONString(info));
    }

    /**
     * 移除Session
     */
    public void removeSession(String userId) {
        localSessions.remove(userId);
        redisTemplate.opsForHash().delete("ws:sessions", userId);
    }

    /**
     * 异步广播消息给所有在线用户
     */
    @Async("asyncTaskExecutor")
    public void broadcastAsync(String message) {
        // ✅ 从Redis获取所有在线用户
        Map<Object, Object> allSessions = redisTemplate.opsForHash().entries("ws:sessions");

        log.info("广播消息给 {} 个在线用户", allSessions.size());

        for (Object sessionInfoObj : allSessions.values()) {
            SessionInfo info = JSON.parseObject(sessionInfoObj.toString(), SessionInfo.class);

            // ✅ 如果Session在本地，直接发送
            if (getServerId().equals(info.getServerId())) {
                sendToLocalUser(info.getUserId(), message);
            } else {
                // ✅ 如果Session在其他服务器，通过Redis Pub/Sub转发
                redisTemplate.convertAndSend("ws:forward:" + info.getServerId(),
                    new ForwardMessage(info.getUserId(), message));
            }
        }
    }

    /**
     * 发送消息给本地用户
     */
    public void sendToLocalUser(String userId, String message) {
        WebSocketSession session = localSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("发送消息给用户 [{}] 失败", userId, e);
            }
        }
    }

    /**
     * 获取当前服务器ID
     */
    private String getServerId() {
        return InetAddress.getLocalHost().getHostAddress() + ":" + 9058;
    }
}
```

**5. Redis Pub/Sub配置（跨服务器消息转发）**

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            WebSocketForwardListener listener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 订阅消息转发频道
        container.addMessageListener(listener, new PatternTopic("ws:forward:*"));

        return container;
    }
}

@Component
@Slf4j
public class WebSocketForwardListener implements MessageListener {

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        log.info("收到Redis转发消息: {}", body);

        // 解析转发消息
        ForwardMessage msg = JSON.parseObject(body, ForwardMessage.class);

        // 推送给本地用户
        sessionManager.sendToLocalUser(msg.getUserId(), msg.getMessage());
    }
}

@Data
@AllArgsConstructor
class ForwardMessage {
    private String userId;
    private String message;
}
```

**6. Maven依赖（需要添加）**

```xml
<!-- Kafka依赖 -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Redis依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**7. 配置文件（application.yml）**

```yaml
spring:
  # Kafka配置
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ws-consumer-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

  # Redis配置
  redis:
    host: localhost
    port: 6379
    database: 0
```

---

## 总结

WebSocket是实现实时通信的重要技术，在Spring Boot项目中推荐使用`WebSocketHandler`方式实现。

**核心要点**：

1. **选择合适的实现方式**（Spring Boot推荐WebSocketHandler）
2. **使用线程安全的集合**管理会话
3. **正确处理连接生命周期**
4. **实现心跳检测和自动重连**
5. **注意异常处理和资源清理**
6. **生产环境注意CORS和安全配置**
7. **区分广播和定向推送**：
   - 广播（Broadcast）：向所有在线用户发送消息
   - 定向推送（Unicast）：向特定用户发送消息（私聊、个人通知）
8. **使用ConcurrentHashMap管理UserId与Session的映射关系**：
   - 单设备：`Map<UserId, Session>`
   - 多设备：`Map<UserId, Set<Session>>`

**参考资料**：

- [Spring WebSocket官方文档](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [MDN WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [RFC 6455 (WebSocket协议)](https://tools.ietf.org/html/rfc6455)

**相关文档**：

- [WebSocket大规模IM架构演进](./WebSocket大规模IM架构演进.md) - 包含消息队列、集群部署等高级内容

---

*创建时间: 2025-12-23*
*作者: lyh*
*项目: WebSocket Demo*
*最后更新: 2025-12-23（新增第9章：WebSocket定向推送）*
