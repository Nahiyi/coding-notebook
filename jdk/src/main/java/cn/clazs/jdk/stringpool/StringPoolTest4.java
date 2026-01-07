package cn.clazs.jdk.stringpool;

public class StringPoolTest4 {
    public static void main(String[] args) {
        String s1 = new String("1");
        s1.intern();
        String s2 = "1";
        System.out.println(s2 == "1"); // true
    }
}