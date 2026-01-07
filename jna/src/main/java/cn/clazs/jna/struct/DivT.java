package cn.clazs.jna.struct;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import java.util.Arrays;
import java.util.List;

/**
 * JNA映射C语言的div_t结构体
 *
 * C语言定义：
 * typedef struct {
 *     int quot;  // 商
 *     int rem;   // 余数
 * } div_t;
 *
 * @author clazs
 */
@FieldOrder({"quot", "rem"})
public class DivT extends Structure {

    /**
     * 商 (quotient)
     */
    public int quot;

    /**
     * 余数 (remainder)
     */
    public int rem;

    /**
     * 按值传递的内部类
     * 用于将结构体按值（而不是指针）传递给native函数
     */
    public static class ByValue extends DivT implements Structure.ByValue {
    }

    /**
     * 按引用传递的内部类
     * 显式指定按引用（指针）传递
     */
    public static class ByReference extends DivT implements Structure.ByReference {
    }

    // @Override
    // protected List<String> getFieldOrder() {
    //     return Arrays.asList("quot", "rem");
    // }

    @Override
    public String toString() {
        return String.format("DivT{quot=%d, rem=%d}", quot, rem);
    }
}
