# WebSocket 大规模IM架构演进

## 📚 目录

1. [当前方案的局限性](#1-当前方案的局限性)
2. [方案演进路线](#2-方案演进路线)
3. [IM + MQ 最佳实践架构](#3-im--mq-最佳实践架构)
4. [推荐的开源方案](#4-推荐的开源方案)

---

## 1. 当前方案的局限性

### 1.1 当前实现

```java
// 当前实现：所有Session存储在JVM堆内存中
private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
```

### 1.2 存在的问题

| 问题 | 影响 |
|------|------|
| **内存压力** | 10万在线用户 × 1KB = 100MB+ 内存 |
| **单机瓶颈** | 单机最大连接数约1-5万，无法水平扩展 |
| **单点故障** | 一台服务器宕机，所有连接丢失 |
| **广播效率低** | 遍历所有Session逐个发送，O(n)复杂度 |

### 1.3 适用场景

**适合**：
- ✅ 系统消息通知（低频、小规模）
- ✅ 学习、演示项目

**不适合**：
- ❌ 中大型IM应用（> 1万在线）
- ❌ 高并发实时聊天系统

---

## 2. 方案演进路线

根据在线用户规模，选择合适的方案：

| 方案 | 适用规模 | 复杂度 | 说明 |
|------|---------|--------|------|
| **单机内存存储** | < 1万在线 | ⭐ | 当前实现，适合学习和小规模应用 |
| **应用层优化** | 1-10万在线 | ⭐⭐ | 优化Session存储、异步发送 |
| **引入消息队列** | 10-100万在线 | ⭐⭐⭐⭐ | MQ削峰、解耦、可扩展 |
| **专业IM框架** | > 100万在线 | ⭐⭐⭐⭐⭐ | 使用成熟的开源方案 |

---

## 3. IM + MQ 最佳实践架构

### 3.1 核心架构图

> 仅供参考

```
客户端A (发送者)            客户端B (接收者)
     │                           │
     │ HTTP POST                 │ WebSocket连接
     ▼                           ▼
┌────────────────┐       ┌────────────────┐
│  业务服务器     │       │  WS网关集群     │
│  (HTTP API)    │       │  (长连接)      │
│                │       │                │
│ • 接收消息      │       │ • 维持连接     │
│ • 存储到DB      │       │ • 管理Session  │
│ • 发送到MQ      │       │ • 路由映射     │
└───────┬────────┘       └───────▲────────┘
        │                        │
        │ 发送消息                │ RPC调用
        ▼                        │
┌────────────────┐              │
│  消息队列(MQ)   │              │
│  Kafka/RocketMQ│              │
│                │              │
│ • 削峰填谷      │              │
│ • 解耦         │              │
└───────┬────────┘              │
        │                       │
        │ 消费                  │
        ▼                       │
┌────────────────┐              │
│  推送服务       │              │
│  (Consumer)    │              │
│                │              │
│ • 从MQ拉取      │              │
│ • 查Redis路由   │              │
│ • RPC调用网关   │──────────────┘
└────────────────┘
```

### 3.2 核心思想

**关键点**：
1. **上行（发送消息）**：走HTTP POST，快速返回
2. **下行（接收消息）**：走WebSocket长连接，实时推送
3. **WS网关**：只维护连接，不做业务逻辑
4. **推送服务**：从MQ消费消息，通过RPC调用WS网关完成推送
5. **Redis**：存储路由映射 `User_ID -> WS网关节点`

**核心优势**：
- ✅ **解耦**：发送和推送解耦，互不影响
- ✅ **削峰**：MQ缓冲消息，保护推送服务
- ✅ **扩展**：各组件可独立扩展
- ✅ **高可用**：MQ保证消息不丢失

### 3.3 完整消息流程

**场景：用户A 给 用户B 发送私聊消息**

1. **建立连接**：客户端B主动建立WebSocket连接到WS网关，网关记录路由映射到Redis
2. **发送消息**：客户端A通过HTTP POST发送消息给业务服务器
3. **业务处理**：业务服务器存库、发送消息到MQ、立即返回HTTP 200
4. **异步推送**：推送服务从MQ消费消息，查询Redis获取用户B的网关节点
5. **精准推送**：推送服务通过RPC调用WS网关，网关通过已建立的连接推送消息给客户端B

### 3.4 核心组件职责

#### WS网关（WebSocket Gateway）

**职责**：
- 维护WebSocket长连接
- 管理Session映射
- 接收RPC调用，推送消息给客户端

**关键实现**：
```java
// 维护用户与Session的映射
private ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

// 连接建立时
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    String userId = getUserIdFromSession(session);
    sessions.put(userId, session);

    // 同步路由信息到Redis
    redisTemplate.opsForHash().put("ws:routing", userId, "当前节点IP");
}

// 推送消息（RPC调用此方法）
public void pushMessage(String userId, String message) {
    WebSocketSession session = sessions.get(userId);
    if (session != null && session.isOpen()) {
        session.sendMessage(new TextMessage(message));
    }
}
```

#### 业务服务（Business Service）

**职责**：
- 提供HTTP API
- 业务逻辑处理（存数据库、消息审核）
- 发送消息到MQ

**关键实现**：
```java
@PostMapping("/message/send")
public ResponseEntity<?> sendMessage(@RequestBody SendMessageDTO dto) {
    // 1. 存储到数据库
    messageMapper.insert(new Message(dto.getFrom(), dto.getTo(), dto.getContent()));

    // 2. 发送到MQ
    kafkaTemplate.send("chat-messages", JSON.toJSONString(dto));

    // 3. 立即返回（不等待推送完成）
    return ResponseEntity.ok("发送成功");
}
```

#### 推送服务（Push Service / Consumer）

**职责**：
- 从MQ消费消息
- 查询Redis获取用户路由信息
- RPC调用WS网关推送消息

**关键实现**：
```java
@KafkaListener(topics = "chat-messages", groupId = "push-group")
public void consumeMessage(String message) {
    ChatMessage chatMsg = JSON.parseObject(message, ChatMessage.class);

    // 1. 查询接收者路由信息
    String gatewayNode = (String) redisTemplate.opsForHash()
        .get("ws:routing", chatMsg.getTo());

    if (gatewayNode != null) {
        // 2. RPC调用WS网关推送消息
        rpcClient.pushMessage(gatewayNode, chatMsg.getTo(), chatMsg.getContent());
    } else {
        // 用户离线，存储离线消息
        saveOfflineMessage(chatMsg);
    }
}
```

### 3.5 关键架构要点

1. **WebSocket只能由客户端主动建立连接**，服务端无法主动发起
2. **发送消息走HTTP，接收消息走WebSocket**（混合模式）
3. **WS网关只维护连接，不做业务逻辑**
4. **MQ消费者不持有WebSocket连接**，必须通过RPC调用WS网关
5. **Redis存储路由映射**：`UserId -> WS网关节点`

---

## 4. 推荐的开源方案

当在线用户超过百万时，建议使用专业的IM框架，而不是自己搭建。

### 4.1 Go语言方案（性能最优）

| 框架 | Stars | 特点 | 适用场景 |
|------|-------|------|---------|
| [GoIM](https://github.com/Terry-Mao/goim) | 10k+ | B站开源，支持百万级并发 | 大型直播、IM系统 |
| [Tiws](https://github.com/tinode/chat) | 8k+ | 完整IM解决方案 | 通用IM应用 |

**GoIM架构特点**：
- 使用**Go语言协程**（百万级并发）
- **长连接网关层** + **业务逻辑层**分离
- **Kafka**作为消息总线
- **Redis**存储在线状态

### 4.2 Java语言方案

| 框架 | Stars | 特点 | 适用场景 |
|------|-------|------|---------|
| [Open-IM-Server](https://github.com/OpenIMSDK/Open-IM-Server) | 12k+ | 完整IM服务，支持集群 | 企业级IM |

**Open-IM-Server架构特点**：
- 基于**Go + Java**混合架构
- 支持单聊、群聊、超大群
- 完整的消息存储、离线推送

### 4.3 商业方案

| 方案 | 特点 | 价格 |
|------|------|------|
| **融云** | 成熟稳定，国内领先 | 按日活收费 |
| **环信** | 功能完善，文档齐全 | 按日活收费 |
| **网易云信** | 大厂背书，稳定性好 | 按日活收费 |
| **腾讯云IM** | 与微信/QQ集成度高 | 按日活收费 |

---

## 5. 总结

### 核心要点

1. **WebSocket连接的本质**：客户端主动建立，服务端被动维护
2. **混合模式**：上行HTTP（发送），下行WebSocket（接收）
3. **MQ的作用**：削峰、解耦、扩展
4. **关键架构**：WS网关只维护连接，推送服务通过RPC调用网关

### 选择建议

- **学习/演示**：单机内存存储（当前实现）
- **小型应用**：应用层优化（ConcurrentHashMap + 异步发送）
- **中型应用**：引入消息队列（Kafka + Redis + RPC）
- **大型应用**：专业IM框架（GoIM、Open-IM-Server）

---

## 参考资料

- [Spring WebSocket官方文档](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [MDN WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [RFC 6455 (WebSocket协议)](https://tools.ietf.org/html/rfc6455)
- [GoIM - 百万级并发IM系统](https://github.com/Terry-Mao/goim)
- [Open-IM-Server - 开源IM服务](https://github.com/OpenIMSDK/Open-IM-Server)

---

*创建时间: 2025-12-23*
*作者: lyh*
*项目: WebSocket Demo*
*最后更新: 2025-12-23（精简版）*
