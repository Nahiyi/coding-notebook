package cn.clazs.jdk.jnio.bytebuf;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * ByteBuffer和Channel的概念、API的简单demo
 */
@Slf4j
public class ChannelDemo1 {
    public static void main(String[] args) throws IOException {
        RandomAccessFile file = new RandomAccessFile("D:\\develop\\Projects\\IDEAProjects\\code-playground\\jdk\\src\\main\\resources\\demo-data.txt", "rw");
        FileChannel channel = file.getChannel();
        // 开辟10字节内存（堆内）的字节缓冲
        ByteBuffer buffer = ByteBuffer.allocate(10);
        while (true) {
            // 向 buffer 写入
            int len = channel.read(buffer);
            log.debug("读到字节数：{}", len);
            if (len == -1) {
                break;
            }
            // 切换 buffer 读模式
            buffer.flip();
            while (buffer.hasRemaining()) {
                log.debug("{}", (char) buffer.get());
            }
            // 切换 buffer 写模式
            buffer.clear();
        }
        channel.close();
    }
}
