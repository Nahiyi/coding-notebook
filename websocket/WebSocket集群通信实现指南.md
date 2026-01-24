# 跨服务器WebSocket通信实现指南

## 目录
- [背景问题](#背景问题)
- [解决方案](#解决方案)
- [核心原理](#核心原理)
- [实现步骤](#实现步骤)
- [代码实现](#代码实现)
- [全链路验证](#全链路验证)
- [重要概念](#重要概念)
- [常见问题](#常见问题)

---

## 背景问题

### 场景描述
在单台WebSocket服务器的情况下，所有用户的WebSocket连接都存储在同一台服务器的内存中。当用户A想给用户B发送消息时，服务器可以直接在本地找到用户B的session并发送消息。

**但是**，当部署多台WebSocket服务器时，会出现以下问题：

```
服务器1（9058端口）: 用户A, 用户B, 用户C
服务器2（9090端口）: 用户D, 用户E, 用户F
```

当**用户A**想给**用户E**发送消息时：
1. 用户A连接在服务器1上
2. 用户E连接在服务器2上
3. 服务器1在本地查找用户E → **找不到**！
4. 消息发送失败

### 核心矛盾
- **每台服务器只存储自己连接的用户**
- **跨服务器的用户无法直接通信**
- **需要一个中间机制来转发消息**

---

## 解决方案

### 引入消息中间件

使用具备**发布/订阅**（Pub/Sub）功能的消息中间件作为消息转发中心：

```
┌─────────────┐         ┌─────────────┐
│  服务器1    │          │  服务器2    │
│  用户A      │          │  用户E      │
└──────┬──────┘         └──────┬──────┘
       │                       │
       │ 订阅主题               │ 订阅主题
       └───────────┬───────────┘
                   │
            ┌──────▼──────┐
            │   Redis     │
            │  Pub/Sub    │
            │  MY_MESSAGE │
            │   _TOPIC    │
            └─────────────┘
```

### 工作流程
1. **所有服务器都订阅同一个Redis主题**
2. **用户A发送消息给用户E**：
   - 服务器1收到请求
   - 将消息发布到Redis主题
   - 所有订阅了该主题的服务器都收到消息
3. **每台服务器在本地查找目标用户**：
   - 服务器1：查找用户E → 找不到 → 忽略
   - 服务器2：查找用户E → **找到了** → 发送成功！

---

## 核心原理

### Redis发布/订阅（Pub/Sub）

Redis的Pub/Sub模式是一种**消息传递模式**，具有以下特点：

#### 特点
- **即发即弃**（Fire and Forget）
  - 消息发布后立即传递给在线订阅者
  - 没有订阅者在线时，消息直接丢弃
  - 不保存消息历史

- **非持久化**
  - 消息不会在Redis中存储
  - 服务器重启后，未投递的消息丢失

- **广播机制**
  - 一个发布者，多个订阅者
  - 所有订阅者都会收到消息

#### 为什么看不到Redis数据？
使用 `KEYS *` 命令在Redis中看不到任何数据结构，这是**正常现象**！

**原因**：
- Pub/Sub是实时消息传递机制
- 消息不存储在Redis中
- 只是一个消息传递通道，不是存储

**类比**：就像广播电台，电台不会保存它播放过的所有节目。

---

## 实现步骤

### 第一步：添加依赖

在 `pom.xml` 中添加 Redisson 依赖：

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.25.2</version>
</dependency>
```

### 第二步：配置Redis连接

在 `application.yml` 中配置：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password:
    timeout: 2000ms
    database: 0
```

### 第三步：创建消息DTO

创建传输对象 `ChatMessage.java`：

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage implements Serializable {
    private String senderSessionId;   // 发送者会话ID
    private String receiverSessionId;  // 接收者会话ID
    private String content;            // 消息内容
}
```

### 第四步：实现Redis消息处理器

创建 `RedisMessageHandler.java`：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageHandler {

    public static final String TOPIC_NAME = "MY_MESSAGE_TOPIC";

    private final RedissonClient redissonClient;
    private final WebSocketHandler webSocketHandler;

    // 启动时自动订阅
    @PostConstruct
    public void init() {
        listenTopic();
    }

    // 监听Redis主题
    private void listenTopic() {
        RTopic topic = redissonClient.getTopic(TOPIC_NAME,
            new TypedJsonJacksonCodec(ChatMessage.class));

        // 添加监听器
        topic.addListener(ChatMessage.class, (channel, msg) -> {
            // 尝试在本地发送消息
            boolean sent = webSocketHandler.sendMessageToUser(
                msg.getReceiverSessionId(),
                msg.getContent()
            );

            if (sent) {
                log.info("本服务器成功向用户发送了消息");
            } else {
                log.info("本服务器未找到用户，忽略此消息");
            }
        });
    }

    // 发布消息到Redis
    public void publishMessage(String sender, String receiver, String content) {
        ChatMessage chatMessage = new ChatMessage(sender, receiver, content);

        RTopic topic = redissonClient.getTopic(TOPIC_NAME,
            new TypedJsonJacksonCodec(ChatMessage.class));
        topic.publish(chatMessage);
    }
}
```

### 第五步：在WebSocketHandler中添加点对点发送方法

```java
public boolean sendMessageToUser(String receiverSessionId, String message) {
    for (WebSocketSession session : sessionSet) {
        if (session.getId().equals(receiverSessionId)) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                    log.info("成功发送消息给用户: {}", receiverSessionId);
                    return true;  // 找到并发送成功
                }
            } catch (IOException e) {
                log.error("发送失败", e);
                return false;
            }
        }
    }
    log.info("未找到目标用户: {}", receiverSessionId);
    return false;  // 未找到目标用户
}
```

### 第六步：创建HTTP接口

在 `AdminController.java` 中添加点对点发送接口：

> 将原本单机模式下，直接基于“WebSocket推送发送消息”，变为了利用中间件解耦，使用HTTP API**间接**发送消息

```java
@Slf4j
@RestController
@RequestMapping("/api/ws")
public class AdminController {

    @Autowired
    private RedisMessageHandler redisMessageHandler;

    @PostMapping("/send-to-user")
    public Map<String, Object> sendToUser(@RequestBody Map<String, String> request) {
        String sender = request.get("senderSessionId");
        String receiver = request.get("receiverSessionId");
        String message = request.get("message");

        // 发布到Redis
        redisMessageHandler.publishMessage(sender, receiver, message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "消息已发布到Redis");
        return response;
    }
}
```

### 第七步：前端调用

前端通过HTTP调用发送接口：

```javascript
async function sendMessage() {
    const targetUser = document.getElementById('targetUserInput').value.trim();
    const message = document.getElementById('messageInput').value.trim();
    const mySessionId = localStorage.getItem('mySessionId');

    const response = await fetch('/api/ws/send-to-user', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            senderSessionId: mySessionId,
            receiverSessionId: targetUser,
            message: message
        })
    });

    const data = await response.json();
    console.log(data.message);
}
```

---

## 代码实现

### 核心类说明

#### 1. ChatMessage（消息DTO）
- 封装发送者、接收者、消息内容
- 实现 `Serializable` 接口用于序列化

#### 2. RedisMessageHandler（Redis消息处理器）
- **订阅Redis主题**：`@PostConstruct` 启动时自动订阅
- **监听消息**：使用 `addListener()` 监听Redis消息
- **发布消息**：使用 `publish()` 发布消息到Redis

#### 3. WebSocketHandler（WebSocket处理器）
- **存储所有session**：`CopyOnWriteArraySet<WebSocketSession> sessionSet`
- **点对点发送**：`sendMessageToUser()` 在本地查找并发送
- **返回结果**：找到用户返回 `true`，未找到返回 `false`

#### 4. AdminController（HTTP接口）
- **接收前端请求**：`POST /api/ws/send-to-user`
- **调用RedisMessageHandler**：发布消息到Redis

---

## 全链路验证

### 实际测试日志

#### 场景：用户A（9058端口）发送消息给用户E（9090端口）

**服务器1（9058端口）日志**：
```
2026-01-24 22:48:45.690  INFO  WebSocketHandler - WebSocket连接建立: b2ba419f...
2026-01-24 22:49:04.515  INFO  RedisMessageHandler - 已发布消息到Redis主题
                                - 发送者: b2ba419f..., 接收者: fbfa5611...
2026-01-24 22:49:04.525  INFO  RedisMessageHandler - 收到Redis消息
2026-01-24 22:49:04.525  INFO  WebSocketHandler - 未找到目标用户 [fbfa5611...]，
                                该用户不在此服务器上
2026-01-24 22:49:04.525  INFO  RedisMessageHandler - 本服务器未找到用户，忽略此消息
```

**服务器2（9090端口）日志**：
```
2026-01-24 22:51:30.321  INFO  WebSocketHandler - WebSocket连接建立: 5066211e...
2026-01-24 22:51:40.235  INFO  RedisMessageHandler - 收到Redis消息
2026-01-24 22:51:40.235  INFO  WebSocketHandler - 成功发送消息给用户 [5066211e...]: 客户1的消息
2026-01-24 22:51:40.235  INFO  RedisMessageHandler - 本服务器成功向用户发送了消息
```

### 流程分析

✅ **步骤1**：用户A通过HTTP调用 `POST /api/ws/send-to-user`
✅ **步骤2**：AdminController接收请求，调用 `redisMessageHandler.publishMessage()`
✅ **步骤3**：消息发布到Redis主题 `MY_MESSAGE_TOPIC`
✅ **步骤4**：所有订阅了该主题的服务器都收到消息
✅ **步骤5**：
   - 服务器1收到消息，在本地查找 → 找不到 → 返回false
   - 服务器2收到消息，在本地查找 → **找到了** → 发送成功 → 返回true
✅ **步骤6**：用户E通过WebSocket收到消息

**全链路验证成功！** ✅

---

## 重要概念

### 1. @PostConstruct注解

**作用**：在Bean初始化完成后自动调用

**执行时机**：
```
构造方法 → 依赖注入 → @PostConstruct方法 → Bean就绪
```

**使用场景**：
- 初始化操作
- 启动后台任务
- 订阅消息队列

### 2. Lambda表达式 vs 匿名内部类

#### Lambda表达式（推荐）
```java
topic.addListener(ChatMessage.class, (channel, msg) -> {
    log.info("收到消息: {}", msg.getContent());
});
```

#### 匿名内部类
```java
topic.addListener(ChatMessage.class, new MessageListener<ChatMessage>() {
    @Override
    public void onMessage(CharSequence channel, ChatMessage msg) {
        log.info("收到消息: {}", msg.getContent());
    }
});
```

**对比**：
- Lambda：简洁，推荐使用
- 匿名内部类：更冗长，但可以包含更复杂逻辑

### 3. TypedJsonJacksonCodec

**作用**：JSON序列化编解码器

**功能**：
- **发布消息**：Java对象 → JSON字符串
- **接收消息**：JSON字符串 → Java对象

**为什么需要？**
- Redis只能存储字符串和字节
- 不能直接存储Java对象
- 必须序列化为JSON才能传输

### 4. CopyOnWriteArraySet

**特点**：
- 线程安全的Set集合
- 适合读多写少的场景
- 写操作时复制整个数组

**为什么使用？**
- WebSocket连接可能被多线程访问
- 保证线程安全
- 写操作（添加/删除session）相对较少

---

## 常见问题

> **首先，必须声明**，本次 commit 的 demo 代码，仅仅是为了演示多机器跨 websocket 服务器进行通信的必经之路和最简实现方式的代码简单实践！

### Q1: 为什么在Redis中看不到数据？

**A**: 这是正常现象！

Redis Pub/Sub是**即发即弃**模式：
- 消息发布后立即传递给在线订阅者
- 不会在Redis中存储任何数据结构
- 使用 `KEYS *` 命令看不到任何相关数据

**类比**：就像打电话，电话公司不会保存所有的通话内容。

### Q2: 如果需要持久化消息怎么办？

**A**: 使用 **Redis Stream** 而不是 Pub/Sub

Redis Stream特点：
- 消息持久化存储
- 支持消息回溯
- 支持消费者组

### Q3: 消息会丢失吗？

**A**: 在Pub/Sub模式下，是的！

**可能丢失的场景**：
- 订阅者离线
- Redis服务器重启
- 网络中断

**解决方案**：

- 使用Redis Stream（持久化）
- 使用消息队列（RabbitMQ、Kafka）
- 实现消息确认机制

### Q4: 多台服务器如何保证消息不重复？

**A**: 通过返回值控制

每台服务器都会收到消息，但：
- 找到目标用户的服务器：返回 `true`，发送成功
- 未找到目标用户的服务器：返回 `false`，忽略消息

**只有一台服务器会真正发送消息**，不会重复。

### Q5: @PostConstruct和构造方法的区别？

**A**: 执行时机不同

```
构造方法 → 创建对象，字段还未注入
@PostConstruct → 字段已注入，可以调用其他Bean的方法
```

**错误示例**：
```java
public RedisMessageHandler(RedissonClient redissonClient) {
    // ❌ 错误：其他Bean可能还未注入
    redissonClient.getTopic("...");
}
```

**正确示例**：
```java
@PostConstruct
public void init() {
    // ✅ 正确：所有依赖都已注入
    listenTopic();
}
```

### Q6: 为什么要使用Redisson而不是Jedis？

**A**: Redisson提供更高级的功能

**Redisson优势**：
- 分布式对象（Lock、Map、Set等）
- 发布/订阅封装更简单
- 自动序列化/反序列化
- 连接池管理
- 集群支持

**Jedis**：
- 更底层，需要手动管理更多细节
- 适合简单场景

---

## 总结

### 核心要点

1. **问题本质**：每台服务器只存储自己的用户连接
2. **解决方案**：引入Redis Pub/Sub作为消息转发中心
3. **工作机制**：
   - 所有服务器订阅同一个主题
   - 消息发布到Redis后广播给所有服务器
   - 只有目标用户所在的服务器能成功发送
4. **关键特性**：即发即弃，不存储消息历史

### 适用场景

✅ **适合**：
- 实时通信
- 消息广播
- 即时通讯
- 在线通知

❌ **不适合**：
- 需要消息持久化
- 需要消息回溯
- 离线消息

### 扩展方向

如果需要更强大的功能，可以考虑：
- **Redis Stream**：持久化消息队列
- **RabbitMQ**：企业级消息队列
- **Kafka**：高吞吐量消息队列
- **WebSocket集群**：使用Spring WebSocket的Message Broker

---

## 附录

### 相关技术栈

- **Spring Boot 2.7.18**：基础框架
- **WebSocket**：实时通信协议
- **Redisson 3.25.2**：Redis客户端
- **Redis**：消息中间件
- **Lombok**：简化代码

### 参考文档

- [Redis Pub/Sub官方文档](https://redis.io/docs/manual/pubsub/)
- [Redisson官方文档](https://redisson.org)
- [Spring WebSocket文档](https://docs.spring.io/spring-framework/reference/web/websocket.html)

---

**作者**: clazs
**日期**: 2026年1月24日 23:11
