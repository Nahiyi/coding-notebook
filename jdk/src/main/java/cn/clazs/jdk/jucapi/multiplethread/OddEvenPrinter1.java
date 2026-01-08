package cn.clazs.jdk.jucapi.multiplethread;

import java.util.ArrayList;

/**
 * 多线程有序依次打印奇偶数1-用synchronized实现
 */
public class OddEvenPrinter1 {
    static volatile int num = 1;
    static final Object obj = new Object();

    public static void main(String[] args) throws InterruptedException {
        ArrayList<Integer> oddPrintList = new ArrayList<>();
        ArrayList<Integer> evenPrintList = new ArrayList<>();
        Thread odd = new Thread(() -> {
            while (num < 100) {
                synchronized (obj) {
                    if ((num & 1) == 1) {
                        oddPrintList.add(num);
                        System.out.println("[" + Thread.currentThread().getName() + "] - " + "print: [" + num + "]");
                        num++;
                    }
                }
            }
            System.out.println("[" + Thread.currentThread().getName() + "] 打印完毕！");
        }, "奇数Thread");

        Thread even = new Thread(() -> {
            while (num < 100) {
                synchronized (obj) {
                    if ((num & 1) == 0) {
                        evenPrintList.add(num);
                        System.out.println("[" + Thread.currentThread().getName() + "] - " + "print: [" + num + "]");
                        num++;
                    }
                }
            }
            System.out.println("[" + Thread.currentThread().getName() + "] 打印完毕！");
        }, "偶数Thread");

        odd.start();
        even.start();

        // 保障结果集收集完毕，否则边add边forEach遍历会有并发修改失败异常
        Thread.sleep(1000);

        System.out.println("\n奇数线程打印结果：size = " + oddPrintList.size());
        oddPrintList.forEach(i -> System.out.print(i + " "));
        System.out.println("\n偶数线程打印结果：size = " + evenPrintList.size());
        evenPrintList.forEach(i -> System.out.print(i + " "));
    }
}
