package cn.clazs.netty.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Netty TCP服务器
 *
 * 功能：实现基础的TCP Socket通信，适用于：
 * - 自定义协议通信
 * - RPC框架底层通信
 * - 游戏服务器
 * - 物联网设备通信
 *
 * @author clazs
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "netty.tcp.enabled", havingValue = "true", matchIfMissing = true)
public class TcpServer {

    @Value("${netty.tcp.port:9001}")
    private int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * Spring Boot启动后自动启动TCP服务器
     */
    @PostConstruct
    public void start() {
        // bossGroup: 接收客户端连接
        // workerGroup: 处理客户端连接的I/O操作
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)  // 连接队列大小
                    .childOption(ChannelOption.SO_KEEPALIVE, true)  // 保持连接
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new TcpServerHandler());
                        }
                    });

            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            log.info("========================================");
            log.info("Netty TCP服务器启动成功！");
            log.info("监听端口: {}", port);
            log.info("测试命令: telnet localhost {}", port);
            log.info("测试命令: nc localhost {}", port);
            log.info("========================================");

        } catch (Exception e) {
            log.error("TCP服务器启动失败", e);
            shutdown();
        }
    }

    /**
     * Spring Boot关闭时自动关闭TCP服务器
     */
    @PreDestroy
    public void shutdown() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        log.info("TCP服务器已关闭");
    }
}
