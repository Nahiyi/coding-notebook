# AQS 核心知识总结

> AQS（AbstractQueuedSynchronizer）- Java 并发包的核心框架

---

## 一、AQS 本质

**AQS 是一个框架（抽象类），不是具体的锁或同步器**

它提供了构建同步工具的基础设施和统一**模板**，具体的同步工具通过继承和重写来实现差异化功能。

### 1.1 架构图

```
┌─────────────────────────────────────────────┐
│              AbstractQueuedSynchronizer       │
│                   (AQS 框架)                  │
└──────────────────┬──────────────────────────┘
                   │
      ┌────────────┼────────────┐
      │            │            │
      ▼            ▼            ▼
┌─────────┐  ┌──────────┐  ┌────────┐
│ Reentrant│  │Semaphore │  │CountDown│
│   Lock   │  │          │  │ Latch   │
└─────────┘  └──────────┘  └────────┘
      │            │            │
  独占模式      共享模式      共享模式
```

### 1.2 架构分层

```
┌─────────────────────────────────────────────────┐
│         用户层：使用 API                          │
│  ReentrantLock / Semaphore / CountDownLatch     │
└─────────────────┬───────────────────────────────┘
                  │ 组合
                  ↓
┌─────────────────────────────────────────────────┐
│         实现层：Sync 内部类                      │
│  FairSync / NonfairSync / Sync                  │
│  - 继承 AQS                                      │
│  - 重写 tryAcquire/tryRelease                   │
│  - 定义 state 语义                              │
└─────────────────┬───────────────────────────────┘
                  │ 继承
                  ↓
┌─────────────────────────────────────────────────┐
│         框架层：AQS                               │
│  - 提供：state + CAS + 队列                      │
│  - 定义：模板方法（acquire/release）             │
│  - 留白：钩子方法（tryAcquire/tryRelease）       │
└─────────────────────────────────────────────────┘
```

---

## 二、AQS 核心三要素

```
┌──────────────────────────────────────┐
│           AQS 核心三要素               │
├──────────────────────────────────────┤
│ 1. state（同步状态）                  │
│    - volatile int                    │
│    - 通过 CAS 修改                    │
│    - 含义由子类决定                   │
├──────────────────────────────────────┤
│ 2. CLH 队列变体（双向链表）            │
│    - 管理等待线程                     │
│    - FIFO 顺序                        │
├──────────────────────────────────────┤
│ 3. 模板方法 + 钩子方法                 │
│    - acquire/release（模板）          │
│    - tryAcquire/tryRelease（钩子）    │
└──────────────────────────────────────┘
```

### 2.1 模板方法模式

```
AQS 提供的模板方法（算法流程）：
┌─────────────────────────────────────┐
│  acquire(int arg)                   │
│  ├─ tryAcquire(arg)    ← 钩子方法   │
│  ├─ acquire失败？                   │
│  │   ├─ 加入等待队列                │
│  │   └─ 阻塞当前线程                │
│  └─ 获取成功，返回                  │
└─────────────────────────────────────┘
```

**子类工作**：只重写 `tryAcquire()`，定义"如何尝试获取"

### 2.2 分离关注点

| AQS 负责（框架） | 子类负责（实现） |
|-----------------|----------------|
| 如何管理等待队列 | state 的含义是什么 |
| 如何原子地修改状态 | 什么条件下可以获取 |
| 如何阻塞和唤醒线程 | 如何释放资源 |
| 如何保证线程安全 | - |

---

## 三、三大同步工具对比

### 3.1 对比表

| 特性 | ReentrantLock | Semaphore | CountDownLatch |
|------|---------------|-----------|----------------|
| **AQS 模式** | 独占 | 共享 | 共享 |
| **state 含义** | 0=未锁定，>0=重入次数 | 可用许可证数量 | 倒计数值 |
| **可重用性** | 可重复使用 | 可重复使用 | 一次性 |
| **主要用途** | 互斥访问 | 资源限流 | 线程协调 |
| **典型场景** | 替代 synchronized | 连接池、限流 | 并行计算 |

### 3.2 state 在不同工具中的含义

```
ReentrantLock:
  state = 0  → 未锁定
  state > 0  → 重入次数

Semaphore:
  state = 许可证数量
  state > 0  → 有可用许可
  state = 0  → 无可用许可

CountDownLatch:
  state = 倒计数值
  state > 0  → 仍在等待
  state = 0  → 所有任务完成
```

### 3.3 独占模式 vs 共享模式

| 特性 | 独占模式 | 共享模式 |
|------|---------|---------|
| **持有者数量** | 只能有一个 | 可以有多个 |
| **try 方法** | tryAcquire/tryRelease | tryAcquireShared/tryReleaseShared |
| **返回值** | boolean（成功/失败） | int（负数失败，0/正数成功） |
| **唤醒策略** | 只唤醒后继节点 | 可能唤醒多个节点 |
| **典型应用** | ReentrantLock | Semaphore, CountDownLatch |

---

## 四、设计模式与原则

### 4.1 设计模式组合

| 模式 | 体现 |
|------|------|
| **模板方法模式** | AQS 定义算法骨架，子类实现具体步骤 |
| **策略模式** | 不同同步器有不同的 state 语义和获取策略 |
| **组合模式** | 外部类组合 Sync 对象，而非直接继承 AQS |

### 4.2 组合 > 继承

```java
// ✅ 推荐：组合内部类
public class ReentrantLock {
    private final Sync sync = new Sync();

    abstract static class Sync extends AbstractQueuedSynchronizer {
        // 实现细节隐藏在内部类中
    }
}
```

**好处**：封装性更好，隐藏 AQS 实现细节

### 4.3 设计原则

- **开闭原则**：对扩展开放（重写钩子），对修改封闭（AQS 核心不变）
- **单一职责**：AQS 负责队列管理，子类负责语义定义
- **依赖倒置**：AQS 依赖抽象（钩子方法），不依赖具体实现

---

## 五、核心 API 速查

### 5.1 ReentrantLock

```java
// 创建
ReentrantLock lock = new ReentrantLock();           // 非公平（默认）
ReentrantLock fairLock = new ReentrantLock(true);  // 公平

// 使用
lock.lock();                          // 阻塞获取
lock.unlock();                        // 释放
boolean success = lock.tryLock();      // 非阻塞尝试
lock.lockInterruptibly();             // 可中断获取

// 查询
boolean isFair = lock.isFair();       // 是否公平锁
int count = lock.getHoldCount();      // 重入次数
```

### 5.2 Semaphore

```java
// 创建
Semaphore sem = new Semaphore(3);     // 3个许可

// 使用
sem.acquire();                        // 获取1个（阻塞）
sem.release();                        // 释放1个
boolean ok = sem.tryAcquire();         // 非阻塞尝试

// 查询
int available = sem.availablePermits(); // 可用许可数
```

### 5.3 CountDownLatch

```java
// 创建
CountDownLatch latch = new CountDownLatch(3);

// 使用
latch.await();                         // 等待（阻塞）
latch.countDown();                     // 计数-1
long count = latch.getCount();        // 当前计数值
```

---

## 六、常见问题

### Q1: ReentrantLock vs synchronized？

| 特性 | ReentrantLock | synchronized |
|------|---------------|-------------|
| 可中断 | ✅ | ❌ |
| 可超时 | ✅ | ❌ |
| 公平性 | 可选 | 非公平 |
| 使用难度 | 需手动 unlock | 自动释放 |

### Q2: 公平锁 vs 非公平锁？

| 特性 | 公平锁 | 非公平锁（默认） |
|------|--------|-----------------|
| 获取顺序 | 严格 FIFO | 允许插队 |
| 吞吐量 | 较低 | 较高 |
| 线程饥饿 | 无 | 可能出现 |
| 适用场景 | 需要严格顺序 | 追求性能 |

### Q3: CountDownLatch vs CyclicBarrier？

| 特性 | CountDownLatch | CyclicBarrier |
|------|----------------|---------------|
| 可重用 | ❌ 一次性 | ✅ 可重用 |
| 计数器 | 只能减 | 可重置 |
| 场景 | 等待多个任务 | 循环同步 |

---

## 七、学习路径

1. **ReentrantLock** → 理解独占模式、可重入性
2. **Semaphore** → 理解共享模式、资源限流
3. **CountDownLatch** → 理解线程协调、并行计算

### 运行演示

```bash
cd D:\develop\Projects\IDEAProjects\code-playground\jdk
mvn clean compile

mvn exec:java -Dexec.mainClass="cn.clazs.jdk.aqs.lock.ReentrantLockDemo"
mvn exec:java -Dexec.mainClass="cn.clazs.jdk.aqs.semaphore.SemaphoreDemo"
mvn exec:java -Dexec.mainClass="cn.clazs.jdk.aqs.latch.CountDownLatchDemo"
```

---

## 八、一句话总结

> **AQS 是同步工具的"半成品"，提供了核心机制（state + CAS + 队列），留下了扩展点（钩子方法），子类通过重写扩展点来实现具体功能。**

**记住**：
- **ReentrantLock** → 互斥访问（独占）
- **Semaphore** → 资源限流（共享）
- **CountDownLatch** → 线程协调（共享）

> 创建时间：2026年1月5日 22:59:48
>
> 更新时间：2026年1月5日 22:59:52
>
> 作者：clazs
