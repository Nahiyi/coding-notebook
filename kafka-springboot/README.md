# Kafka Spring Boot 示例

这是基于 Spring Boot 的 Kafka 实现示例，演示了如何在 Spring Boot 环境中使用 Kafka。

## 项目结构

```
kafka-springboot
├── src/main/java/cn/clazs/kafka
│   ├── KafkaSpringbootApplication.java      # 应用程序主类
│   ├── config
│   │   ├── KafkaProducerConfig.java         # Kafka 生产者配置
│   │   └── KafkaConsumerConfig.java         # Kafka 消费者配置
│   ├── producer
│   │   └── KafkaProducerService.java        # Kafka 生产者服务
│   ├── consumer
│   │   └── KafkaConsumerService.java        # Kafka 消费者服务
│   └── controller
│       └── KafkaController.java             # REST API 控制器
└── src/main/resources
    ├── application.yml                       # 应用配置文件
    └── logback-spring.xml                    # 日志配置文件
```

## 核心功能

### 1. Kafka 配置类

- **KafkaProducerConfig**: 配置 Kafka 生产者，包括序列化器、ACK 确认、重试机制等
- **KafkaConsumerConfig**: 配置 Kafka 消费者，包括消费者组、反序列化器、offset 自动提交等

### 2. 生产者服务 (KafkaProducerService)

提供两种发送消息方式：
- `sendMessage(topic, message)`: 发送简单消息
- `sendMessageWithKey(topic, key, message)`: 发送带 Key 的消息（相同 Key 的消息会发送到同一分区）

### 3. 消费者服务 (KafkaConsumerService)

使用 `@KafkaListener` 注解监听 Kafka 主题，自动消费消息并处理。

### 4. REST API 控制器

提供以下接口：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/kafka/send?message=xxx` | GET | 发送消息到默认主题 |
| `/kafka/send?topic=xxx` | POST | 发送消息到指定主题 |
| `/kafka/send-with-key?key=xxx&topic=xxx` | POST | 发送带 Key 的消息 |
| `/kafka/batch-send?count=10` | GET | 批量发送消息 |
| `/kafka/health` | GET | 健康检查 |

## 快速开始

### 1. 修改 Kafka 服务器地址

编辑 `src/main/resources/application.yml`，修改 Kafka 服务器地址：

```yaml
spring:
  kafka:
    bootstrap-servers: your-kafka-server:9092
```

### 2. 启动应用

```bash
mvn spring-boot:run
```

或者直接运行主类 `KafkaSpringbootApplication`

### 3. 测试接口

**发送单条消息：**
```bash
curl "http://localhost:8080/kafka/send?message=Hello Kafka"
```

**批量发送消息：**
```bash
curl "http://localhost:8080/kafka/batch-send?count=10"
```

**发送带 Key 的消息：**
```bash
curl -X POST "http://localhost:8080/kafka/send-with-key?key=user-123" \
  -H "Content-Type: text/plain" \
  -d "这是一条带Key的消息"
```

**发送到指定主题：**
```bash
curl -X POST "http://localhost:8080/kafka/send?topic=my-topic" \
  -H "Content-Type: text/plain" \
  -d "发送到自定义主题的消息"
```

## 配置说明

### Kafka 生产者配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `acks` | 确认机制（0/1/all/-1） | all |
| `retries` | 重试次数 | 3 |
| `batch-size` | 批量发送大小（字节） | 16384 |
| `linger-ms` | 批量发送延迟（毫秒） | 10 |
| `buffer-memory` | 缓冲区大小（字节） | 33554432 |

### Kafka 消费者配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `group-id` | 消费者组ID | spring-boot-kafka-group |
| `enable-auto-commit` | 自动提交offset | true |
| `auto-commit-interval-ms` | 自动提交间隔（毫秒） | 1000 |
| `auto-offset-reset` | offset重置策略 | earliest |

## 与原生 Kafka 客户端对比

### 原生 Kafka 客户端（kafka 模块）
- 需要手动创建 Properties 配置
- 需要手动管理 Producer 和 Consumer 生命周期
- 需要手动编写消息循环逻辑
- 适合需要细粒度控制的场景

### Spring Boot Kafka（kafka-springboot 模块）
- 自动配置，只需在 application.yml 中配置
- Spring 自动管理 Bean 生命周期
- 使用注解驱动，代码更简洁
- 与 Spring 生态无缝集成
- 适合 Spring Boot 项目快速开发

## 注意事项

1. 确保 Kafka 服务器已启动并可访问
2. 如果主题不存在，Kafka 会自动创建（取决于服务器配置）
3. 生产者发送是异步的，通过回调函数处理结果
4. 消费者会自动处理 offset 提交
5. 多个消费者使用相同的 group-id 时，会负载均衡消费

## 扩展功能

可以根据需要添加以下功能：

1. **消息序列化/反序列化**：支持 JSON、Avro 等格式
2. **消息重试机制**：消费失败时自动重试
3. **死信队列**：处理无法消费的消息
4. **批量消费**：提高消费吞吐量
5. **消息转换**：在发送和接收时转换消息格式
