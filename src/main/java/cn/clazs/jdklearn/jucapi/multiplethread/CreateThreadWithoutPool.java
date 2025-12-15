package cn.clazs.jdklearn.jucapi.multiplethread;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * 三种不利用线程池实现多线程的方式
 */
public class CreateThreadWithoutPool {
    public static void main(String[] args) throws Exception {
        // 最基础的继承Thread然后重写run方法，然后创建本类对象new然后start
        // method1();

        // 使用Runnable封装任务
        // method2();

        // 使用Callable，有两种思路
        // 一种是借助线程池的submit方法（或者execute也行，只不过拿不到返回值了）
        // 一种是手动将Callable封装为FutureTask类型，然后还是作为“Runnable”传给Thread执行（但是区别于Runnable的是我们可以手动获取结果）
        // 后者本质上就是手动简化实现了线程池的内部逻辑
        method3();
    }

    private static void method3() throws ExecutionException, InterruptedException {
        Callable task = () -> {
            int cnt = 50;
            while (cnt >= 2) {
                cnt--;
                LocalDateTime now = LocalDateTime.now();
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("now = " + now.toString().replace("T", " "));
            }
            return "成功结束";
        };

        RunnableFuture<String> futureTask = new FutureTask<String>(task);
        Thread t = new Thread(futureTask);
        System.out.println("Begin:");
        t.start();

        String result = futureTask.get();
        // t线程结束之后才拿到结果
        System.out.println("线程状态: " + t.getState()); // TERMINATED
        System.out.println("futureTask 的执行结果 = " + result);
    }

    private static void method1() {
        SonOfThread sonOfThread = new SonOfThread();
        sonOfThread.start();
    }

    private static void method2() {
        Runnable task = () -> {
            int cnt = 101;
            while (cnt >= 1) {
                cnt--;
                LocalDateTime now = LocalDateTime.now();
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("now = " + now.toString().replace("T", " "));
            }
        };
        Thread t1 = new Thread(task);
        t1.start();
    }
}
