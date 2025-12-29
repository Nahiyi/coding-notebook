package cn.clazs.jdk.stringpool;

import java.util.HashMap;

/**
 * 字符串常量池模型的终极理解
 */
public class StringPoolTest {
    public static void main(String[] args) {
        // 1. 生成了两个对象：
        //    一个是 "1" (在常量池中会有引用)
        //    一个是 new String("1")
        //    s1 指向 new 出来的对象
        String s1 = new String("1");
        s1.intern();
        String s2 = "1";
        // JDK7+ 输出 false
        // 因为 s1 指向堆中的 new 对象，s2 指向常量池引用的那个 "1" 字面量对象
        System.out.println(s1 == s2);

        // 2. 关键点来了！
        // new String("2") + new String("2") 会在堆中拼接生成一个新的 "22" 对象
        // 注意：此时常量池里并没有 "22" 的引用！
        String s3 = new String("2") + new String("2");

        // 执行 intern()：
        // 检查常量池，发现没有 "22"
        // 于是！JVM 直接把 s3 这个对象的引用（地址）放进了常量池
        // 不会再重新创建一个 "22" 对象了
        s3.intern();

        // s4 去常量池找 "22"，发现有了（就是刚才放进去的 s3 的引用），于是直接返回那个引用
        String s4 = "22";

        // JDK 7+ 输出：true
        // 这证明了：常量池里存的就是 s3 这个对象的引用！s4 拿到的也是这个引用
        System.out.println(s3 == s4);
    }
}