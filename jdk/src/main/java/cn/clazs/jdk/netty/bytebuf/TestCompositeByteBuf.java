package cn.clazs.jdk.netty.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;

import static cn.clazs.jdk.netty.bytebuf.TestByteBuf.log;

public class TestCompositeByteBuf {
    public static void main(String[] args) {
        ByteBuf buf1 = ByteBufAllocator.DEFAULT.buffer(5);
        buf1.writeBytes(new byte[]{1, 2, 3, 4, 5});
        ByteBuf buf2 = ByteBufAllocator.DEFAULT.buffer(5);
        buf2.writeBytes(new byte[]{6, 7, 8, 9, 10});

        log(buf1);
        log(buf2);
        System.out.println("\n##############################\n");
        CompositeByteBuf compositeBuf1 = ByteBufAllocator.DEFAULT.compositeBuffer();
        CompositeByteBuf compositeBuf2 = ByteBufAllocator.DEFAULT.compositeBuffer();
        // true 表示增加新的 ByteBuf 自动递增 write index, 否则 write index 会始终为 0
        compositeBuf1.addComponents(true, buf1, buf2);
        log(compositeBuf1);
        compositeBuf2.addComponents(true, buf2, buf1);
        log(compositeBuf2);
    }
}
