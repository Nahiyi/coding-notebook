package cn.clazs.jdk;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class IteratorDifferenceDemo {

    public static void main(String[] args) throws InterruptedException {
        // --- 1. 演示 Fail-Fast (会报错) ---
        System.out.println("========== 测试 1: Collections.synchronizedList (Fail-Fast) ==========");
        testFailFast();

        // 为了防止控制台输出混杂，主线程睡一会儿，等上面那个报错报完
        Thread.sleep(2000); 
        System.out.println("\n\n");

        // --- 2. 演示 Fail-Safe (不会报错) ---
        System.out.println("========== 测试 2: CopyOnWriteArrayList (Fail-Safe) ==========");
        testFailSafe();
    }

    /**
     * 场景 A：使用 Collections.synchronizedList
     * 结果：虽然它是线程安全的容器，但迭代器是 Fail-Fast 的，检测到修改会抛异常。
     */
    public static void testFailFast() {
        // 初始化数据
        List<String> list = Collections.synchronizedList(new ArrayList<>(Arrays.asList("A", "B", "C", "D")));

        // 线程 1：负责遍历 (读)
        new Thread(() -> {
            try {
                // 使用 for-each (底层就是迭代器)
                for (String s : list) {
                    System.out.println("[读线程] 正在读取: " + s);
                    // 故意睡一会儿，让写线程有机会插队
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            } catch (ConcurrentModificationException e) {
                System.err.println(">>>>> [读线程] 崩溃了！捕获到异常: ConcurrentModificationException");
                System.err.println(">>>>> 原因：迭代器发现 modCount 变了 (有人动了数据)！");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        // 线程 2：负责修改 (写)
        new Thread(() -> {
            try {
                // 稍微等一下，确保读线程已经开始遍历了
                TimeUnit.MILLISECONDS.sleep(50);
                // 突然插入一个数据
                list.add("插队的数据");
                System.out.println("[写线程] -> 成功插入了 '插队的数据' (修改了底层 list)");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 场景 B：使用 CopyOnWriteArrayList
     * 结果：迭代器是 Fail-Safe 的 (基于快照)，完全不管别人的修改。
     */
    public static void testFailSafe() {
        // 初始化数据
        List<String> list = new CopyOnWriteArrayList<>(Arrays.asList("1", "2", "3", "4"));

        // 线程 1：负责遍历 (读)
        new Thread(() -> {
            try {
                // 使用 for-each (底层是 COWIterator)
                for (String s : list) {
                    System.out.println("[读线程] 正在读取: " + s);
                    // 故意睡一会儿
                    TimeUnit.MILLISECONDS.sleep(100);
                }
                System.out.println("[读线程] 遍历结束！(注意：你没读到 '新数据'，因为你读的是旧照片)");
            } catch (Exception e) {
                // 这里绝对不会执行
                System.err.println("崩溃了？不可能！");
                e.printStackTrace();
            }
        }).start();

        // 线程 2：负责修改 (写)
        new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
                // 突然插入一个数据
                list.add("新数据5");
                System.out.println("[写线程] -> 成功插入了 '新数据5' (但在新数组里)");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}