# Java NIO中SocketChannel和ServerSocketChannel的区别

## ServerSocketChannel

**作用**：用于监听和接受客户端连接请求（服务器端使用）

**主要功能**：
1. 绑定到特定端口监听连接
2. 接受客户端连接请求
3. 创建新的SocketChannel实例

**示例代码**：
```java
// 创建ServerSocketChannel
ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.bind(new InetSocketAddress(8080));
serverChannel.configureBlocking(false); // 设置非阻塞模式

// 接受连接
SocketChannel clientChannel = serverChannel.accept();
```

## SocketChannel

**作用**：用于网络I/O操作（读写数据）

**主要功能**：
1. 与远程服务器建立连接（客户端）
2. 读取数据
3. 写入数据
4. 进行实际的数据传输

**示例代码**：
```java
// 客户端连接
SocketChannel socketChannel = SocketChannel.open();
socketChannel.connect(new InetSocketAddress("localhost", 8080));
socketChannel.configureBlocking(false);

// 读写数据
ByteBuffer buffer = ByteBuffer.allocate(1024);
int bytesRead = socketChannel.read(buffer);
int bytesWritten = socketChannel.write(buffer);
```

## 主要区别对比

| 特性 | ServerSocketChannel | SocketChannel |
|------|---------------------|---------------|
| **角色** | 服务器监听端 | 客户端/连接端 |
| **主要方法** | accept() | connect(), read(), write() |
| **绑定端口** | 是 | 否 |
| **接受连接** | 是 | 否 |
| **发起连接** | 否 | 是 |
| **数据传输** | 间接（通过accept返回的SocketChannel） | 直接 |

## 工作流程示例

```java
// 服务器端
ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.bind(new InetSocketAddress(8080));

// 接受客户端连接
SocketChannel clientChannel = serverChannel.accept();
if (clientChannel != null) {
    // 通过clientChannel进行数据读写
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    clientChannel.read(buffer);
}

// 客户端
SocketChannel socketChannel = SocketChannel.open();
socketChannel.connect(new InetSocketAddress("localhost", 8080));

// 直接进行数据读写
ByteBuffer buffer = ByteBuffer.allocate(1024);
socketChannel.write(buffer);
```

简单来说：
- **ServerSocketChannel** = 电话总机（接听电话）
- **SocketChannel** = 电话线路（通话传输）

ServerSocketChannel负责接听连接请求，而SocketChannel负责实际的数据通信。