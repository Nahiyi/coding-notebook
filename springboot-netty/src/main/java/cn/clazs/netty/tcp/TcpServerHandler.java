package cn.clazs.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * Netty TCP服务器处理器
 *
 * 功能：实现Echo服务，将客户端发送的消息原样返回
 *
 * @author clazs
 */
@Slf4j
public class TcpServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 读取客户端发送的数据
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        try {
            // 读取数据
            String message = buf.toString(StandardCharsets.UTF_8);
            log.info("TCP服务器收到消息: {} - 来自: {}", message, ctx.channel().remoteAddress());

            // 将消息原样返回给客户端（Echo服务）
            ctx.write(buf);
        } finally {
            // 释放缓冲区
            buf.release();
        }
    }

    /**
     * 数据读取完成后刷新到客户端
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("TCP服务器发生异常: {}", cause.getMessage(), cause);
        // 关闭连接
        ctx.close();
    }

    /**
     * 客户端连接时触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("客户端已连接: {}", ctx.channel().remoteAddress());
    }

    /**
     * 客户端断开连接时触发
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("客户端已断开: {}", ctx.channel().remoteAddress());
    }
}
