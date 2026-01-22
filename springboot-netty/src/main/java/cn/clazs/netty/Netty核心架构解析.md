# Netty核心架构深度解析

> 从架构到API层面,彻底理解Netty的核心组件和工作原理

---

## 📚 目录

- [一、Netty整体架构](#一netty整体架构)
- [二、核心组件详解](#二核心组件详解)
- [三、ServerBootstrap启动流程](#三serverbootstrap启动流程)
- [四、ChannelPipeline工作原理](#四channelpipeline工作原理)
- [五、三种服务器的Pipeline对比](#五三种服务器的pipeline对比)
- [六、实战代码分析](#六实战代码分析)
- [七、常见问题FAQ](#七常见问题faq)

---

## 一、Netty整体架构

### 1.1 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      Netty应用层                             │
│  (TcpServer / WebSocketServer / HttpServer)                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                  ServerBootstrap                            │
│  - 配置服务器参数                                            │
│  - 设置EventLoopGroup                                       │
│  - 注册ChannelHandler                                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
┌───────▼──────────┐         ┌───────▼──────────┐
│   bossGroup      │         │  workerGroup     │
│  (1个线程)        │         │  (CPU核心数*2)   │
│                  │         │                  │
│ 职责:            │         │ 职责:            │
│ - 接收客户端连接  │         │ - 处理I/O读写    │
│ - 将连接分发给   │         │ - 处理Handler逻辑 │
│   workerGroup    │         │ - 执行业务逻辑    │
└──────────────────┘         └──────────────────┘
        │                             │
        └──────────────┬──────────────┘
                       │
        ┌──────────────▼──────────────────────────────┐
        │         Channel (连接通道)                   │
        │  - 每个客户端连接对应一个Channel              │
        │  - 每个Channel绑定到一个EventLoop            │
        └──────────────┬──────────────────────────────┘
                       │
        ┌──────────────▼──────────────────────────────┐
        │      ChannelPipeline (处理器链)              │
        │  ┌────────┬────────┬────────┬────────┐      │
        │  │Handler1│Handler2│Handler3│Handler4│      │
        │  └────────┴────────┴────────┴────────┘      │
        │     入站 ────────▶                          │
        │     出站 ◀────────                          │
        └─────────────────────────────────────────────┘
```

### 1.2 Reactor线程模型

Netty采用的是**改进版Reactor线程模型**,主要特点:

```
                ┌─────────────────┐
                │  客户端1 连接    │
                └────────┬────────┘
                         │
                ┌────────▼────────┐
                │  Acceptor       │
                │  (bossGroup)    │
                │  1个线程         │
                └────────┬────────┘
                         │
        ┌────────────────┴───────────────┐
        │                                │
┌───────▼──────┐                  ┌──────▼──────┐
│  Selector 1  │                  │ Selector 2  │
│  (worker)    │                  │  (worker)   │
│              │                  │             │
│  处理:       │                  │  处理:      │
│  - 客户端1   │                  │  - 客户端3  │
│  - 客户端2   │                  │  - 客户端4  │
└──────────────┘                  └─────────────┘
```

**关键点:**
- `bossGroup = new NioEventLoopGroup(1)`: 单线程接收连接,避免竞争
- `workerGroup = new NioEventLoopGroup()`: 默认CPU核心数×2,处理I/O和业务
- **一个EventLoop可以绑定多个Channel**
- **一个Channel只会绑定到一个EventLoop**(保证线程安全)

---

## 二、核心组件详解

### 2.1 EventLoopGroup (线程组)

#### 职责
- **管理EventLoop线程的生命周期**
- **分配EventLoop给Channel**
- **处理I/O事件和任务**

#### 构造方法
```java
// 无参: 默认CPU核心数 * 2
EventLoopGroup group = new NioEventLoopGroup();

// 指定线程数
EventLoopGroup bossGroup = new NioEventLoopGroup(1);   // 接收连接
EventLoopGroup workerGroup = new NioEventLoopGroup(4);  // 处理I/O
```

#### 工作原理
```
EventLoopGroup (包含多个EventLoop)
    │
    ├─── EventLoop1 (线程1)
    │     ├── Channel1 (客户端A)
    │     ├── Channel2 (客户端B)
    │     └── Channel3 (客户端C)
    │
    ├─── EventLoop2 (线程2)
    │     ├── Channel4 (客户端D)
    │     └── Channel5 (客户端E)
    │
    └─── EventLoop3 (线程3)
          ├── Channel6 (客户端F)
          └── TaskQueue (异步任务队列)
```

**关键特性:**
1. **EventLoop与Channel是一对多关系**
2. **一个Channel的生命周期内,始终绑定同一个EventLoop**
3. **所有I/O操作和Handler执行都在EventLoop线程中,保证线程安全**

---

### 2.2 ServerBootstrap (启动引导类)

#### 核心方法链

```java
ServerBootstrap bootstrap = new ServerBootstrap();

bootstrap.group(bossGroup, workerGroup)           // 1. 设置线程组
         .channel(NioServerSocketChannel.class)   // 2. 设置Channel类型
         .option(ChannelOption.SO_BACKLOG, 128)   // 3. 设置ServerSocket参数
         .childOption(ChannelOption.SO_KEEPALIVE, true) // 4. 设置Socket参数
         .handler(new LoggingHandler(...))        // 5. 设置ServerSocket的Handler
         .childHandler(new ChannelInitializer<>() { // 6. 设置Socket的Handler
             @Override
             protected void initChannel(SocketChannel ch) {
                 ch.pipeline().addLast(...);
             }
         });
```

#### 方法详解

##### 1) `group(EventLoopGroup parentGroup, EventLoopGroup childGroup)`
- **parentGroup**: `bossGroup`,负责接收连接
- **childGroup**: `workerGroup`,负责处理I/O

##### 2) `channel(Class<? extends ServerChannel> channelClass)`
指定服务器Channel类型:
- `NioServerSocketChannel`: NIO传输,Java NIO实现
- `OioServerSocketChannel`: 阻塞I/O(已废弃)
- `EpollServerSocketChannel`: Linux专用,性能更高(需引入netty-transport-native-epoll)

##### 3) `option(ChannelOption<T> option, T value)`
设置**ServerSocket**的参数(影响所有连接):
```java
.option(ChannelOption.SO_BACKLOG, 128)        // 连接队列大小
.option(ChannelOption.SO_REUSEADDR, true)     // 地址重用
```

##### 4) `childOption(ChannelOption<T> option, T value)`
设置每个**Socket连接**的参数:
```java
.childOption(ChannelOption.SO_KEEPALIVE, true)   // 保持连接
.childOption(ChannelOption.TCP_NODELAY, true)    // 禁用Nagle算法,降低延迟
```

##### 5) `handler(ChannelHandler handler)`
设置**ServerSocketChannel**的Handler(只执行一次,处理连接事件):
```java
.handler(new LoggingHandler(LogLevel.INFO))  // 日志记录
```

##### 6) `childHandler(ChannelHandler childHandler)`
设置每个**SocketChannel**的Handler(每个连接都会创建新的Pipeline):
```java
.childHandler(new ChannelInitializer<SocketChannel>() {
    @Override
    protected void initChannel(SocketChannel ch) {
        // 为每个连接配置Pipeline
        ch.pipeline().addLast(new MyHandler());
    }
})
```

---

### 2.3 Channel (网络连接通道)

#### 职责
- **代表一个网络连接**(客户端<->服务器)
- **提供I/O操作接口**(read, write, connect, bind)
- **获取配置信息**(ChannelConfig, ChannelPipeline)

#### 继承体系
```
Channel (接口)
    │
    ├── AbstractChannel (抽象类)
    │     │
    │     ├── NioServerSocketChannel (服务器端监听Channel)
    │     ├── NioSocketChannel (客户端连接Channel)
    │     └── ...
    │
    └── 关联组件:
         - ChannelPipeline: 处理器链
         - ChannelConfig: 配置信息
         - ChannelFuture: 异步操作结果
         - EventLoop: 事件循环
```

#### 核心API
```java
channel.writeAndFlush(msg);    // 写数据并刷新
channel.close();                // 关闭连接
channel.remoteAddress();        // 获取远程地址
channel.localAddress();         // 获取本地地址
channel.eventLoop();            // 获取绑定的EventLoop
channel.pipeline();             // 获取Pipeline
```

---

### 2.4 ChannelPipeline (处理器链)

#### 核心理念
**责任链模式**: 数据流经多个Handler,每个Handler负责特定功能

#### 结构图
```
              入站事件(Inbound)           出站事件(Outbound)
                   ▲                          │
                   │                          │
┌──────────────────────────────────────────────────────────┐
│ ChannelPipeline                                          │
│                                                          │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   │
│  │Handler1 │   │Handler2 │   │Handler3 │   │Handler4 │   │
│  │(Inbound)│   │(Inbound)│   │(Outbound│   │(Outbound│   │
│  └────┬────┘   └────┬────┘   └────┬────┘   └────┬────┘   │
│       │             │             │             │        │
│       └─────────────┴─────────────┴─────────────┘        │
│                           │                              │
│                    ChannelHandlerContext                 │
└──────────────────────────────────────────────────────────┘
                   │                          ▲
                   │                          │
              读数据(channelRead)         写数据(write)
```

#### Handler类型

##### 1) ChannelInboundHandler (入站处理器)
**处理从客户端到服务器的事件:**

```java
public interface ChannelInboundHandler {
    void channelRegistered(ChannelHandlerContext ctx);    // Channel注册到EventLoop
    void channelActive(ChannelHandlerContext ctx);        // Channel激活(连接建立)
    void channelRead(ChannelHandlerContext ctx, Object msg); // 读取数据
    void channelReadComplete(ChannelHandlerContext ctx);  // 读取完成
    void channelInactive(ChannelHandlerContext ctx);      // Channel失活(连接断开)
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause); // 异常
}
```

##### 2) ChannelOutboundHandler (出站处理器)
**处理从服务器到客户端的事件:**
```java
public interface ChannelOutboundHandler {
    void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise);
    void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);
    void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise); // 写数据
    void flush(ChannelHandlerContext ctx); // 刷新数据
    void read(ChannelHandlerContext ctx);  // 请求读数据
    void close(ChannelHandlerContext ctx, ChannelPromise promise); // 关闭连接
}
```

#### 数据流向
```
客户端发送数据 "Hello"
        │
        ▼
channelRead() 触发
        │
        ▼
Handler1.channelRead() ──▶ 解码
        │
        ▼
Handler2.channelRead() ──▶ 业务逻辑
        │
        ▼
ctx.write("Response") ──▶ 出站
        │
        ▼
Handler3.write() ──▶ 编码
        │
        ▼
发送到客户端
```

---

## 三、ServerBootstrap启动流程

### 3.1 完整流程图

```
1. 创建EventLoopGroup
   └─▶ bossGroup = new NioEventLoopGroup(1);
   └─▶ workerGroup = new NioEventLoopGroup();

2. 创建ServerBootstrap
   └─▶ ServerBootstrap bootstrap = new ServerBootstrap();

3. 配置参数
   └─▶ bootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(...)
        .childOption(...)
        .childHandler(...);

4. 绑定端口
   └─▶ ChannelFuture future = bootstrap.bind(port).sync();
        │
        ▼
   ┌─────────────────────────────┐
   │ bind()内部流程:              │
   │ 1. 创建ServerSocketChannel  │
   │ 2. 注册到bossGroup          │
   │ 3. 调用pipeline().addLast() │
   │ 4. bind(address)            │
   └─────────────────────────────┘

5. 等待启动完成
   └─▶ future.channel().closeFuture().sync();

6. 关闭资源
   └─▶ workerGroup.shutdownGracefully();
   └─▶ bossGroup.shutdownGracefully();
```

### 3.2 bind()内部原理

```java
// 简化版伪代码
public ChannelFuture bind(int port) {
    // 1. 创建Channel
    Channel channel = channelFactory.newChannel();

    // 2. 注册到EventLoop (不指定则使用bossGroup)
    channel.unsafe().register(eventLoop);
    // 此时pipeline中只有head和handler

    // 3. 调用pipeline.invokeBind()
    pipeline.bind(port);

    // 4. Java NIO底层: ServerSocketChannel.bind(port)
    javaChannel().socket().bind(new InetSocketAddress(port));

    return channel.newFuture();
}
```

---

## 四、ChannelPipeline工作原理

### 4.1 Pipeline初始化

```java
// 创建Channel时,自动初始化Pipeline
protected AbstractChannel(Channel parent) {
    this.pipeline = new DefaultChannelPipeline(this);
}

// DefaultChannelPipeline构造函数
protected DefaultChannelPipeline(Channel channel) {
    this.channel = channel;
    tail = new TailContext(this);    // 尾部Handler(系统内部)
    head = new HeadContext(this);    // 头部Handler(系统内部)
    head.next = tail;
    tail.prev = head;
}
```

**初始Pipeline结构:**
```
HeadContext ─────────▶ TailContext
   (出站)              (入站)
```

### 4.2 addLast()原理

```java
@Override
public final ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler) {
    AbstractChannelHandlerContext newCtx = newContext(group, name, handler);

    // 将新HandlerContext插入到tail之前
    callHandlerAdded0(newCtx);

    return this;
}
```

**添加Handler后:**
```
Head ──▶ HttpServerCodec ──▶ HttpObjectAggregator ──▶ MyHandler ──▶ Tail
```

### 4.3 事件传播

#### 入站事件(channelRead)
```java
// DefaultChannelPipeline.java
@Override
public void fireChannelRead(Object msg) {
    // 从head之后开始传播
    AbstractChannelHandlerContext.invokeChannelRead(head, msg);
}

// 找到下一个InboundHandler
static void invokeChannelRead(AbstractChannelHandlerContext next, Object msg) {
    ChannelHandler handler = next.handler();
    handler.channelRead(next, msg);  // 调用Handler的channelRead()
}
```

**传播链路:**
```
msg到达
    │
    ▼
HeadContext.fireChannelRead(msg)
    │
    ▼
HttpServerCodec.channelRead() ──▶ 解码
    │
    ▼
HttpObjectAggregator.channelRead() ──▶ 聚合
    │
    ▼
MyHandler.channelRead() ──▶ 业务逻辑
```

#### 出站事件(write)
```java
// DefaultChannelPipeline.java
@Override
public ChannelFuture write(Object msg) {
    // 从tail之前开始传播(倒序)
    return write(msg, newPromise());
}

// 找到前一个OutboundHandler
private void write(Object msg, ChannelPromise promise) {
    AbstractChannelHandlerContext.invokeWrite(tail.prev, msg, promise);
}
```

**传播链路:**
```
ctx.write("response")
    │
    ▼
TailContext.prev
    │
    ▼
MyHandler.write() ──▶ (如果是Outbound)
    │
    ▼
HttpObjectAggregator.write() ──▶ (如果是Outbound)
    │
    ▼
HttpServerCodec.write() ──▶ 编码
    │
    ▼
HeadContext.write() ──▶ 写入Socket
```

---

## 五、三种服务器的Pipeline对比

### 5.1 TCP服务器

```java
@Override
protected void initChannel(SocketChannel ch) {
    ch.pipeline().addLast(new TcpServerHandler());
}
```

**Pipeline结构:**
```
Head ──▶ TcpServerHandler ──▶ Tail
         (Inbound)
```

**特点:**
- **最简单的Pipeline**
- **直接处理原始TCP字节流**
- **适用于自定义协议**

---

### 5.2 HTTP服务器

```java
@Override
protected void initChannel(SocketChannel ch) {
    ChannelPipeline pipeline = ch.pipeline();

    // HTTP编解码器
    pipeline.addLast(new HttpServerCodec());

    // HTTP消息聚合器
    pipeline.addLast(new HttpObjectAggregator(65536));

    // 自定义业务处理器
    pipeline.addLast(new HttpServerHandler());
}
```

**Pipeline结构:**
```
Head ──▶ HttpServerCodec ──▶ HttpObjectAggregator ──▶ HttpServerHandler ──▶ Tail
         (Inbound/Outbound)      (Inbound)                (Inbound)
```

**Handler详解:**

##### 1) HttpServerCodec
**职责:** HTTP请求/响应编解码

**内部结构:**
```
HttpServerCodec = {
    HttpRequestDecoder,   // 解码HTTP请求
    HttpResponseEncoder,  // 编码HTTP响应
    HttpObjectAggregator  // 聚合HTTP消息
}
```

**处理流程:**
```
TCP字节流
    │
    ▼
HttpRequestDecoder.decode()
    │
    ▼
HttpRequest对象
    │
    ▼
HttpObjectAggregator.aggregate()
    │
    ▼
FullHttpRequest对象 (完整的HTTP请求)
    │
    ▼
HttpServerHandler.channelRead(FullHttpRequest)
```

##### 2) HttpObjectAggregator
**职责:** 将HTTP消息的多个部分聚合为一个完整的`FullHttpRequest`

**为什么需要聚合?**
```
HTTP请求可能分多次到达:
1. HttpRequest (请求行 + 请求头)
2. HttpContent (请求体分块1)
3. HttpContent (请求体分块2)
4. LastHttpContent (结束标记)

聚合后变为:
FullHttpRequest (包含所有部分)
```

---

### 5.3 WebSocket服务器

```java
@Override
protected void initChannel(SocketChannel ch) {
    ChannelPipeline pipeline = ch.pipeline();

    // HTTP编解码器 (WebSocket握手需要HTTP)
    pipeline.addLast(new HttpServerCodec());

    // HTTP消息聚合器
    pipeline.addLast(new HttpObjectAggregator(65536));

    // 支持大文件传输
    pipeline.addLast(new ChunkedWriteHandler());

    // WebSocket协议处理器 (握手、帧处理)
    pipeline.addLast(new WebSocketServerProtocolHandler(path));

    // 自定义业务处理器
    pipeline.addLast(new WebSocketServerHandler());
}
```

**Pipeline结构:**
```
Head ──▶ HttpServerCodec ──▶ HttpObjectAggregator ──▶ ChunkedWriteHandler ──▶ WebSocketServerProtocolHandler ──▶ WebSocketServerHandler ──▶ Tail
```

**Handler详解:**

##### 1) HttpServerCodec
处理WebSocket握手阶段的HTTP请求

##### 2) HttpObjectAggregator
聚合握手请求

##### 3) ChunkedWriteHandler
支持大文件分块传输

##### 4) WebSocketServerProtocolHandler
**核心职责:**
- **处理WebSocket握手**
- **升级HTTP连接为WebSocket连接**
- **处理WebSocket帧(文本/二进制/关闭/心跳等)**

**握手流程:**
```
客户端发送HTTP握手请求:
    GET /ws HTTP/1.1
    Upgrade: websocket
    Connection: Upgrade
    Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==

    │
    ▼
WebSocketServerProtocolHandler处理握手
    │
    ▼
验证Sec-WebSocket-Key
    │
    ▼
计算Sec-WebSocket-Accept
    │
    ▼
返回HTTP 101响应:
    HTTP/1.1 101 Switching Protocols
    Upgrade: websocket
    Connection: Upgrade
    Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=

    │
    ▼
连接升级为WebSocket,后续使用WebSocket帧通信
```

**帧处理:**
```
WebSocket帧类型:
- TextFrame (文本帧)
- BinaryFrame (二进制帧)
- PingFrame (心跳帧)
- PongFrame (心跳响应)
- CloseFrame (关闭帧)

WebSocketServerProtocolHandler根据帧类型分发:
    if (frame instanceof TextWebSocketFrame) {
        // 触发下一个Handler的channelRead()
    }
```

##### 5) WebSocketServerHandler
处理WebSocket消息的业务逻辑

---

## 六、实战代码分析

### 6.1 TcpServer源码分析

#### 关键代码
```java
@PostConstruct
public void start() {
    // 步骤1: 创建EventLoopGroup
    bossGroup = new NioEventLoopGroup(1);    // 单线程接收连接
    workerGroup = new NioEventLoopGroup();   // CPU*2个线程处理I/O

    // 步骤2: 配置ServerBootstrap
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(bossGroup, workerGroup)           // 设置线程组
            .channel(NioServerSocketChannel.class)   // NIO Channel
            .option(ChannelOption.SO_BACKLOG, 128)   // 连接队列大小
            .childOption(ChannelOption.SO_KEEPALIVE, true) // 保持连接
            .childHandler(new ChannelInitializer<SocketChannel>() { // 配置Pipeline
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new TcpServerHandler());
                }
            });

    // 步骤3: 绑定端口并启动
    ChannelFuture future = bootstrap.bind(port).sync();
    serverChannel = future.channel();
}
```

#### 执行流程
```
1. @PostConstruct触发start()
    │
    ▼
2. 创建EventLoopGroup
    │
    ▼
3. 配置ServerBootstrap
    │
    ▼
4. bind(port).sync() 阻塞等待启动完成
    │
    ▼
5. 服务器启动成功,监听9001端口
    │
    ▼
6. bossGroup线程循环执行:
        while (true) {
            // 接收客户端连接
            SocketChannel ch = serverSocket.accept();

            // 注册到workerGroup的某个EventLoop
            workerGroup.next().register(ch);

            // 执行initChannel(),初始化Pipeline
            pipeline.addLast(new TcpServerHandler());
        }
    │
    ▼
7. workerGroup线程循环执行:
        while (true) {
            // 检测到I/O事件
            if (ch.isReadable()) {
                // 读取数据
                ByteBuf buf = ch.read();

                // 触发pipeline.fireChannelRead(buf)
                // 从head开始传播到TcpServerHandler
            }
        }
```

---

### 6.2 TcpServerHandler源码分析

#### 关键代码
```java
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf buf = (ByteBuf) msg;
    try {
        // 读取数据
        String message = buf.toString(StandardCharsets.UTF_8);
        log.info("TCP服务器收到消息: {}", message);

        // 将消息原样返回给客户端（Echo服务）
        ctx.write(buf);
    } finally {
        // 释放缓冲区
        buf.release();
    }
}

@Override
public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();  // 刷新数据到客户端
}
```

#### 执行流程
```
1. 客户端发送 "Hello"
    │
    ▼
2. bossGroup接收连接,分发给workerGroup
    │
    ▼
3. workerGroup的EventLoop检测到读事件
    │
    ▼
4. 从Socket读取字节流到ByteBuf
    │
    ▼
5. pipeline.fireChannelRead(ByteBuf)
    │
    ▼
6. HeadContext.channelRead() ──▶ 跳过
    │
    ▼
7. TcpServerHandler.channelRead(ByteBuf)
        │
        ▼
    buf.toString(UTF-8) ──▶ "Hello"
        │
        ▼
    ctx.write(buf) ──▶ 写入缓冲区,但不发送
    │
    ▼
8. channelReadComplete()触发
    │
    ▼
9. TcpServerHandler.channelReadComplete()
        │
        ▼
    ctx.flush() ──▶ 刷新缓冲区,发送"Hello"到客户端
```

---

### 6.3 WebSocketServer源码分析

#### Pipeline配置详解
```java
.childHandler(new ChannelInitializer<SocketChannel>() {
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // 阶段1: HTTP握手阶段
        pipeline.addLast(new HttpServerCodec());           // 解码握手请求
        pipeline.addLast(new HttpObjectAggregator(65536)); // 聚合握手消息
        pipeline.addLast(new ChunkedWriteHandler());       // 支持大文件

        // 阶段2: WebSocket协议处理
        pipeline.addLast(new WebSocketServerProtocolHandler(path));

        // 阶段3: 业务逻辑
        pipeline.addLast(new WebSocketServerHandler());
    }
})
```

#### 连接生命周期
```
1. 客户端发起HTTP握手请求
        GET /ws HTTP/1.1
        Upgrade: websocket
        Sec-WebSocket-Key: xxx
    │
    ▼
2. HttpServerCodec解码HttpRequest
    │
    ▼
3. HttpObjectAggregator聚合为FullHttpRequest
    │
    ▼
4. WebSocketServerProtocolHandler处理握手
    - 验证Sec-WebSocket-Key
    - 计算Sec-WebSocket-Accept
    - 返回HTTP 101响应
    │
    ▼
5. 连接升级为WebSocket
    - 从Pipeline中移除HttpServerCodec
    - 从Pipeline中移除HttpObjectAggregator
    - 添加WebSocketFrame编解码器
    │
    ▼
6. 客户端发送WebSocket帧
    │
    ▼
7. WebSocketServerProtocolHandler解码帧
    - TextWebSocketFrame
    - BinaryWebSocketFrame
    - PingWebSocketFrame
    - CloseWebSocketFrame
    │
    ▼
8. WebSocketServerHandler处理业务逻辑
    if (frame instanceof TextWebSocketFrame) {
        String text = ((TextWebSocketFrame) frame).text();
        // 处理文本消息
    }
```

---

## 七、常见问题FAQ

### Q1: bossGroup为什么设置为1个线程?

**答:** 因为接收连接的操作非常轻量,只需要:
1. 接受TCP连接(accept)
2. 将新连接注册到workerGroup

如果设置多个线程,反而会造成线程竞争,降低性能。

### Q2: workerGroup默认线程数为什么是CPU核心数×2?

**答:** Netty的EventLoop既要处理I/O事件,又要执行Handler业务逻辑。
设置为CPU核心数×2可以充分利用CPU时间片:
- 当线程阻塞在I/O等待时,另一个线程可以执行业务逻辑
- 平衡I/O密集型和CPU密集型任务

### Q3: 为什么要用ChannelInitializer初始化Pipeline?

**答:** 因为**每个客户端连接都需要独立的Pipeline**,不能共享。

```java
// 错误示例: 所有连接共享同一个Handler
.channelHandler(new TcpServerHandler()) // ❌ 不安全

// 正确示例: 每个连接创建新的Handler
.childHandler(new ChannelInitializer<SocketChannel>() {
    @Override
    protected void initChannel(SocketChannel ch) {
        // 每次连接建立时,都会执行此方法,创建新的Pipeline
        ch.pipeline().addLast(new TcpServerHandler()); // ✅ 线程安全
    }
})
```

### Q4: ByteBuf为什么要手动release?

**答:** Netty采用**引用计数**管理内存,需要手动释放:

```java
ByteBuf buf = (ByteBuf) msg;
try {
    // 使用buf
} finally {
    buf.release();  // 引用计数-1,为0时回收内存
}
```

**如果不释放会导致内存泄漏!**

### Q5: ctx.write()和channel().write()有什么区别?

**答:**
- `ctx.write(msg)`: 从**当前Handler**开始传播
- `channel().write(msg)`: 从**Pipeline尾部**开始传播

```java
// Pipeline: Head ──▶ Handler1 ──▶ Handler2 ──▶ Handler3 ──▶ Tail

// 在Handler2中:
ctx.write(msg);      // 传播: Handler2 ──▶ Handler3 ──▶ Tail
channel().write(msg); // 传播: Tail ──▶ Handler3 ──▶ Handler2 ──▶ Handler1 ──▶ Head
```

### Q6: EventLoop与Channel是一对一还是一对多?

**答:** **一对多**。一个EventLoop可以处理多个Channel,但一个Channel只能绑定到一个EventLoop。

```
EventLoop1: Channel1, Channel2, Channel3
EventLoop2: Channel4, Channel5, Channel6
```

**好处:**
- 减少线程数量,降低上下文切换开销
- 保证Channel的线程安全(所有操作在同一个EventLoop线程中执行)

### Q7: 如何理解ChannelFuture?

**答:** Netty所有I/O操作都是**异步**的,返回Future表示操作的结果:

```java
// 同步方式: 阻塞等待结果
ChannelFuture future = channel.writeAndFlush(msg);
future.sync(); // 阻塞直到写入完成

// 异步方式: 回调通知
channel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
    @Override
    public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {
            System.out.println("写入成功");
        } else {
            System.out.println("写入失败: " + future.cause());
        }
    }
});
```

---

## 八、总结

### Netty核心组件关系图

```
┌─────────────────────────────────────────────────────────┐
│                    ServerBootstrap                      │
│  - 配置服务器参数                                        │
│  - 设置EventLoopGroup                                   │
│  - 注册ChannelHandler                                   │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
┌───────▼────────┐       ┌───────▼────────┐
│  bossGroup      │      │  workerGroup   │
│  (1线程)        │      │  (CPU*2线程)   │
└───────┬────────┘       └───────┬────────┘
        │                         │
        └────────────┬────────────┘
                     │
            ┌────────▼────────┐
            │  ServerChannel  │
            │  (监听端口)      │
            └────────┬────────┘
                     │
        ┌────────────┴──────────┐
        │                       │
┌───────▼────────┐      ┌───────▼─────────┐
│  SocketChannel1│      │  SocketChannel2 │
│  └─EventLoop1  │      │  └─EventLoop2   │
│  └─Pipeline1   │      │  └─Pipeline2    │
└────────────────┘      └─────────────────┘
```

### 学习建议

1. **先理解Reactor线程模型**
   - EventLoopGroup的作用
   - bossGroup与workerGroup的分工

2. **深入Pipeline机制**
   - Handler的调用顺序
   - 入站/出站事件传播

3. **实践三种服务器**
   - TCP: 理解原始Socket通信
   - HTTP: 理解协议编解码
   - WebSocket: 理解协议升级

4. **阅读源码**
   - `NioEventLoop`: 事件循环
   - `DefaultChannelPipeline`: Pipeline实现
   - `ServerBootstrap`: 启动流程

---

**推荐资源:**
- Netty官方文档: https://netty.io/wiki/user-guide.html
- Netty源码: https://github.com/netty/netty
- 《Netty实战》书籍

---

**作者:** clazs
**日期:** 2026-01-11

