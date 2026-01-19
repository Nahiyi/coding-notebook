package cn.clazs.jdk.netty.handler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

/**
 * Netty的核心：处理器
 * 基本的入站、出站处理器的知识点：执行顺序、入站/出站的数据的依次流水传输和自定义处理
 */
@Slf4j
public class TestInOutBound {
    public static void main(String[] args) {
        new ServerBootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        log.debug("ch: {}", ch);
                        // 先获取各个处理器所在的流水线
                        ChannelPipeline pipeline = ch.pipeline();
                        // 为流水线添加处理器
                        // head -> in1 -> in2 -> in3 -> out1 -> out2 -> out3 -> tail

                        // ---------------- 入站处理器 (解码过程) ----------------
                        // -> ByteBuf ->
                        pipeline.addLast("in-1", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                String name = buf.toString(Charset.defaultCharset());
                                Student student = new Student(name);
                                log.debug("入站处理器 - 1");
                                super.channelRead(ctx, student); // 必须通过super或者ctx.fireChannelRead(msg)转到下一个入站处理器
                            }
                        });
                        pipeline.addLast("in-2", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object student) throws Exception {
                                log.debug("入站处理器 - 2, msg: {}", student);
                                super.channelRead(ctx, student);
                            }
                        });
                        pipeline.addLast("in-3", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                log.debug("入站处理器 - 3");
                                super.channelRead(ctx, msg);

                                // 这个发送消息是直接从当前入站处理器开始向前找出站处理器，而不是从tail开始找
                                // ctx.writeAndFlush(ctx.alloc().buffer().writeBytes("Out-msg".getBytes()));

                                // 发送消息，触发出站处理器：顺序为从tail开始向前找的 out3 -> out2 -> out1
                                // ch.writeAndFlush(ctx.alloc().buffer().writeBytes("Out-msg".getBytes()));

                                Student responseStudent = new Student("李四");
                                ch.writeAndFlush(responseStudent);
                            }
                        });


                        // ---------------- 出站处理器 (编码过程) ----------------
                        // 执行顺序是反过来的：Out-3 -> Out-2 -> Out-1
                        pipeline.addLast("out-1", new ChannelOutboundHandlerAdapter() {
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                // 这一层是最底层的，必须保证 msg 已经是 ByteBuf 了，否则发不到网卡里
                                log.debug("出站-1: 收到最终数据: {}，发送给网卡", msg.getClass().getSimpleName());
                                super.write(ctx, msg, promise);
                            }
                        });
                        pipeline.addLast("out-2", new ChannelOutboundHandlerAdapter() {
                            @Override
                            public void write(ChannelHandlerContext ctx, Object name, ChannelPromise promise) throws Exception {
                                // 假设这一层负责将 String 转为 ByteBuf (编码)
                                log.debug("出站-2: 收到 String: {}, 将其转换为 ByteBuf", name);
                                String str = (String) name;
                                ByteBuf buf = ctx.alloc().buffer().writeBytes(str.getBytes());
                                super.write(ctx, buf, promise); // 传递变形后的 ByteBuf
                            }
                        });
                        pipeline.addLast("out-3", new ChannelOutboundHandlerAdapter() {
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                // 这一层首先收到 In-3 写过来的 Student 对象
                                log.debug("出站-3: 收到 Student: {}, 将其转换为 String", msg);
                                Student s = (Student) msg;
                                super.write(ctx, s.getName(), promise); // 传递变形后的 String
                            }
                        });
                    }
                })
                .bind(7070);
    }

    @Data
    @AllArgsConstructor
    static class Student {
        private String name;
    }
}
