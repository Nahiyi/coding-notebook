package cn.clazs.jdk.jnio.multithread.blockage;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.TimeUnit;

/**
 * 验证：另一个线程注册channel后，阻塞的select()能否感知
 */
@Slf4j
public class SelectorMultiThreadDemo {
    public static void main(String[] args) throws IOException, InterruptedException {
        final Selector selector = Selector.open();

        // 线程1：启动select()阻塞
        Thread selectThread = new Thread(() -> {
            try {
                log.debug("线程1: 开始 select() 阻塞...");
                int count = selector.select();
                log.debug("线程1: select() 返回！count={}", count);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "SelectThread");

        selectThread.start();

        // 等待线程1进入select()阻塞
        TimeUnit.SECONDS.sleep(2);

        // 线程2（主线程）：注册channel
        log.debug("主线程: 准备注册 ServerSocketChannel...");

        // ⚠️ 关键修复：必须先wakeup()打破select()的阻塞，否则会死锁！
        selector.wakeup();
        log.debug("主线程: 已调用 wakeup()");

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.bind(new InetSocketAddress(7070));

        // 现在可以安全注册了
        SelectionKey key = ssc.register(selector, SelectionKey.OP_ACCEPT);
        log.debug("主线程: 注册完成！key={}", key);

        // 等待线程1结束
        selectThread.join();
        log.debug("主线程: 线程1已结束");

        selector.close();
    }
}
