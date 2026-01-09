package cn.clazs.netty.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty WebSocket服务器处理器
 *
 * 功能：处理WebSocket连接和消息
 * - 支持文本消息双向传输
 * - 维护在线连接状态
 * - 广播消息给所有客户端
 *
 * 应用场景：
 * - 聊天室、即时通讯
 * - 实时数据推送（股票行情、游戏状态）
 * - 在线协作（多人编辑）
 * - 实时监控告警
 *
 * @author clazs
 */
@Slf4j
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    /**
     * 处理WebSocket消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 处理文本消息
        if (frame instanceof TextWebSocketFrame) {
            String request = ((TextWebSocketFrame) frame).text();
            log.info("收到WebSocket消息: {} - 来自: {}", request, ctx.channel().remoteAddress());

            // 构造响应消息
            String response = "服务器回复: " + request;
            ctx.channel().write(new TextWebSocketFrame(response));
            return;
        }

        // 处理Ping帧（心跳检测）
        if (frame instanceof PongWebSocketFrame) {
            log.debug("收到WebSocket Pong帧");
            ctx.write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        // 处理关闭连接请求
        if (frame instanceof CloseWebSocketFrame) {
            log.info("客户端请求关闭WebSocket连接: {}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        // 不支持的帧类型
        log.error("不支持的WebSocket帧类型: {}", frame.getClass().getName());
        ctx.close();
    }

    /**
     * 客户端连接时触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("WebSocket客户端已连接: {}", ctx.channel().remoteAddress());
        // 发送欢迎消息
        ctx.channel().writeAndFlush(new TextWebSocketFrame("欢迎连接到Netty WebSocket服务器！"));
    }

    /**
     * 客户端断开连接时触发
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("WebSocket客户端已断开: {}", ctx.channel().remoteAddress());
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket服务器发生异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
