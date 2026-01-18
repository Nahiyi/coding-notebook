package cn.clazs.jdk.netty.evtchl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Scanner;

/**
 * 关闭channel连接demo（还是与future打交道）
 */
@Slf4j
public class CloseChannelDemo {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        ChannelFuture channelFuture = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                .connect(new InetSocketAddress("localhost", 7070));

        channelFuture.sync();
        Channel channel = channelFuture.channel();

        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (true) {
                String line = sc.nextLine();
                if ("quit".equals(line)) {
                    channel.close(); // close是真实发起关闭，只不过是异步操作
                    break;
                }
                log.debug("发送: {}", line);
                channel.writeAndFlush(line);
            }
        }, "input-thread").start();

        // 方法一：使用sync同步方法，在main中等待关闭
        ChannelFuture closedFuture = channel.closeFuture(); // 获取一个对于连接关闭的监听future，并不发起关闭请求
        /* closedFuture.sync(); // 阻塞直到连接关闭
        log.debug("这是连接关闭后才能执行的一些操作...");
        group.shutdownGracefully(); */

        // 方法二：为future添加关闭的异步回调
        closedFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                log.debug("这是连接关闭后才能执行的一些操作...");
                group.shutdownGracefully();
            }
        });
    }
}
