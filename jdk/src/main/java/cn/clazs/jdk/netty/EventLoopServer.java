package cn.clazs.jdk.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

@Slf4j
public class EventLoopServer {
    public static void main(String[] args) {
        // 普通的时间循环，处理除了IO事件之外的事件
        EventLoopGroup customGroup = new DefaultEventLoopGroup();

        ChannelInitializer<NioSocketChannel> childHandler = new ChannelInitializer<NioSocketChannel>() {
            // 一个新的连接被接受时，Netty会调用这个方法来初始化这个连接的通道
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                // 获取通道的Pipeline（处理链），并添加一个自定义的入站处理器
                ch.pipeline().addLast("handler-1", new ChannelInboundHandlerAdapter() {
                    // 当通道中有数据可读时，会触发此方法
                    @Override // 不加解码器接受到的是 ByteBuf 类型
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        ByteBuf buf = (ByteBuf) msg;
                        log.info("服务器接收: {}", buf.toString(Charset.defaultCharset()));
                        ctx.fireChannelRead(msg);
                    }
                });

                // 如果是一些耗时的任务，就可以单独交给DefaultEventLoopGroup执行
                // 而不用继续占据Nio的Worker的线程（类似CompletableFuture可以自定义线程池）
                ch.pipeline().addLast(customGroup, "handler-2", new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    }
                });
            }
        };

        new ServerBootstrap()
                // 第一个是Boss的时间循环组， 第二个是Worker的时间循环组
                // Boss只负责ServerSocketChannel上的Accept事件，Worker只负责SocketChannel上的读写事件
                // Boss默认只有一个，因为只有一个ServerSocketChannel
                .group(new NioEventLoopGroup(), new NioEventLoopGroup(2))
                // Worker线程为2，代表有两个Worker的事件循环
                .channel(NioServerSocketChannel.class)
                .childHandler(childHandler)
                .bind(new InetSocketAddress(7070));
    }
}