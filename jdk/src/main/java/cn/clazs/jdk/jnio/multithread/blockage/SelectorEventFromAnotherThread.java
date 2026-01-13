package cn.clazs.jdk.jnio.multithread.blockage;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * 验证：另一个线程注册channel后，且客户端连接后，能否感知到真正的ACCEPT事件
 */
@Slf4j
public class SelectorEventFromAnotherThread {
    private static boolean running = true;

    public static void main(String[] args) throws Exception {
        final Selector selector = Selector.open();

        // 线程1：Event Loop
        Thread eventLoopThread = new Thread(() -> {
            try {
                log.debug("Event Loop: 开始运行...");

                while (running) {
                    log.debug("Event Loop: 调用 select() 阻塞...");
                    int count = selector.select();
                    log.debug("Event Loop: select() 返回！count={}", count);

                    // 处理就绪事件
                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();

                        if (key.isAcceptable()) {
                            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                            SocketChannel sc = ssc.accept();
                            log.debug("Event Loop: 接受连接！sc={}", sc);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "EventLoopThread");

        eventLoopThread.start();

        // 等待Event Loop进入select()阻塞
        TimeUnit.SECONDS.sleep(2);

        // 主线程：注册ServerSocketChannel
        log.debug("主线程: 注册 ServerSocketChannel...");

        // ⚠️ 关键修复：必须先wakeup()打破select()的阻塞，否则会死锁！
        selector.wakeup();
        log.debug("主线程: 已调用 wakeup()");

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.bind(new InetSocketAddress(7070));
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        log.debug("主线程: 注册完成！");

        // 等待Event Loop被wakeup唤醒
        TimeUnit.SECONDS.sleep(1);

        // 主线程：模拟客户端连接
        log.debug("主线程: 模拟客户端连接...");
        SocketChannel client = SocketChannel.open();
        client.connect(new InetSocketAddress("localhost", 7070));
        log.debug("主线程: 客户端连接成功！");

        // 等待Event Loop处理完ACCEPT事件
        TimeUnit.SECONDS.sleep(1);

        // 停止Event Loop
        running = false;
        selector.wakeup();
        eventLoopThread.join();

        client.close();
        ssc.close();
        selector.close();
        log.debug("主线程: 程序结束");
    }
}
