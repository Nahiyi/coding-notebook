package cn.clazs.jdk.jnio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class Client2 {
    public static void main(String[] args) throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("localhost", 7070));

        sc.write(Charset.defaultCharset().encode("world"));

        // 模拟阻塞等待
        System.in.read();
    }
}
