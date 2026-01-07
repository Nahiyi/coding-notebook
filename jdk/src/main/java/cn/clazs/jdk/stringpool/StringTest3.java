package cn.clazs.jdk.stringpool;

public class StringTest3 {
    public static void main(String[] args) {
        String s1 = "hello";
        String s2 = new String("hello");
        System.out.println(s1 == s2); // false

        String s3 = new String("world") + new String("!");
        String s4 = "world!";
        System.out.println(s3 == s4); // false

        s3.intern(); // s4已经导致字符串常量池有东西了，这里不会做任何事情
        String s5 = "world!";
        System.out.println(s3 == s5); // false
    }
}
