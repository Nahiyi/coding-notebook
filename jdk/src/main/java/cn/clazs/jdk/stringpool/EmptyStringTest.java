package cn.clazs.jdk.stringpool;

public class EmptyStringTest {
    public static void main(String[] args) {
        // 测试1：空字符串字面量
        String s1 = "";
        String s2 = "";
        System.out.println(s1 == s2); // true 都指向常量池中的同一个空字符串
        
        // 测试2：new String("") 与字面量
        String s3 = new String("");
        String s4 = "";
        System.out.println(s3 == s4); // false s3在堆中，s4在常量池
        
        // 测试3：intern() 方法
        s3.intern();
        String s5 = "";
        System.out.println(s3 == s5); // false s3.intern()返回常量池引用但s3本身不变
        
        // 测试4：intern() 返回值比较
        String s6 = s3.intern();
        System.out.println(s6 == s4); // true intern()返回常量池引用
        System.out.println(s6 == s5); // true
        
        // 测试5：字符串拼接产生的空字符串
        String s7 = new String("") + new String("");
        System.out.println(s7 == ""); // false 拼接产生的新对象
        String s7Intern = s7.intern();
        String s8 = "";
        System.out.println(s7 == s8); // false s7还是String的引用，s8是字符串字面量引用
        System.out.println(s7Intern == s8); // true 因为s7.intern()返回了池中的空串（如果没有，就会在池中创建，只不过这里本身就有）
    }
}
