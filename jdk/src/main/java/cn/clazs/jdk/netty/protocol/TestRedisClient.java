package cn.clazs.jdk.netty.protocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

/**
 * 使用Redis的协议制作建议Redis客户端，实现Redis简单命令
 * 注：Redis的RESP协议，要求间隔符是“\r\n”，也就是对应ASCII的13,10，对应十六进制的d,a
 */
@Slf4j
public class TestRedisClient {
    public static void main(String[] args) throws InterruptedException {
        ChannelFuture channelFuture = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler());
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                // 连接建立触发发送消息：set name jack
                                StringBuilder sb = new StringBuilder()
                                        .append("*3\r\n") // 表示命令拆分数组有3个单位
                                        .append("$3\r\n") // $符号+单个命令长度
                                        .append("set\r\n") // 接实际命令
                                        .append("$4\r\n")
                                        .append("name\r\n")
                                        .append("$4\r\n")
                                        .append("jack\r\n"); // ⚠️ 最后也要加 \r\n
                                ByteBuf buf = ctx.alloc().buffer();
                                buf.writeBytes(sb.toString().getBytes());
                                log.debug("命令发送: {}", sb);
                                ctx.writeAndFlush(buf);
                            }
                        });
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                // 接受Redis响应
                                ByteBuf resp = (ByteBuf) msg;
                                log.debug("Redis响应: {}", resp.toString(Charset.defaultCharset()));
                            }
                        });
                    }
                })
                .connect("localhost", 6379);
        log.debug("连接Redis服务器中...");
        channelFuture.sync();
        log.debug("连接成功");
    }
}
