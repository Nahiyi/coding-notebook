package cn.clazs.netty.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Netty WebSocket服务器
 *
 * 功能：实现WebSocket协议的实时双向通信
 *
 * 核心Handler说明：
 * - HttpServerCodec: HTTP编解码器（WebSocket基于HTTP）
 * - HttpObjectAggregator: HTTP消息聚合器（将HttpRequest和HttpContent聚合为FullHttpRequest）
 * - ChunkedWriteHandler: 支持大文件传输
 * - WebSocketServerProtocolHandler: WebSocket协议处理器（处理握手、帧处理等）
 * - WebSocketServerHandler: 自定义业务处理器
 *
 * @author clazs
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "netty.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketServer {

    @Value("${netty.websocket.port:9002}")
    private int port;

    @Value("${netty.websocket.path:/ws}")
    private String path;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * Spring Boot启动后自动启动WebSocket服务器
     */
    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // HTTP编解码器
                            pipeline.addLast(new HttpServerCodec());

                            // HTTP消息聚合器（将多个HttpRequest/HttpContent聚合为FullHttpRequest）
                            pipeline.addLast(new HttpObjectAggregator(65536));

                            // 支持大文件传输
                            pipeline.addLast(new ChunkedWriteHandler());

                            // WebSocket协议处理器（处理握手、帧处理等）
                            pipeline.addLast(new WebSocketServerProtocolHandler(path));

                            // 自定义业务处理器
                            pipeline.addLast(new WebSocketServerHandler());
                        }
                    });

            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            log.info("========================================");
            log.info("Netty WebSocket服务器启动成功！");
            log.info("监听端口: {}", port);
            log.info("WebSocket路径: ws://localhost:{}{}", port, path);
            log.info("测试工具: http://www.websocket-test.com/");
            log.info("测试代码: const ws = new WebSocket('ws://localhost:{}{}');", port, path);
            log.info("========================================");

        } catch (Exception e) {
            log.error("WebSocket服务器启动失败", e);
            shutdown();
        }
    }

    /**
     * Spring Boot关闭时自动关闭WebSocket服务器
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
        log.info("WebSocket服务器已关闭");
    }
}
