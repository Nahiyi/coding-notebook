package cn.clazs.jdk.netty.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static cn.clazs.jdk.netty.bytebuf.TestByteBuf.log;

/**
 * ByteBuf的零拷贝：切片slice，并未真实切片，只是基于引用做的
 */
public class TestSliceByteBuf {
    public static void main(String[] args) {
        // 每个ByteBuf的初始引用数是 1
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(10);
        buf.writeBytes(new byte[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'});
        log(buf);
        ByteBuf f1 = buf.slice(0, 5); // 参数1：起始索引；参数2：切片长度
        f1.retain(); // 引用计数+1
        log(f1);
        ByteBuf f2 = buf.slice(5, 5);
        f2.retain(); // 引用计数+1
        log(f2);

        System.out.println("######################");

        // 修改f1的内容，能直接影响到原buf
        f1.setByte(0, 'X');
        log(f1);
        log(buf);

        System.out.println("######################");

        System.out.println("释放原buf一次"); // 此时引用数应该是3，可以释放3次
        buf.release();
        buf.release();
        // buf.release();
        log(f1);
    }
}
