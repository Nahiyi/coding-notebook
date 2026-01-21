package cn.clazs.jdk.netty.echo.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Scanner;

@Slf4j
public class DemoClient {
    public static void main(String[] args) {
        ChannelFuture channelFuture = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                log.info("[Echo]: {}", buf.toString(Charset.defaultCharset()));
                                // super.channelRead(ctx, msg);
                            }
                        });
                    }
                })
                .connect(new InetSocketAddress("localhost", 7070));

        log.debug("等待建立连接中...");
        channelFuture.addListener((ChannelFutureListener) future -> {
            log.debug("连接已建立.");
            Channel channel = future.channel();
            Scanner sc = new Scanner(System.in);
            new Thread(() -> {
                while (true) {
                    String line = sc.nextLine();
                    if ("quit".equals(line)) {
                        ChannelFuture closedFuture = channel.close();
                        try {
                            closedFuture.sync();
                            log.debug("连接已关闭!");
                            break;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    log.debug("客户端发送: {}", line);
                    ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
                    buf.writeBytes(line.getBytes());
                    channel.writeAndFlush(buf);
                }
            }).start();
        });
    }
}
