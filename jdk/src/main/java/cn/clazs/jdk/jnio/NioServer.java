package cn.clazs.jdk.jnio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import static cn.clazs.jdk.jnio.ByteBufferUtil.debugRead;

/**
 * Java NIO 非阻塞IO服务器示例
 *
 * <p>这是一个典型的NIO底层实现，充分利用了操作系统的IO多路复用模型</p>
 *
 * <p>核心概念：</p>
 * <ul>
 *   <li>Channel: 通道，相当于传统IO中的Stream，但可异步读写</li>
 *   <li>Buffer: 缓冲区，数据读写通过缓冲区进行</li>
 *   <li>Selector: 选择器，能够检测多个注册的通道上是否有事件发生（IO多路复用）</li>
 *   <li>SelectionKey: 选择键，表示Channel和Selector的注册关系，包含事件类型</li>
 * </ul>
 *
 * <p>与传统BIO（Blocking IO）的区别：</p>
 * <ul>
 *   <li>BIO: 每个连接需要一个独立线程，阻塞式读写，资源消耗大</li>
 *   <li>NIO: 单线程可处理多个连接，非阻塞式读写，基于事件驱动</li>
 * </ul>
 *
 * <p>操作系统底层支持：</p>
 * <ul>
 *   <li>Linux: 使用 epoll 实现IO多路复用</li>
 *   <li>Windows、Mac: 略...</li>
 * </ul>
 */
@Slf4j
public class NioServer {
    public static void main(String[] args) {
        // 1.创建ServerSocketChannel并绑定端口（相当于传统ServerSocket）
        // ServerSocketChannel：用于监听和接受客户端连接请求；
        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ssc.bind(new InetSocketAddress(7070));
            log.debug("服务器启动 ServerSocketChannel: {}", ssc);

            // 2. 创建Selector选择器（IO多路复用的核心）
            // Selector会利用操作系统的底层机制（如Linux的epoll）来监听多个Channel的事件
            Selector selector = Selector.open();

            // 3. 设置Channel为非阻塞模式，只有非阻塞模式的Channel才能注册到Selector
            // configureBlocking(false)会调用底层系统API设置socket为O_NONBLOCK模式
            ssc.configureBlocking(false);

            // 4. 将ServerSocketChannel注册到Selector，关注OP_ACCEPT事件：即接收客户端连接的事件
            // 返回的SelectionKey包含了Channel和Selector的关联关系
            SelectionKey sscKey = ssc.register(selector, SelectionKey.OP_ACCEPT);

            // 5. 事件循环（Event Loop）- NIO的核心模式
            while (true) {
                // 6. 阻塞等待事件发生（如果没有事件，线程会阻塞在这里）
                // selector.select()会调用操作系统的select/epoll_wait系统调用
                // 当有就绪的Channel时返回，返回值是就绪Channel的数量
                int count = selector.select();
                // selector.selectNow() 是非阻塞版本，立即返回当前就绪的Channel数量

                log.debug("select count: {}", count);

                // 7. 获取所有就绪的SelectionKey（这是依赖于S/SocketChannel对于指定Selector的注册！）
                Set<SelectionKey> keys = selector.selectedKeys();

                // 8. 遍历处理所有就绪事件
                Iterator<SelectionKey> iter = keys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    log.debug("当前遍历 key: {}", key);

                    // 9. 判断事件类型并处理
                    if (key.isAcceptable()) {
                        // OP_ACCEPT事件：有新的客户端连接请求
                        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                        // 接受客户端连接（非阻塞模式下，如果没有连接会返回null）
                        SocketChannel sc = channel.accept();
                        log.debug("连接建立: {}", sc);
                        // 这里将SocketChannel也设置为非阻塞
                        // 并注册到Selector关注OP_READ事件，以读取客户端数据
                        sc.configureBlocking(false);
                        sc.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer bf = ByteBuffer.allocate(16);
                        channel.read(bf);
                        bf.flip();
                        debugRead(bf);
                    } else if (key.isWritable()) {

                    }
                    // 10. 处理完毕后必须手动移除该SelectionKey
                    // 这一点非常重要！selectedKeys()不会自动清理已处理的Key
                    // 如果不remove，下次循环时同一个事件还会被重复处理
                    iter.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
