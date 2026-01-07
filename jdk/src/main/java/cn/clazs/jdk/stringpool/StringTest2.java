package cn.clazs.jdk.stringpool;

public class StringTest2 {
    public static void main(String[] args) {
        String s1 = new String("xyz") + new String("xyz");
        s1.intern();
        String s2 = "xyzxyz";
        System.out.println(s1 == s2); // true

        String s3 = new String("uvw");
        String s4 = "uvw";
        s3.intern();
        System.out.println(s3 == s4); // false
    }
}
