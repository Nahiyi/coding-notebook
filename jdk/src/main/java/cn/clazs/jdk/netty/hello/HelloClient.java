package cn.clazs.jdk.netty.hello;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class HelloClient {
    public static void main(String[] args) throws InterruptedException {
        // 1. 创建客户端启动引导类
        // 对应服务端的 ServerBootstrap，这是专门用于客户端的引导类
        new Bootstrap()

                // 2. 配置线程组 -> 对应 NIO 概念：Selector
                // 客户端通常不需要像服务端那样处理成千上万的连接，所以只需要一个 EventLoopGroup
                // 这个 Group 里的线程既负责发起连接（类似 NIO 的 connect），也负责连接建立后的 IO 读写
                // 它内部封装了 Selector，用于监听连接是否建立成功（OP_CONNECT）以及是否有数据可读（OP_READ）
                .group(new NioEventLoopGroup())

                // 3. 指定 Channel 类型 -> 对应 NIO 概念：SocketChannel
                // NioSocketChannel 对应 Java NIO 中的 java.nio.channels.SocketChannel
                // 它代表客户端与服务端建立的一条非阻塞 TCP 连接通道
                .channel(NioSocketChannel.class)

                // 4. 配置处理器
                // 客户端不需要 childHandler，因为客户端没有“子连接”的概念，它本身就是唯一的连接
                // 这里的 handler 会在连接建立成功后，初始化 Channel 的 Pipeline（流水线）
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        // 5. 添加编码器
                        // 服务端用的是 StringDecoder（解码），客户端这里用的是 StringEncoder（编码）
                        // 对应 NIO 概念：Buffer (向 Buffer 中写入字节)
                        // 当我们调用 writeAndFlush 发送字符串时，StringEncoder 会自动把 String 转换成 Netty 的 ByteBuf
                        // 这就相当于我们在 NIO 中手动把字符串转成字节放入 ByteBuffer 的过程
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })

                // 6. 发起连接 -> 对应 NIO 概念：SocketChannel.connect()
                // 这是一个异步操作。它会立即返回一个 ChannelFuture 对象
                // 在底层，它向 Selector 注册了 OP_CONNECT 事件，告诉操作系统：“我要连 localhost:7070，连好了告诉我”
                .connect(new InetSocketAddress("localhost", 7070))

                // 7. 同步等待连接建立 -> 对应 NIO 概念：阻塞直到连接就绪
                // 因为 connect 是异步的，如果不加 sync()，代码可能会在连接还没建立好时就往下跑，导致报错
                // sync() 的作用是阻塞当前 main 线程，直到连接真正建立成功（类似于 Future.get()）
                // 只有连接建立成功，后续的 channel() 操作才有意义
                .sync()

                // 8. 获取连接对象
                // 拿到代表当前连接的 Channel 对象
                .channel()

                // 9. 发送数据并刷新 -> 对应 NIO 概念：Channel.write() + flush()
                // writeAndFlush 是 Netty 的核心方法：
                // 1. write: 将数据写入到 Channel 的缓冲区（经过 StringEncoder 编码成 ByteBuf）
                // 2. flush: 调用底层 Socket 的发送操作，将缓冲区的数据真正通过网络发出去
                // 在 NIO 中，这相当于 byteBuffer.put() 和 socketChannel.write(byteBuffer) 的组合
                .writeAndFlush("Hello, World");

        log.debug("Done...");
    }
}
