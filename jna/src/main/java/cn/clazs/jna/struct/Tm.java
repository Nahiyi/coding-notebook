package cn.clazs.jna.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * JNA映射C语言的tm结构体（时间结构）
 *
 * C语言定义：
 * struct tm {
 *     int tm_sec;   // 秒 [0, 60]
 *     int tm_min;   // 分 [0, 59]
 *     int tm_hour;  // 时 [0, 23]
 *     int tm_mday;  // 日 [1, 31]
 *     int tm_mon;   // 月 [0, 11]
 *     int tm_year;  // 年 (从1900年开始)
 *     int tm_wday;  // 星期几 [0, 6] (周日 = 0)
 *     int tm_yday;  * 一年中的第几天 [0, 365]
 *     int tm_isdst; // 夏令时标志
 * };
 *
 * @author clazs
 */
public class Tm extends Structure {

    /**
     * 秒 [0, 60] (60用于闰秒)
     */
    public int tm_sec;

    /**
     * 分 [0, 59]
     */
    public int tm_min;

    /**
     * 时 [0, 23]
     */
    public int tm_hour;

    /**
     * 日 [1, 31]
     */
    public int tm_mday;

    /**
     * 月 [0, 11] (0表示1月)
     */
    public int tm_mon;

    /**
     * 年 (从1900年开始的年数，如2025年则为125)
     */
    public int tm_year;

    /**
     * 星期几 [0, 6] (0表示周日，1表示周一，依此类推)
     */
    public int tm_wday;

    /**
     * 一年中的第几天 [0, 365]
     */
    public int tm_yday;

    /**
     * 夏令时标志
     * >0: 夏令时生效
     * =0: 夏令时不生效
     * <0: 信息不可用
     */
    public int tm_isdst;

    /**
     * 默认构造函数
     */
    public Tm() {
        super();
    }

    /**
     * 从Pointer构造Tm对象
     * 用于从native函数返回的指针创建Structure
     *
     * @param pointer 指向tm结构体的native指针
     */
    public Tm(Pointer pointer) {
        super(pointer);
        read();  // 自动从native内存读取数据
    }

    /**
     * 按值传递的内部类
     */
    public static class ByValue extends Tm implements Structure.ByValue {
        public ByValue() {
            super();
        }
    }

    /**
     * 按引用传递的内部类
     */
    public static class ByReference extends Tm implements Structure.ByReference {
        public ByReference() {
            super();
        }
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("tm_sec", "tm_min", "tm_hour", "tm_mday",
                "tm_mon", "tm_year", "tm_wday", "tm_yday", "tm_isdst");
    }

    /**
     * 获取格式化的日期时间字符串
     * @return 格式化的日期时间，如 "2025-01-15 14:30:25"
     */
    public String toFormattedString() {
        int year = 1900 + tm_year;
        int month = tm_mon + 1;  // tm_mon是0-based
        return String.format("%04d-%02d-%02d %02d:%02d:%02d",
                year, month, tm_mday, tm_hour, tm_min, tm_sec);
    }

    /**
     * 获取星期几的中文名称
     */
    public String getWeekdayName() {
        String[] weekdays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return weekdays[tm_wday];
    }

    @Override
    public String toString() {
        return String.format("Tm{%s, %s, 第%d天, 夏令时=%s}",
                toFormattedString(),
                getWeekdayName(),
                tm_yday + 1,
                tm_isdst > 0 ? "是" : (tm_isdst == 0 ? "否" : "未知"));
    }
}
