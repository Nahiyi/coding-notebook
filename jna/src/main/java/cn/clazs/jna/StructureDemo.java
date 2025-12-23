package cn.clazs.jna;

import com.sun.jna.*;

/**
 * JNA结构体演示
 * 展示如何在Java中定义和使用C语言结构体
 */
public class StructureDemo {

    /**
     * 时间结构体示例
     */
    @Structure.FieldOrder({"year", "month", "day", "hour", "minute", "second"})
    public static class TimeStruct extends Structure {
        public int year;
        public int month;
        public int day;
        public int hour;
        public int minute;
        public int second;

        public TimeStruct() {}

        public TimeStruct(int year, int month, int day, int hour, int minute, int second) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.hour = hour;
            this.minute = minute;
            this.second = second;
        }

        @Override
        public String toString() {
            return String.format("%04d-%02d-%02d %02d:%02d:%02d",
                year, month, day, hour, minute, second);
        }
    }

    /**
     * 点坐标结构体
     */
    @Structure.FieldOrder({"x", "y"})
    public static class Point extends Structure {
        public int x;
        public int y;

        public Point() {}

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        // 按值传递
        public static class ByValue extends Point implements Structure.ByValue {
            public ByValue() {}
            public ByValue(int x, int y) {
                super(x, y);
            }
        }

        // 按引用传递
        public static class ByReference extends Point implements Structure.ByReference {
            public ByReference() {}
            public ByReference(int x, int y) {
                super(x, y);
            }
        }

        @Override
        public String toString() {
            return String.format("(%d, %d)", x, y);
        }
    }

    /**
     * 矩形结构体
     */
    @Structure.FieldOrder({"left", "top", "right", "bottom"})
    public static class Rect extends Structure {
        public int left;
        public int top;
        public int right;
        public int bottom;

        public Rect() {}

        public Rect(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public int getWidth() {
            return right - left;
        }

        public int getHeight() {
            return bottom - top;
        }

        @Override
        public String toString() {
            return String.format("Rect[l=%d,t=%d,r=%d,b=%d,w=%d,h=%d]",
                left, top, right, bottom, getWidth(), getHeight());
        }
    }

    /**
     * 自定义库接口，模拟结构体操作
     */
    public interface CustomLibrary extends Library {
        CustomLibrary INSTANCE = Native.load("msvcrt", CustomLibrary.class);

        // 内存操作函数
        Pointer malloc(long size);
        void free(Pointer ptr);
        void memset(Pointer ptr, int value, long size);
        void memcpy(Pointer dest, Pointer src, long size);

        // 字符串操作
        Pointer strcpy(Pointer dest, String src);
        int strlen(String str);
    }

    /**
     * 回调接口演示
     */
    public interface CompareCallback extends Callback {
        int compare(int a, int b);
    }

    public static void main(String[] args) {
        System.out.println("=== JNA结构体演示 ===");

        // 1. 基础结构体操作
        demonstrateBasicStructures();

        // 2. 结构体内存操作
        demonstrateMemoryOperations();

        // 3. 按值和按引用传递
        demonstratePassingMechanisms();

        // 4. 回调函数演示
        demonstrateCallbacks();

        // 5. 复杂结构体操作
        demonstrateComplexOperations();

        System.out.println("\n=== 结构体演示完成 ===");
    }

    private static void demonstrateBasicStructures() {
        System.out.println("\n1. 基础结构体操作:");

        // 创建时间结构体
        TimeStruct time = new TimeStruct(2024, 1, 15, 10, 30, 45);
        System.out.println("时间结构体: " + time);
        System.out.println("结构体大小: " + time.size() + " 字节");

        // 创建点结构体
        Point point1 = new Point(10, 20);
        Point point2 = new Point(30, 40);
        System.out.println("点1: " + point1);
        System.out.println("点2: " + point2);

        // 创建矩形结构体
        Rect rect = new Rect(10, 10, 100, 50);
        System.out.println("矩形: " + rect);
    }

    private static void demonstrateMemoryOperations() {
        System.out.println("\n2. 结构体内存操作:");

        CustomLibrary lib = CustomLibrary.INSTANCE;

        // 分配内存并写入结构体数据
        Memory memory = new Memory(100);
        Point point = new Point(100, 200);

        // 将结构体数据写入内存
        point.write();
        memory.setInt(0, point.x); // 写入x坐标
        memory.setInt(4, point.y); // 写入y坐标

        System.out.println("内存分配地址: " + memory);
        System.out.println("写入的数据: x=" + memory.getInt(0) + ", y=" + memory.getInt(4));

        // 使用memset填充内存
        lib.memset(memory, 65, 10); // 用'A'填充前10个字节
        System.out.println("内存填充完成");
    }

    private static void demonstratePassingMechanisms() {
        System.out.println("\n3. 按值和按引用传递:");

        // 按值传递
        Point.ByValue pointByValue = new Point.ByValue(50, 60);
        System.out.println("按值传递的点: " + pointByValue);

        // 按引用传递
        Point.ByReference pointByRef = new Point.ByReference();
        pointByRef.x = 70;
        pointByRef.y = 80;
        System.out.println("按引用传递的点: " + pointByRef);
        System.out.println("修改前: x=" + pointByRef.x + ", y=" + pointByRef.y);

        // 模拟通过引用修改值
        pointByRef.x = 90;
        pointByRef.y = 100;
        System.out.println("修改后: x=" + pointByRef.x + ", y=" + pointByRef.y);
    }

    private static void demonstrateCallbacks() {
        System.out.println("\n4. 回调函数演示:");

        CompareCallback callback = new CompareCallback() {
            @Override
            public int compare(int a, int b) {
                System.out.println("回调函数被调用: compare(" + a + ", " + b + ")");
                return a - b; // 返回差值
            }
        };

        int result1 = callback.compare(10, 5);
        int result2 = callback.compare(3, 8);

        System.out.println("比较结果1: " + result1);
        System.out.println("比较结果2: " + result2);
    }

    private static void demonstrateComplexOperations() {
        System.out.println("\n5. 复杂结构体操作:");

        // 创建多个点
        Point[] points = new Point[4];
        points[0] = new Point(0, 0);
        points[1] = new Point(10, 10);
        points[2] = new Point(20, 5);
        points[3] = new Point(15, 15);

        System.out.println("点集合:");
        for (int i = 0; i < points.length; i++) {
            System.out.println("  点[" + i + "]: " + points[i]);
        }

        // 计算包围矩形
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (Point p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        Rect boundingBox = new Rect(minX, minY, maxX, maxY);
        System.out.println("包围矩形: " + boundingBox);
        System.out.println("包围面积: " + boundingBox.getWidth() * boundingBox.getHeight());
    }
}