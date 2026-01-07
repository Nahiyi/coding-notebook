package cn.clazs.jna.struct;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 * 定义msvcrt.dll中的函数接口
 *
 * msvcrt.dll是Microsoft的C运行库，包含标准C函数
 * 这里演示使用结构体的函数：
 * - div(): 整数除法，返回div_t结构体
 * - localtime(): 将时间转换为本地时间，返回tm结构体指针
 *
 * @author clazs
 */
public interface MsvcrtLibrary extends Library {

    /**
     * 加载msvcrt.dll库
     * 注意：在不同路径下可能需要指定完整路径
     */
    MsvcrtLibrary INSTANCE = Native.load("msvcrt", MsvcrtLibrary.class);

    /**
     * div函数 - 整数除法
     *
     * C语言原型：
     * div_t div(int numerator, int denominator);
     *
     * 计算numerator/denominator，同时返回商和余数
     * 注意：返回的是结构体值（按值返回），不是指针！
     *
     * @param numerator 被除数
     * @param denominator 除数
     * @return div_t结构体，包含quot(商)和rem(余数)
     */
    DivT.ByValue div(int numerator, int denominator);

    /**
     * ldiv函数 - 长整数除法
     *
     * C语言原型：
     * ldiv_t ldiv(long numerator, long denominator);
     *
     * @param numerator 被除数
     * @param denominator 除数
     * @return ldiv_t结构体（这里用DivT简化演示）
     */
    DivT.ByValue ldiv(int numerator, int denominator);

    /**
     * localtime函数 - 转换为本地时间
     *
     * C语言原型：
     * struct tm* localtime(const time_t* timer);
     *
     * 将time_t时间值转换为本地时间的tm结构体
     * 注意：返回的是指向静态内部缓冲区的指针
     *
     * @param timer 指向time_t值的指针
     * @return 指向tm结构体的指针
     */
    Pointer localtime(Pointer timer);

    /**
     * mktime函数 - 将本地时间转换为时间戳
     *
     * C语言原型：
     * time_t mktime(struct tm* timer);
     *
     * 将tm结构体表示的本地时间转换为time_t值
     * 这是一个接受结构体指针作为参数的典型例子
     *
     * @param tm 指向tm结构体的指针
     * @return 对应的Unix时间戳
     */
    NativeLong mktime(Tm tm);

    /**
     * time函数 - 获取当前时间
     *
     * C语言原型：
     * time_t time(time_t* timer);
     *
     * 获取当前的Unix时间戳（从1970-01-01 00:00:00 UTC开始的秒数）
     * 如果timer不为NULL，也会将值存储到timer指向的位置
     *
     * @param timer 指向time_t的指针，可以为NULL
     * @return 当前时间的时间戳
     */
    NativeLong time(Pointer timer);

    /**
     * malloc函数 - 分配内存
     *
     * C语言原型：
     * void* malloc(size_t size);
     *
     * @param size 要分配的字节数
     * @return 指向分配内存的指针，失败返回NULL
     */
    Pointer malloc(int size);

    /**
     * free函数 - 释放内存
     *
     * C语言原型：
     * void free(void* ptr);
     *
     * @param ptr 要释放的内存指针
     */
    void free(Pointer ptr);
}
