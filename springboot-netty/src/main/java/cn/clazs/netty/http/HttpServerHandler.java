package cn.clazs.netty.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static javax.sound.sampled.LineEvent.Type.CLOSE;

/**
 * Netty HTTP服务器处理器
 *
 * 功能：处理HTTP请求并返回响应
 * - 支持GET/POST请求
 * - 支持Keep-Alive连接
 * - 返回JSON和HTML格式数据
 *
 * 应用场景：
 * - 替代Tomcat提供HTTP服务（高性能场景）
 * - 微服务网关（如Spring Cloud Gateway）
 * - API网关、反向代理
 * - 高并发REST API服务
 *
 * @author clazs
 */
@Slf4j
public class HttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final byte[] CONTENT = "Hello World from Netty HTTP Server!".getBytes(StandardCharsets.UTF_8);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            // 获取请求信息
            String uri = req.uri();
            String method = req.method().name();
            boolean keepAlive = HttpUtil.isKeepAlive(req);

            log.info("收到HTTP请求: {} {} - 来自: {}", method, uri, ctx.channel().remoteAddress());

            // 构造响应
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HTTP_1_1,
                    OK,
                    Unpooled.wrappedBuffer(CONTENT)
            );

            // 设置响应头
            response.headers()
                    .set(CONTENT_TYPE, "text/plain; charset=UTF-8")
                    .setInt(CONTENT_LENGTH, response.content().readableBytes());

            // 设置Keep-Alive
            if (keepAlive) {
                if (!req.protocolVersion().isKeepAliveDefault()) {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                }
            } else {
                response.headers().set(CONNECTION, CLOSE);
            }

            // 发送响应
            ctx.write(response);

            if (!keepAlive) {
                // 如果不支持Keep-Alive，发送完成后关闭连接
                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("HTTP服务器发生异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
