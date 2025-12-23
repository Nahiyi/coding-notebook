# CompletableFuture API 分类详解

## 核心架构思想

CompletableFuture 的 API 设计遵循清晰的职责分工：
- **静态方法**：创建和启动异步任务（工厂方法）
- **实例方法**：在已有任务上添加后续处理（组装方法）

---

## 一、静态方法：创建和启动任务

### 1.1 任务创建启动类

| API 方法 | 输入 | 输出 | 作用 | 线程池 |
|---------|------|------|------|-------|
| **`supplyAsync(Supplier)`** | ❌ 无 | ✅ 有 | 创建并执行**有返回值**的异步任务 | 默认ForkJoinPool |
| **`supplyAsync(Supplier, Executor)`** | ❌ 无 | ✅ 有 | 创建并执行**有返回值**的异步任务（自定义线程池） | 自定义Executor |
| **`runAsync(Runnable)`** | ❌ 无 | ❌ 无 | 创建并执行**无返回值**的异步任务 | 默认ForkJoinPool |
| **`runAsync(Runnable, Executor)`** | ❌ 无 | ❌ 无 | 创建并执行**无返回值**的异步任务（自定义线程池） | 自定义Executor |
| **`completedFuture(value)`** | ❌ 无 | ✅ 有 | 创建**已完成**的Future（用于测试或组合） | 无 |

### 1.2 组合控制类

| API 方法 | 输入 | 输出 | 作用 |
|---------|------|------|------|
| **`allOf(CompletableFuture...)`** | 多个Future | ✅ 有（Void） | 等待**所有**任务完成 |
| **`anyOf(CompletableFuture...)`** | 多个Future | ✅ 有（Object） | 等待**任意**一个任务完成 |

---

## 二、实例方法：组装后续处理

### 2.1 结果转换类（接收前置结果 → 返回新结果）

| API 方法 | 同步/异步 | 输入 | 输出 | 作用 | 线程池 |
|---------|----------|------|------|------|-------|
| **`thenApply(Function)`** | 同步 | ✅ 有 | ✅ 有 | 转换结果并返回新值 | 前置任务线程 |
| **`thenApplyAsync(Function)`** | 异步 | ✅ 有 | ✅ 有 | 转换结果并返回新值 | 默认ForkJoinPool |
| **`thenApplyAsync(Function, Executor)`** | 异步 | ✅ 有 | ✅ 有 | 转换结果并返回新值 | 自定义Executor |

### 2.2 结果消费类（接收前置结果 → 无返回结果）

| API 方法 | 同步/异步 | 输入 | 输出 | 作用 | 线程池 |
|---------|----------|------|------|------|-------|
| **`thenAccept(Consumer)`** | 同步 | ✅ 有 | ❌ 无 | 消费结果（如打印） | 前置任务线程 |
| **`thenAcceptAsync(Consumer)`** | 异步 | ✅ 有 | ❌ 无 | 消费结果（如打印） | 默认ForkJoinPool |
| **`thenAcceptAsync(Consumer, Executor)`** | 异步 | ✅ 有 | ❌ 无 | 消费结果（如打印） | 自定义Executor |

### 2.3 完成回调类（不关心结果 → 无返回结果）

| API 方法 | 同步/异步 | 输入 | 输出 | 作用 | 线程池 |
|---------|----------|------|------|------|-------|
| **`thenRun(Runnable)`** | 同步 | ❌ 无 | ❌ 无 | 前置任务完成后执行动作 | 前置任务线程 |
| **`thenRunAsync(Runnable)`** | 异步 | ❌ 无 | ❌ 无 | 前置任务完成后执行动作 | 默认ForkJoinPool |
| **`thenRunAsync(Runnable, Executor)`** | 异步 | ❌ 无 | ❌ 无 | 前置任务完成后执行动作 | 自定义Executor |

### 2.4 组合操作类

| API 方法 | 依赖关系 | 输入 | 输出 | 作用 |
|---------|----------|------|------|------|
| **`thenCompose(Function)`** | **串行依赖** | ✅ 有 | ✅ 有 | 依赖前置结果，返回新Future继续链式调用 |
| **`thenComposeAsync(Function)`** | **串行依赖** | ✅ 有 | ✅ 有 | 异步版本的thenCompose |
| **`thenCombine(other, BiFunction)`** | **并行组合** | ✅ 有 + ✅ 有 | ✅ 有 | 合并两个**独立**Future的结果 |
| **`thenCombineAsync(other, BiFunction)`** | **并行组合** | ✅ 有 + ✅ 有 | ✅ 有 | 异步版本的thenCombine |
| **`thenAcceptBoth(other, BiConsumer)`** | **并行组合** | ✅ 有 + ✅ 有 | ❌ 无 | 合并两个独立Future的结果并消费 |
| **`runAfterBoth(other, Runnable)`** | **并行组合** | ❌ 无 | ❌ 无 | 两个Future都完成后执行动作 |
| **`applyToEither(other, Function)`** | **竞争选择** | ✅ 有（较快者） | ✅ 有 | 两个Future中**较快**的结果转换 |
| **`acceptEither(other, Consumer)`** | **竞争选择** | ✅ 有（较快者） | ❌ 无 | 两个Future中**较快**的结果消费 |
| **`runAfterEither(other, Runnable)`** | **竞争选择** | ❌ 无 | ❌ 无 | 两个Future中**任意一个**完成就执行 |

### 2.5 异常处理类

| API 方法 | 处理时机 | 输入 | 输出 | 作用 |
|---------|----------|------|------|------|
| **`exceptionally(Function)`** | 异常时 | ✅ 有（异常） | ✅ 有 | 异常恢复，提供默认值 |
| **`handle(BiFunction)`** | 成功/失败 | ✅ 有（结果+异常） | ✅ 有 | 统一处理成功和失败 |
| **`whenComplete(BiConsumer)`** | 成功/失败 | ✅ 有（结果+异常） | ✅ 有 | 完成回调（不改变结果） |

### 2.6 完成控制类

| API 方法 | 作用 |
|---------|------|
| **`complete(value)`** | 手动完成（成功） |
| **`completeExceptionally(throwable)`** | 手动完成（失败） |
| **`cancel(mayInterruptIfRunning)`** | 取消任务 |

---

## 三、使用模式图解

### 3.1 典型调用模式
```java
// 1. 静态方法创建任务
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello");

// 2. 实例方法组装后续
future
  .thenApply(s -> s + " World")           // 转换：Hello → Hello World
  .thenApply(String::toUpperCase)         // 转换：Hello World → HELLO WORLD
  .thenAccept(System.out::println)        // 消费：打印结果
  .thenRun(() -> System.out.println("完成")); // 回调：完成后执行
```

### 3.2 组合模式选择
- **串行依赖**：用 `thenCompose`，前一步的结果决定后一步
- **并行组合**：用 `thenCombine`，两个独立任务合并结果
- **竞争选择**：用 `applyToEither`，取较快的结果

### 3.3 Async版本的选择
- **同步版本**：在前置任务的线程中执行（轻量级，但可能阻塞）
- **异步版本**：在线程池中执行（不阻塞，但需要线程切换开销）

---

## 四、设计哲学总结

1. **职责分离**：静态方法创建，实例方法组装
2. **类型安全**：编译期检查输入输出类型
3. **灵活控制**：同步/异步版本，线程池可配置
4. **功能完整**：转换、消费、组合、异常处理全覆盖
5. **链式编程**：流畅的API设计，易于理解

---

## 五、核心要点记忆

**记住这个分类原则：**
- **静态方法 = 启动器**：从无到有创建任务
- **实例方法 = 组装器**：在现有任务上添加逻辑

**记住这个命名规律：**
- **Apply** → 转换结果，有返回值
- **Accept** → 消费结果，无返回值
- **Run** → 完成回调，不关心结果
- **Async后缀** → 在线程池中异步执行