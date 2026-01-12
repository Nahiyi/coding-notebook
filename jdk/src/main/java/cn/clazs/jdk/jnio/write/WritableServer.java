package cn.clazs.jdk.jnio.write;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * 关注写事件的NIO服务器
 */
@Slf4j
public class WritableServer {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        Selector selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        ssc.bind(new InetSocketAddress(7070));

        log.debug("创建 {}, 并绑定到 Selector 的 OP_ACCEPT", ssc);

        while (true) {
            selector.select();

            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey selectionKey = iter.next();
                iter.remove();

                // 建立连接后，发送数据
                if (selectionKey.isAcceptable()) {
                    SocketChannel channel = ssc.accept();
                    channel.configureBlocking(false);
                    log.debug("建立连接: {}", channel);

                    SelectionKey scKey = channel.register(selector, 0);

                    // 构造大量数据
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 5_000_000; i++) {
                        sb.append("a");
                    }

                    ByteBuffer buffer = Charset.defaultCharset().encode(sb.toString());
                    log.debug("encode后: position={}, limit={}", buffer.position(), buffer.limit()); // 会自动切换读模式

                    int write = channel.write(buffer); // 读取buf数据发送网卡
                    log.debug("先发送: {}", write);

                    if (buffer.hasRemaining()) {
                        // 先发送一次，还有剩余，就让当前key关注可写事件
                        scKey.interestOps(scKey.interestOps() | SelectionKey.OP_WRITE);
                        // 绑定buf到scKey上
                        scKey.attach(buffer);
                    }
                } else if (selectionKey.isWritable()) {
                    ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();
                    SocketChannel channel = (SocketChannel) selectionKey.channel();
                    int write = channel.write(buffer);
                    log.debug("在写事件处理中写了: {}", write);

                    // 检查是否写完，清理buf，并取消关注可写事件
                    if (!buffer.hasRemaining()) {
                        selectionKey.attach(null);
                        selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                    }
                }
            }
        }
    }
}
