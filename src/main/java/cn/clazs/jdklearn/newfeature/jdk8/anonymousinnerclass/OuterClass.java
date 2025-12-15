package cn.clazs.jdklearn.newfeature.jdk8.anonymousinnerclass;

public class OuterClass {
    public static void main(String[] args) {
        OuterClass oc = new OuterClass();
        oc.test();
    }

    public void test() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                System.out.println("匿名内部类执行");
            }
        };
        new Thread(task).start();
    }
}