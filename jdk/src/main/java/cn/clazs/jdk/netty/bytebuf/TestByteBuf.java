package cn.clazs.jdk.netty.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static io.netty.buffer.ByteBufUtil.appendPrettyHexDump;
import static io.netty.util.internal.StringUtil.NEWLINE;

/**
 * Netty基于java的NIO的ByteBuffer进一步封装的可扩容的：ByteBuf的API简单演示
 */
public class TestByteBuf {
    public static void main(String[] args) {
        // buffer()方法默认使用直接内存，且初始容量为256字节
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(); // heapBuffer() 分配JVM堆内存
        System.out.println("池化情况、内存类型： " + buffer.getClass().getSimpleName());
        System.out.println("初始：");
        log(buffer);

        // 写入300字节，超过256
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            sb.append("a");
        }

        // 扩充成为512
        buffer.writeBytes(sb.toString().getBytes());

        System.out.println("又写入300字节后：");
        log(buffer);

    }

    public static void log(ByteBuf buffer) {
        int length = buffer.readableBytes();
        int rows = length / 16 + (length % 15 == 0 ? 0 : 1) + 4;
        StringBuilder buf = new StringBuilder(rows * 80 * 2)
                .append("read index: ").append(buffer.readerIndex()) // 读指针
                .append(", write index: ").append(buffer.writerIndex()) // 写指针
                .append(", capacity: ").append(buffer.capacity()) // 容量
                .append(NEWLINE);
        appendPrettyHexDump(buf, buffer);
        System.out.println(buf.toString());
    }
}
