package cn.clazs.jdk.jnio.multithread;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.clazs.jdk.jnio.ByteBufferUtil.debugAll;

@Data
@Slf4j
public class Worker implements Runnable {
    private Thread thread;
    private Selector selector;
    private String name;
    private AtomicBoolean start = new AtomicBoolean(false); // Worker是否已初始化标志位
    private Queue<Runnable> queue = new ConcurrentLinkedQueue<>(); // 这里的Runnable跟线程没关系，仅仅作为一个无参无返回的函数式接口

    public Worker(String name) {
        this.name = name;
    }

    // 初始化Worker线程，一个Worker只创建一个线程维护，一个Selector用于注册，可对应多个SocketChannel
    public void register(SocketChannel channel) throws IOException {
        if (!start.get()) {
            thread = new Thread(this, name);
            selector = Selector.open(); // 先open再开线程
            thread.start();
            start.set(true);
        }

        // 将注册封装成行为，加入队列
        queue.offer(() -> {
            try {
                selector.wakeup();
                channel.register(selector, SelectionKey.OP_READ);
                log.debug("[after] 将客户端Channel注册到 {} 的 Selector 上", name);
            } catch (ClosedChannelException e) {
                throw new RuntimeException(e);
            }
        });
        selector.wakeup();
    }

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();

                Runnable task = queue.poll();
                if (task != null) {
                    // 取出注册的任务，在worker线程执行“将客户端注册在worker的selector且关注读事件”
                    task.run();
                }

                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey selectionKey = iter.next();
                    iter.remove();

                    // 这里值关注可读事件
                    if (selectionKey.isReadable()) {
                        SocketChannel channel = (SocketChannel) selectionKey.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(16);
                        try {
                            int read = channel.read(buffer);
                            log.debug("读取到客户端: {} 字节", read);
                            if (read == -1) {
                                log.warn("客户端连接关闭...");
                                selectionKey.cancel();
                            } else {
                                buffer.flip();
                                debugAll(buffer);
                            }
                        } catch (IOException e) {
                            // 添加一些客户端关闭的处理，防止关闭的读事件没被处理导致select无法正常阻塞！
                            log.error("客户端异常关闭...");
                            selectionKey.cancel();
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
