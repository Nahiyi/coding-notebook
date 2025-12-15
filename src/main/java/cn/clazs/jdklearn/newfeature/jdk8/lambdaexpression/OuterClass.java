package cn.clazs.jdklearn.newfeature.jdk8.lambdaexpression;

import java.util.ArrayList;
import java.util.List;

/**
 * lambda 表达式简化的匿名内部类的最终编译的class只有一个
 * 反编译结果也和原类一样
 * 方法引用本质也是 lambda 表达式，所以结果一样
 */
public class OuterClass {
    public static void main(String[] args) {
        OuterClass oc = new OuterClass();
        oc.test();
    }

    public void test() {
        List<Integer> demo = new ArrayList<>();
        demo.add(1);
        demo.add(2);

        demo.forEach(System.out::println);
    }
}
