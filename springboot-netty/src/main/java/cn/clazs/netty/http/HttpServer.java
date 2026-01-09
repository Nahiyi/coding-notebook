package cn.clazs.netty.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Netty HTTP服务器
 *
 * 功能：实现HTTP协议服务器，可替代Tomcat提供HTTP服务
 *
 * 核心Handler说明：
 * - HttpServerCodec: HTTP编解码器（HttpRequest/HttpResponse）
 * - HttpObjectAggregator: HTTP消息聚合器（聚合为FullHttpRequest）
 * - HttpServerHandler: 自定义业务处理器
 *
 * 优势对比Tomcat：
 * - 更高的性能和吞吐量
 * - 更低的内存占用
 * - 更灵活的定制能力
 * - 支持HTTP/2、WebSocket等多种协议
 *
 * 应用场景：
 * - Spring WebFlux底层（Reactor Netty）
 * - Spring Cloud Gateway
 * - 高性能API网关
 * - 微服务架构中的HTTP服务
 *
 * @author clazs
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "netty.http.enabled", havingValue = "true", matchIfMissing = true)
public class HttpServer {

    @Value("${netty.http.port:9003}")
    private int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * Spring Boot启动后自动启动HTTP服务器
     */
    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // HTTP编解码器
                            pipeline.addLast(new HttpServerCodec());

                            // HTTP消息聚合器（将HttpRequest/HttpContent聚合为FullHttpRequest）
                            pipeline.addLast(new HttpObjectAggregator(65536));

                            // 自定义业务处理器
                            pipeline.addLast(new HttpServerHandler());
                        }
                    });

            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            log.info("========================================");
            log.info("Netty HTTP服务器启动成功！");
            log.info("监听端口: {}", port);
            log.info("访问地址: http://localhost:{}", port);
            log.info("测试命令: curl http://localhost:{}", port);
            log.info("========================================");

        } catch (Exception e) {
            log.error("HTTP服务器启动失败", e);
            shutdown();
        }
    }

    /**
     * Spring Boot关闭时自动关闭HTTP服务器
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
        log.info("HTTP服务器已关闭");
    }
}
