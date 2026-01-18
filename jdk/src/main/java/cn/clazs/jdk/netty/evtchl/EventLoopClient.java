package cn.clazs.jdk.netty.evtchl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;

@Slf4j
public class EventLoopClient {
    public static void main(String[] args) throws InterruptedException, IOException {
        // ChannelFuture本质是Future的子接口，代表一个异步任务，及其结果（通过channel获取结果的连接）
        ChannelFuture channelFuture = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                // connect是异步非阻塞方法，交给NIO线程执行连接，调用完毕不代表真正连接上了
                .connect(new InetSocketAddress("localhost", 7070));


        // 方法一：主线程阻塞等待future获取结果
        /* log.info("即将建立连接...");
        Channel channel = channelFuture
                .sync() // 所以需要主动等待（阻塞）连接完成，才能后续对channel执行读写
                .channel(); // 此时返回Channel才是完整、正确的连接对象
        log.info("连接已建立... channel: {}", channel);
        channel.writeAndFlush("Client 发送一条消息！"); */

        // 方法二：为future添加完成连接建立的监听器（异步回调机制）
        log.info("即将建立连接...");
        channelFuture.addListener(new ChannelFutureListener() {
            // 连接完成的回调（这块和 Kafka 发送完毕消息后，对返回的future添加Callback的机制一样！）
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                Channel channel = channelFuture.channel();
                log.info("连接已建立... channel: {}", channel);
                channel.writeAndFlush("Client 发送一条消息！");
            }
        });

        System.in.read();
    }
}
