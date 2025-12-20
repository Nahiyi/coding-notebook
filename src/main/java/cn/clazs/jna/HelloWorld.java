package cn.clazs.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

/**
 * JNA基础Hello World示例
 * 演示如何调用C标准库的printf函数
 *
 * 核心概念：
 * 1. 继承Library接口定义本地库映射
 * 2. 使用Native.load加载本地库
 * 3. 在接口中声明与本地函数对应的Java方法
 * 4. 平台差异化处理（Windows使用msvcrt，其他使用c）
 */
public class HelloWorld {

    /**
     * 定义C标准库接口
     * Windows: msvcrt.dll (Microsoft Visual C++ Runtime)
     * Linux/Unix: libc.so.6 (C标准库)
     */
    public interface CLibrary extends Library {
        // 根据平台自动选择对应的库
        CLibrary INSTANCE = Native.load(Platform.isWindows() ? "msvcrt" : "c",CLibrary.class);

        // 声明C标准库中的函数
        void printf(String format, Object... args);

        // 字符串长度计算函数
        int strlen(String str);

        // 数学函数
        double sqrt(double x);

        // 内存分配函数
        Pointer malloc(long size);
        void free(Pointer ptr);
    }

    public static void main(String[] args) {
        System.out.println("=== JNA Hello World Demo ===");

        // 1. 调用C标准库的printf函数
        System.out.println("\n1. 调用C标准库printf函数:");
        CLibrary.INSTANCE.printf("Hello, World from JNA! %d\n", 1234);
        CLibrary.INSTANCE.printf("当前Java版本: %s\n", System.getProperty("java.version"));

        // 2. 调用字符串长度函数
        System.out.println("\n2. 调用字符串长度函数:");
        String testString = "Hello JNA";
        int length = CLibrary.INSTANCE.strlen(testString);
        System.out.printf("字符串 '%s' 的长度是 %d\n", testString, length);

        // 3. 调用数学函数
        System.out.println("\n3. 调用数学函数:");
        double sqrtResult = CLibrary.INSTANCE.sqrt(16.0);
        System.out.printf("sqrt(16.0) = %.2f\n", sqrtResult);

        // 4. 内存分配演示
        System.out.println("\n4. 内存分配演示:");
        com.sun.jna.Pointer ptr = CLibrary.INSTANCE.malloc(1024);
        System.out.println("分配的内存地址: " + ptr);
        CLibrary.INSTANCE.free(ptr);
        System.out.println("内存已释放");

        System.out.println("\n=== JNA基础演示完成 ===");
    }
}