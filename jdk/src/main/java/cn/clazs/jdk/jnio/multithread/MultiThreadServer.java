package cn.clazs.jdk.jnio.multithread;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * 多线程的NIO服务器，分为BOSS的Selector处理连接事件，Worker的Selector处理读写事件
 * 测试客户端：仍旧使用@Client1、@Client2使用即可
 */
@Slf4j
public class MultiThreadServer {
    public static void main(String[] args) throws IOException {
        //创建 ServerSocketChannel，它是基于 TCP 协议的服务端通道
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        // 创建一个Selector作为BOSS，专门处理服务器端连接事件
        Selector boss = Selector.open();
        // 将 ServerSocketChannel 注册到 Selector 上
        SelectionKey sscKey = ssc.register(boss, 0);
        // 设置 SelectionKey 关注连接事件，当有新的客户端连接请求到达时，Selector 会通知该线程
        sscKey.interestOps(SelectionKey.OP_ACCEPT);
        ssc.bind(new InetSocketAddress(7070));
        Thread.currentThread().setName("boss");
        log.debug("服务器初始化完毕: ServerSocketChannel-SelectionKey: {}", sscKey);

        Worker[] workers = new Worker[2];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker("worker-" + i);
        }

        long idx = 0L;
        while (true) {
            boss.select();
            Iterator<SelectionKey> iter = boss.selectedKeys().iterator();

            while (iter.hasNext()) {
                SelectionKey selectionKey = iter.next();
                iter.remove();

                // 注册在BOSS上的服务器sscChannel只关注连接事件
                if (selectionKey.isAcceptable()) {
                    // ssc等价于selectionKey.channel()，只有一个channel关注了连接事件，不必调用方法获取
                    SocketChannel channel = ssc.accept();
                    channel.configureBlocking(false); // 设为非阻塞模式

                    // 轮询，从多个worker中取处理连接（简单负载均衡）
                    Worker worker = workers[(int) (idx % workers.length)];
                    idx++;
                    log.debug("[before] 将客户端Channel注册到 {} 的 Selector 上", worker.getName());
                    // 为当前连接的客户端channel关注读事件
                    worker.register(channel);
                }
            }
        }
    }
}
