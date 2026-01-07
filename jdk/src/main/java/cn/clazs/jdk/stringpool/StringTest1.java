package cn.clazs.jdk.stringpool;

public class StringTest1 {
    public static void main(String[] args) {
        String s1 = new String("abc");
        String s2 = "abc";
        System.out.println(s1 == s2); // false

        s1.intern();
        String s3 = "abc";
        System.out.println(s1 == s3); // false
    }
}
