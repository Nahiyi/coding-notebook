package cn.clazs.jdk.jnio.write;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@Slf4j
public class WritableClient {
    public static void main(String[] args) throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("localhost", 7070));
        int count = 0;
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024); // 1MB
        while (true) {
            int read = sc.read(buffer); // 读取数据完后，没数据读了会阻塞
            buffer.clear();
            count += read;
            log.debug("从服务器读取到: {} 字节的数据, 截止目前共收到: {} 字节", read, count);
        }
    }
}
