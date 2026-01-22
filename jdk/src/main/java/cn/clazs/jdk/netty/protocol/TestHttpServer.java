package cn.clazs.jdk.netty.protocol;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 一个简单的HTTP服务器，接受、处理浏览器等的HTTP请求，返回响应
 */
@Slf4j
public class TestHttpServer {
    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup work = new NioEventLoopGroup();
        serverBootstrap
                .group(boss, work)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler());
                        /*
                         * 包含两个
                         * - HttpRequestDecoder：入站解码器，把 ByteBuf 解析成 HttpRequest 对象
                         * - HttpResponseEncoder：出站编码器，把 HttpResponse 对象编码成 ByteBuf
                         */
                        ch.pipeline().addLast(new HttpServerCodec());

                        // 下方注释起来的handler的平替，泛型代表处理的msg类型
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<HttpRequest>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
                                log.debug("请求URI: {}", req.uri());
                                HttpHeaders headers = req.headers(); // 键值对的集合
                                String userAgent = headers.get("User-Agent");
                                log.debug("请求头 User-Agent = {}", userAgent);

                                // 返回响应（HttpServerCodec中包含的出站处理器会将HTTP响应转为ByteBuf）
                                DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                                        req.protocolVersion(), HttpResponseStatus.OK);
                                ByteBuf buf = response.content();
                                buf.writeBytes("<h2 style=\"color:red\">服务器响应.</h2>".getBytes());

                                // 【关键修复】设置 Content-Length
                                // 告诉浏览器：我这就发这么多字节，收完就不许再等了！
                                response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

                                // 【建议添加】设置 Content-Type，防止中文乱码
                                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

                                // 将response写出到channel
                                ctx.writeAndFlush(response);
                            }
                        });

                        /* ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                log.debug("msg: {}", msg.getClass());
                                // 一次请求会发送触发两次入站，且解码后的msg类型不同！分别如下两种
                                if (msg instanceof HttpRequest) {
                                    log.debug("处理请求行、请求头");
                                } else if (msg instanceof HttpContent) {
                                    log.debug("处理请求体");
                                }
                            }
                        }); */
                    }
                })
                .bind(7070);
    }
}
