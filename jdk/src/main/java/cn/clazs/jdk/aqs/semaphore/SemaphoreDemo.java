package cn.clazs.jdk.aqs.semaphore;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Semaphore API 使用演示
 *
 * Semaphore 是基于 AQS 实现的信号量
 * 用于控制同时访问特定资源的线程数量
 *
 * @author clazs
 */
public class SemaphoreDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== Semaphore API 演示 ==========\n");

        System.out.println("【演示 1】基本 acquire/release");
        demoBasicAcquireRelease();

        System.out.println("\n【演示 2】资源限流（连接池模拟）");
        demoConnectionPool();

        System.out.println("\n【演示 3】tryAcquire 非阻塞获取");
        demoTryAcquire();

        System.out.println("\n【演示 4】tryAcquire 带超时");
        demoTryAcquireWithTimeout();

        System.out.println("\n【演示 5】acquire 获取多个许可");
        demoAcquireMultiplePermits();

        System.out.println("\n【演示 6】drainPermits 获取所有剩余许可");
        demoDrainPermits();
    }

    /**
     * 演示 1: 基本的 acquire/release
     */
    private static void demoBasicAcquireRelease() throws InterruptedException {
        Semaphore semaphore = new Semaphore(3);

        System.out.println("初始可用许可数: " + semaphore.availablePermits());

        System.out.println("获取 1 个许可...");
        semaphore.acquire();
        System.out.println("获取成功，剩余许可: " + semaphore.availablePermits());

        System.out.println("再获取 2 个许可...");
        semaphore.acquire(2);
        System.out.println("获取成功，剩余许可: " + semaphore.availablePermits());

        System.out.println("释放 1 个许可...");
        semaphore.release();
        System.out.println("释放成功，剩余许可: " + semaphore.availablePermits());
    }

    /**
     * 演示 2: 资源限流 - 数据库连接池模拟
     *
     * 场景：假设只有 3 个数据库连接，但有 10 个线程需要使用
     */
    private static void demoConnectionPool() throws InterruptedException {
        final int MAX_CONNECTIONS = 3; // 最大连接数
        final int TOTAL_CLIENTS = 10;  // 总客户端数

        Semaphore connectionSemaphore = new Semaphore(MAX_CONNECTIONS);

        System.out.println("数据库连接池大小: " + MAX_CONNECTIONS);
        System.out.println("启动 " + TOTAL_CLIENTS + " 个客户端请求连接\n");

        for (int i = 1; i <= TOTAL_CLIENTS; i++) {
            final int clientId = i;
            Thread client = new Thread(() -> {
                try {
                    // 获取连接（许可）
                    System.out.println("客户端 " + clientId + " 请求获取连接 [当前可用连接: " + connectionSemaphore.availablePermits() + "]");

                    // 请求一个信号量，请求不到的话，线程会阻塞在这里
                    connectionSemaphore.acquire();

                    System.out.println("客户端 " + clientId + " 成功获取连接，执行查询... [剩余可用连接: " + connectionSemaphore.availablePermits() + "]");

                    // 模拟查询执行时间
                    Thread.sleep((long) (Math.random() * 1000 + 500));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    // 释放连接（许可）
                    connectionSemaphore.release();
                    System.out.println("客户端 " + clientId + " 查询完成，即将释放连接..." +
                            "  [连接已释放，当前可用: " + connectionSemaphore.availablePermits() + "]");
                }
            }, "Client-" + i);

            client.start();
            Thread.sleep(200);
        }

        // 等待所有客户端完成
        Thread.sleep(10_000);
        System.out.println("\n所有客户端执行完毕");
    }

    /**
     * 演示 3: tryAcquire 非阻塞获取
     */
    private static void demoTryAcquire() throws InterruptedException {
        Semaphore semaphore = new Semaphore(1);

        System.out.println("初始许可: " + semaphore.availablePermits());

        // 主线程先获取
        semaphore.acquire();
        System.out.println("主线程获取许可，剩余: " + semaphore.availablePermits());

        // 子线程尝试获取
        Thread thread = new Thread(() -> {
            boolean success = semaphore.tryAcquire();
            if (success) {
                try {
                    System.out.println("  子线程获取许可成功");
                } finally {
                    semaphore.release();
                }
            } else {
                System.out.println("  子线程 tryAcquire 失败（非阻塞）");
            }
        });

        thread.start();
        thread.join();

        semaphore.release();
        System.out.println("主线程释放许可");
    }

    /**
     * 演示 4: tryAcquire 带超时
     */
    private static void demoTryAcquireWithTimeout() throws InterruptedException {
        Semaphore semaphore = new Semaphore(1);

        System.out.println("主线程获取许可...");
        semaphore.acquire();
        System.out.println("剩余许可: " + semaphore.availablePermits());

        Thread thread = new Thread(() -> {
            try {
                System.out.println("  子线程尝试在 2 秒内获取许可...");
                boolean success = semaphore.tryAcquire(2, TimeUnit.SECONDS);

                if (success) {
                    System.out.println("  子线程在超时前获取许可成功");
                    semaphore.release();
                } else {
                    System.out.println("  子线程 tryAcquire 超时失败");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        thread.start();

        // 主线程持有 3 秒
        Thread.sleep(3000);

        semaphore.release();
        System.out.println("主线程释放许可");
        thread.join();
    }

    /**
     * 演示 5: acquire 获取多个许可
     */
    private static void demoAcquireMultiplePermits() throws InterruptedException {
        Semaphore semaphore = new Semaphore(5);

        System.out.println("初始许可: " + semaphore.availablePermits());

        System.out.println("获取 3 个许可...");
        semaphore.acquire(3);
        System.out.println("获取成功，剩余许可: " + semaphore.availablePermits());

        System.out.println("释放 2 个许可...");
        semaphore.release(2);
        System.out.println("释放成功，剩余许可: " + semaphore.availablePermits());
    }

    /**
     * 演示 6: drainPermits 获取所有剩余许可
     */
    private static void demoDrainPermits() {
        Semaphore semaphore = new Semaphore(5);

        System.out.println("初始许可: " + semaphore.availablePermits());

        // 先获取 2 个
        semaphore.acquireUninterruptibly(2);
        System.out.println("已获取 2 个，剩余: " + semaphore.availablePermits());

        System.out.println("获取所有剩余许可...");
        int drained = semaphore.drainPermits();
        System.out.println("成功获取 " + drained + " 个许可");

        System.out.println("当前可用许可: " + semaphore.availablePermits());

        // 释放所有许可
        System.out.println("释放所有许可...");
        semaphore.release(5);
        System.out.println("释放后可用许可: " + semaphore.availablePermits());
    }
}
