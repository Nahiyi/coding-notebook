package cn.clazs.jna;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * JNA高级功能演示
 * 展示指针、内存管理、回调等高级特性
 */
public class AdvancedDemo {

    /**
     * C标准库接口（高级功能）
     */
    public interface CLibrary extends Library {
        CLibrary INSTANCE = Native.load("msvcrt", CLibrary.class);

        // 内存操作
        Pointer malloc(long size);
        void free(Pointer ptr);
        void memset(Pointer ptr, int value, long size);
        void memcpy(Pointer dest, Pointer src, long size);

        // 字符串操作
        Pointer strcpy(Pointer dest, String src);
        Pointer strcat(Pointer dest, String src);
        int strlen(String str);
        int strcmp(String str1, String str2);

        // 数学函数（注释掉某些函数以提高兼容性）
        // double pow(double x, double y);
        // double sin(double x);
        // double cos(double x);

        // 随机数
        int rand();
        void srand(int seed);
    }

    /**
     * 自定义回调接口
     */
    public interface MathOperationCallback extends Callback {
        double operate(double a, double b);
    }

    /**
     * 回调函数接口
     */
    public interface StringProcessorCallback extends Callback {
        void process(String str);
    }

    public static void main(String[] args) {
        System.out.println("=== JNA高级功能演示 ===");

        // 1. 指针和内存管理
        demonstratePointersAndMemory();

        // 2. ByReference参数
        demonstrateByReference();

        // 3. 回调函数
        demonstrateCallbacks();

        // 4. 高级数据结构
        demonstrateAdvancedStructures();

        // 5. 性能测试
        demonstratePerformance();

        System.out.println("\n=== 高级功能演示完成 ===");
    }

    private static void demonstratePointersAndMemory() {
        System.out.println("\n1. 指针和内存管理:");

        CLibrary lib = CLibrary.INSTANCE;

        // 1.1 JNA Memory类使用
        System.out.println("1.1 JNA Memory类:");
        Memory memory = new Memory(256);
        memory.setString(0, "Hello from JNA Memory!");
        String content = memory.getString(0);
        System.out.println("内存内容: " + content);

        // 1.2 C库内存函数
        System.out.println("\n1.2 C库内存函数:");
        Pointer cMemory = lib.malloc(128);
        try {
            lib.strcpy(cMemory, "Hello from C malloc!");
            lib.strcat(cMemory, " Additional text.");
            String cContent = cMemory.getString(0);
            System.out.println("C内存内容: " + cContent);
            System.out.println("字符串长度: " + lib.strlen(cContent));
        } finally {
            lib.free(cMemory);
            System.out.println("C内存已释放");
        }

        // 1.3 内存拷贝操作
        System.out.println("\n1.3 内存拷贝操作:");
        Memory src = new Memory(100);
        Memory dest = new Memory(100);
        src.setString(0, "Source data for copy");
        lib.memcpy(dest, src, 20);
        System.out.println("拷贝结果: " + dest.getString(0));
    }

    private static void demonstrateByReference() {
        System.out.println("\n2. ByReference参数演示:");

        // 2.1 IntByReference使用
        System.out.println("2.1 IntByReference:");
        IntByReference intRef = new IntByReference(42);
        System.out.println("初始值: " + intRef.getValue());

        // 模拟通过引用修改值
        intRef.setValue(100);
        System.out.println("修改后: " + intRef.getValue());

        // 2.2 PointerByReference使用
        System.out.println("\n2.2 PointerByReference:");
        CLibrary lib = CLibrary.INSTANCE;
        PointerByReference ptrRef = new PointerByReference();
        Pointer allocated = lib.malloc(64);
        ptrRef.setPointer(allocated);

        System.out.println("引用的指针地址: " + ptrRef.getPointer());
        lib.free(ptrRef.getPointer());
        System.out.println("通过引用释放的内存已释放");
    }

    private static void demonstrateCallbacks() {
        System.out.println("\n3. 回调函数演示:");

        // 3.1 数学运算回调
        System.out.println("3.1 数学运算回调:");
        MathOperationCallback addCallback = (a, b) -> {
            System.out.println("执行加法: " + a + " + " + b + " = " + (a + b));
            return a + b;
        };

        MathOperationCallback multiplyCallback = (a, b) -> {
            System.out.println("执行乘法: " + a + " * " + b + " = " + (a * b));
            return a * b;
        };

        double result1 = addCallback.operate(5.0, 3.0);
        double result2 = multiplyCallback.operate(4.0, 6.0);
        System.out.println("加法结果: " + result1);
        System.out.println("乘法结果: " + result2);

        // 3.2 字符串处理回调
        System.out.println("\n3.2 字符串处理回调:");
        StringProcessorCallback processor = new StringProcessorCallback() {
            @Override
            public void process(String str) {
                System.out.println("处理字符串: " + str);
                System.out.println("字符串长度: " + str.length());
                System.out.println("字符串反转: " + new StringBuilder(str).reverse());
            }
        };

        processor.process("Hello JNA Callbacks!");
        processor.process("Java Native Access");
    }

    private static void demonstrateAdvancedStructures() {
        System.out.println("\n4. 高级数据结构:");

        // 4.1 嵌套结构体
        System.out.println("4.1 嵌套结构体:");

        @Structure.FieldOrder({"x", "y"})
        class Vec2 extends Structure {
            public int x, y;
            public Vec2() {}
            public Vec2(int x, int y) { this.x = x; this.y = y; }
        }

        @Structure.FieldOrder({"position", "velocity", "size"})
        class GameObject extends Structure {
            public Vec2 position = new Vec2();
            public Vec2 velocity = new Vec2();
            public int size;

            public GameObject() {}
            public GameObject(int x, int y, int vx, int vy, int size) {
                position.x = x; position.y = y;
                velocity.x = vx; velocity.y = vy;
                this.size = size;
            }

            @Override
            public String toString() {
                return String.format("GameObject[pos=%s, vel=%s, size=%d]", position, velocity, size);
            }
        }

        GameObject obj = new GameObject(100, 200, 5, -3, 10);
        System.out.println("游戏对象: " + obj);
        System.out.println("结构体大小: " + obj.size() + " 字节");

        // 4.2 结构体数组
        System.out.println("\n4.2 结构体数组:");
        GameObject[] objects = new GameObject[3];
        objects[0] = new GameObject(0, 0, 1, 1, 5);
        objects[1] = new GameObject(10, 10, -1, 2, 8);
        objects[2] = new GameObject(20, 5, 0, -1, 6);

        for (int i = 0; i < objects.length; i++) {
            System.out.println("对象[" + i + "]: " + objects[i]);
        }
    }

    private static void demonstratePerformance() {
        System.out.println("\n5. 性能测试:");

        CLibrary lib = CLibrary.INSTANCE;

        // 5.1 JNA调用vs Java方法性能对比
        System.out.println("5.1 性能对比测试:");

        int iterations = 100000;

        // Java Math.sqrt 性能
        long javaStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Math.sqrt(i % 100);
        }
        long javaTime = System.nanoTime() - javaStart;

        // JNA C库strlen性能替代sqrt
        long jnaStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            lib.strlen("test string");
        }
        long jnaTime = System.nanoTime() - jnaStart;

        System.out.println("Java Math.sqrt (" + iterations + "次): " + (javaTime / 1_000_000.0) + " ms");
        System.out.println("JNA C strlen (" + iterations + "次): " + (jnaTime / 1_000_000.0) + " ms");
        System.out.println("注意: 这是JNA调用开销的示例，不是等效功能的比较");

        // 5.2 批量操作优化建议
        System.out.println("\n5.2 优化建议:");
        System.out.println("- JNA调用有额外开销，应尽量减少调用次数");
        System.out.println("- 使用批量操作代替多次单次操作");
        System.out.println("- 对于性能敏感的代码，考虑使用JNI或直接Java实现");
        System.out.println("- 可以使用Direct Mapping提升性能");
    }
}