package cn.clazs.jdk.netty.hello;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelloServer {
    public static void main(String[] args) {
        // 1. 创建服务器启动引导类 (ServerBootstrap)
        // 它是 Netty 服务端的启动辅助类，用于组装各种组件（线程组、Channel、处理器等）
        new ServerBootstrap()

                // 2. 配置线程组 (EventLoopGroup) -> 对应 NIO 概念：Selector 与 线程模型
                // 这里只传入了一个 NioEventLoopGroup
                // 在 Netty 中，通常将线程组分为 Boss 和 Worker：
                // - Boss Group (父组): 通常只包含 1 个线程。它的作用是类似 NIO 中的 "接收线程"
                //   它负责监听服务器端口，处理客户端的连接请求
                // - Worker Group (子组): 包含多个线程。它们才是真正的 "IO 线程"
                //   一旦 Boss 接受了连接，会将对应的 SocketChannel 注册到 Worker 的 Selector 上
                //   由 Worker 线程负责后续的 IO 读写操作
                // 注意：这里为了demo，只传了一个 Group，这意味着该 Group 既充当 Boss 又充当 Worker
                // 即对应的selector既响应连接事件，也响应读写事件
                .group(new NioEventLoopGroup())

                // 3. 指定 Channel 的类型 -> 对应 NIO 概念：ServerSocketChannel
                // NioServerSocketChannel 对应 Java NIO 中的 java.nio.channels.ServerSocketChannel
                // 它是基于 Java NIO Selector 实现的，能够支持非阻塞模式
                .channel(NioServerSocketChannel.class)

                // 4. 配置子处理器 (childHandler) -> 对应 NIO 概念：处理 Buffer 中的数据
                // 这里的 "child" 指的是被 Boss 接受之后，创建出来的连接
                // 每当有一个新的连接（SocketChannel）被接受时，都会初始化一次
                // ChannelInitializer 是一个特殊的处理器，它的作用是在连接建立后，
                // 向 Pipeline（责任链、流水线）中添加其他的业务处理器
                .childHandler(
                        new ChannelInitializer<NioSocketChannel>() {
                            @Override
                            protected void initChannel(NioSocketChannel ch) throws Exception {
                                // ch 代表的是与客户端建立的具体连接通道，对应 NIO 的 SocketChannel
                                // 5. 添加解码器
                                // Netty 是基于 ByteBuf（字节流）传输数据的，而网络传输的都是字节
                                // 我们的业务逻辑通常希望直接处理字符串
                                // StringDecoder 的作用是：将收到的 ByteBuf 解码成 String
                                // 它会帮我们处理 NIO 中繁琐的 ByteBuffer 读操作（如 flip、limit 等）
                                ch.pipeline().addLast(new StringDecoder());

                                // 6. 添加自定义业务处理器
                                // ChannelInboundHandlerAdapter: 处理入站数据（即从客户端流向服务器的数据）
                                // channelRead 方法会在数据读到并解码完成后被调用
                                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        // 这里的 msg 已经是经过 StringDecoder 解码后的 String 对象了
                                        // 相当于 NIO 中我们手动从 channel.read(buffer) 并转换成字符串后的结果
                                        log.debug("服务器收到: {}", msg);
                                    }
                                });
                            }
                        }
                )
                // 7. 绑定端口并启动服务器
                // 这是一个异步操作。bind 内部会向 Boss Group 提交一个注册任务
                // 让 Boss 线程去执行真正的 bind 操作，并开始监听 OP_ACCEPT 事件
                .bind(7070);
    }
}

