package cn.clazs.jna.struct;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 * JNA结构体映射演示类
 *
 * 演示如何在JNA中映射和使用C语言的结构体：
 * 1. 简单结构体：div_t (整数除法结果)
 * 2. 复杂结构体：tm (时间结构)
 * 3. 结构体按值传递 vs 按引用传递
 * 4. 从native函数返回的结构体
 *
 * @author clazs
 */
public class StructDemo {

    public static void main(String[] args) {
        System.out.println("=== JNA结构体映射演示 ===\n");

        // 演示1: div函数 - 返回结构体
        demonstrateDivFunction();

        // 演示2: localtime函数 - 返回结构体指针
        demonstrateLocaltimeFunction();

        // 演示3: mktime函数 - 传递结构体作为参数
        demonstrateMktimeFunction();

        // 演示4: 结构体按值传递 vs 按引用传递
        // demonstratePassingMechanism();
    }

    /**
     * 演示1: div()函数 - 返回结构体
     *
     * C语言原型：div_t div(int numerator, int denominator);
     *
     * 这个函数演示了最简单的情况：
     * - native函数直接返回一个结构体（按值返回）
     * - JNA会自动将结构体字段映射到Java对象
     */
    private static void demonstrateDivFunction() {
        System.out.println("【演示1】div()函数 - 返回结构体");
        System.out.println("C原型: div_t div(int numerator, int denominator);");
        System.out.println();

        MsvcrtLibrary lib = MsvcrtLibrary.INSTANCE;

        // 示例1: 17 / 5 = 3 余 2
        int numerator1 = 17;
        int denominator1 = 5;
        DivT.ByValue result1 = lib.div(numerator1, denominator1);
        System.out.printf("%d / %d = %d\n", numerator1, denominator1, result1.quot);
        System.out.printf("%d %% %d = %d\n", numerator1, denominator1, result1.rem);
        System.out.println("返回的div_t结构体: " + result1);
        System.out.println();

        // 示例2: 100 / 7 = 14 余 2
        int numerator2 = 100;
        int denominator2 = 7;
        DivT.ByValue result2 = lib.div(numerator2, denominator2);
        System.out.printf("%d / %d = %d\n", numerator2, denominator2, result2.quot);
        System.out.printf("%d %% %d = %d\n", numerator2, denominator2, result2.rem);
        System.out.println("返回的div_t结构体: " + result2);
        System.out.println();

        System.out.println("关键点：");
        System.out.println("- div()函数直接返回div_t结构体（按值返回）");
        System.out.println("- 必须使用DivT.ByValue作为返回类型");
        System.out.println("- JNA自动将C结构体映射到DivT.ByValue对象");
        System.out.println("- 可以直接访问结构体的字段（quot和rem）");
        System.out.println("- DivT类继承自Structure，使用@FieldOrder指定字段顺序");
        System.out.println();

        System.out.println(String.format("%60s", " ").replace(" ", "="));
        System.out.println();
    }

    /**
     * 演示2: localtime()函数 - 返回结构体指针
     *
     * C语言原型：struct tm* localtime(const time_t* timer);
     *
     * 这个函数演示了更复杂的情况：
     * - native函数返回一个结构体指针
     * - 需要从Pointer创建Structure对象
     * - time_t是一个typedef类型，在JNA中映射为NativeLong
     */
    private static void demonstrateLocaltimeFunction() {
        System.out.println("【演示2】localtime()函数 - 返回结构体指针");
        System.out.println("C原型: struct tm* localtime(const time_t* timer);");
        System.out.println();

        MsvcrtLibrary lib = MsvcrtLibrary.INSTANCE;

        // 步骤1: 获取当前时间（Unix时间戳）
        NativeLong timestamp = lib.time(null);
        System.out.println("当前Unix时间戳: " + timestamp + " 秒");
        System.out.println();

        // 步骤2: 分配内存存储time_t值
        Pointer timePtr = lib.malloc(NativeLong.SIZE);
        timePtr.setNativeLong(0, timestamp);
        System.out.println("已分配内存存储时间戳，地址: " + timePtr);
        System.out.println();

        // 步骤3: 调用localtime，返回tm结构体指针
        Pointer tmPtr = lib.localtime(timePtr);
        System.out.println("localtime返回的tm结构体指针: " + tmPtr);
        System.out.println();

        // 步骤4: 从指针创建Tm对象并读取数据
        // 重要：使用Pointer构造函数创建Structure，并调用read()读取数据
        Tm tm = new Tm(tmPtr);
        System.out.println("当前本地时间: " + tm.toFormattedString());
        System.out.println("星期: " + tm.getWeekdayName());
        System.out.println("一年中的第: " + (tm.tm_yday + 1) + " 天");
        System.out.println("夏令时: " + (tm.tm_isdst > 0 ? "生效" : (tm.tm_isdst == 0 ? "未生效" : "未知")));
        System.out.println();

        System.out.println("完整的tm结构体内容:");
        System.out.println(tm);
        System.out.println();

        // 步骤5: 释放分配的内存
        lib.free(timePtr);

        System.out.println("关键点：");
        System.out.println("- localtime()返回的是结构体指针（Pointer）");
        System.out.println("- 需要使用new Tm(Pointer)构造函数从指针创建Structure");
        System.out.println("- JNA会自动调用read()方法从native内存读取结构体数据");
        System.out.println("- Tm类必须正确映射所有9个字段，顺序不能错");
        System.out.println("- time_t在JNA中映射为NativeLong（可能是32位或64位）");
        System.out.println("- 需要手动管理native内存（malloc/free）");
        System.out.println();

        System.out.println(String.format("%60s", " ").replace(" ", "="));
        System.out.println();
    }

    /**
     * 演示3: mktime()函数 - 传递结构体作为参数
     *
     * C语言原型：time_t mktime(struct tm* timer);
     *
     * 这个函数演示了如何将Java结构体作为参数传递给native函数：
     * - 创建Tm对象并设置字段值
     * - 调用write()方法将数据写入native内存
     * - JNA自动将结构体指针传递给native函数
     * - native函数可以修改结构体内容
     */
    private static void demonstrateMktimeFunction() {
        System.out.println("【演示3】mktime()函数 - 传递结构体作为参数");
        System.out.println("C原型: time_t mktime(struct tm* timer);");
        System.out.println();

        MsvcrtLibrary lib = MsvcrtLibrary.INSTANCE;

        // 示例: 2025年1月15日 14:30:25
        System.out.println("示例: 将 2025-01-15 14:30:25 转换为Unix时间戳");

        // 步骤1: 创建Tm对象并设置字段值
        Tm tm = new Tm();
        tm.tm_year = 2025 - 1900;  // tm_year是从1900开始的年数
        tm.tm_mon = 0;              // 1月 (0-based，0表示1月)
        tm.tm_mday = 15;            // 15日
        tm.tm_hour = 14;            // 14时
        tm.tm_min = 30;             // 30分
        tm.tm_sec = 25;             // 25秒
        tm.tm_isdst = 0;            // 不使用夏令时

        System.out.println("输入的tm结构体:");
        System.out.println(tm);
        System.out.println();

        // 步骤2: 调用write()方法，将Java对象的字段写入native内存
        // 这一步非常重要！如果不调用write()，native函数看不到Java字段值
        tm.write();
        System.out.println("已调用tm.write()将数据写入native内存");
        System.out.println();

        // 步骤3: 调用mktime函数，传入Tm对象
        // JNA会自动将Tm对象转换为struct tm*指针传递给native函数
        NativeLong timestamp = lib.mktime(tm);
        System.out.println("mktime返回的Unix时间戳: " + timestamp + " 秒");
        System.out.println();

        // 步骤4: (可选) 调用read()方法，从native内存读取修改后的数据
        // 有些函数会修改传入的结构体，调用read()可以获取更新后的值
        tm.read();
        System.out.println("调用tm.read()后，tm结构体可能被native函数修改:");
        System.out.println("  tm_wday (星期几): " + tm.getWeekdayName());
        System.out.println("  tm_yday (一年第几天): " + (tm.tm_yday + 1));
        System.out.println();

        // 示例2: 验证往返转换
        System.out.println("示例2: 验证往返转换 (timestamp -> localtime -> timestamp)");

        // 使用刚才获取的timestamp，调用localtime转换回来
        Pointer timePtr = lib.malloc(NativeLong.SIZE);
        timePtr.setNativeLong(0, timestamp);
        Pointer tmPtr = lib.localtime(timePtr);
        Tm tm2 = new Tm(tmPtr);

        System.out.println("localtime返回: " + tm2.toFormattedString());
        System.out.println("星期: " + tm2.getWeekdayName());
        System.out.println();

        // 再转换回timestamp
        tm2.write();
        NativeLong timestamp2 = lib.mktime(tm2);
        System.out.println("再次调用mktime: " + timestamp2 + " 秒");
        System.out.println("两次时间戳是否一致: " + timestamp.equals(timestamp2));
        System.out.println();

        lib.free(timePtr);

        System.out.println("关键点：");
        System.out.println("- mktime()接受struct tm*作为参数（结构体指针）");
        System.out.println("- 创建Tm对象后，必须设置字段值");
        System.out.println("- 调用write()方法将Java字段写入native内存（重要！）");
        System.out.println("- JNA自动将Tm对象转换为指针传递给native函数");
        System.out.println("- native函数可能会修改结构体内容，调用read()可获取更新");
        System.out.println("- mktime会自动计算并填充tm_wday和tm_yday字段");
        System.out.println("- 这是JNA结构体作为参数传递的典型用法");
        System.out.println();

        System.out.println(String.format("%60s", " ").replace(" ", "="));
        System.out.println();
    }

    /**
     * 演示4: 结构体按值传递 vs 按引用传递
     *
     * JNA支持三种传递方式：
     * 1. ByReference（默认）- 传递结构体指针
     * 2. ByValue - 传递结构体值（拷贝整个结构体）
     * 3. 指针显式指定
     *
     * 注意：msvcrt.dll中的函数可能不支持按值传递结构体，
     * 这里主要演示概念和定义方式
     */
    private static void demonstratePassingMechanism() {
        System.out.println("【演示4】结构体传递方式");
        System.out.println();

        System.out.println("JNA支持三种结构体传递方式：");
        System.out.println();

        System.out.println("1. ByReference（按引用传递，默认）");
        System.out.println("   - 传递结构体指针");
        System.out.println("   - 函数原型: void func(MyStruct* s)");
        System.out.println("   - Java定义: void func(MyStruct s)");
        System.out.println("   - 或者显式: void func(MyStruct.ByReference s)");
        System.out.println();

        System.out.println("2. ByValue（按值传递）");
        System.out.println("   - 传递结构体值的拷贝");
        System.out.println("   - 函数原型: void func(MyStruct s)");
        System.out.println("   - Java定义: void func(MyStruct.ByValue s)");
        System.out.println("   - 需要在Structure类中定义ByValue内部类");
        System.out.println();

        System.out.println("3. Pointer（显式指针）");
        System.out.println("   - 使用Pointer类型");
        System.out.println("   - 需要手动创建和管理内存");
        System.out.println("   - 更底层，更灵活，但也更复杂");
        System.out.println();

        System.out.println("示例（DivT类的定义）：");
        System.out.println("```java");
        System.out.println("public class DivT extends Structure {");
        System.out.println("    public int quot;");
        System.out.println("    public int rem;");
        System.out.println("    ");
        System.out.println("    // 按值传递");
        System.out.println("    public static class ByValue extends DivT implements Structure.ByValue {}");
        System.out.println("    ");
        System.out.println("    // 按引用传递");
        System.out.println("    public static class ByReference extends DivT implements Structure.ByReference {}");
        System.out.println("}");
        System.out.println("```");
        System.out.println();

        System.out.println("关键点：");
        System.out.println("- 大多数C函数使用指针传递结构体（ByReference）");
        System.out.println("- 按值传递（ByValue）会拷贝整个结构体，性能较低");
        System.out.println("- 检查C函数签名来确定使用哪种方式");
        System.out.println("- localtime返回的就是指针（ByReference）");
        System.out.println("- div返回的是结构体值（ByValue）");
        System.out.println();

        System.out.println(String.format("%60s", " ").replace(" ", "="));
        System.out.println();
    }
}
