package cn.clazs.jdk.aqs.lock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 公平锁 vs 非公平锁演示
 *
 * 核心区别：
 * - 公平锁（Fair）：严格按照请求锁的先后顺序获取（FIFO）
 * - 非公平锁（Unfair）：允许插队，性能更好（默认）
 *
 *         ╔═══════════════════════════════════════════════════════════════╗
 *         ║                    公平锁 vs 非公平锁对比                         ║
 *         ╠═══════════════════════════════════════════════════════════════╣
 *         ║  特性              │ 公平锁          │ 非公平锁（默认）            ║
 *         ╠═══════════════════════════════════════════════════════════════╣
 *         ║  获取顺序           │ 严格 FIFO       │ 允许插队                  ║
 *         ║  新线程行为          │ 加入队列尾部      │ 先尝试直接获取             ║
 *         ║  吞吐量             │ 较低            │ 较高                     ║
 *         ║  线程饥饿风险        │ 无              │ 可能出现                  ║
 *         ║  适用场景           │ 需要严格顺序      │ 追求性能                  ║
 *         ║  构造方式           │ new(true)       │ new() 或 new(false)     ║
 *         ╚═══════════════════════════════════════════════════════════════╝
 *
 *         关键点：
 *         • ReentrantLock 默认是非公平锁（性能考虑）
 *         • 公平锁保证顺序，但性能略低
 *         • 非公平锁允许插队，性能更好，但可能导致线程饥饿
 *         • 大多数场景使用非公平锁即可
 *
 * @author clazs
 */
public class FairVsUnfairLockDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 公平锁 vs 非公平锁对比 ==========\n");

        System.out.println("【演示 1】非公平锁（默认）- 允许插队");
        demoUnfairLock();

        System.out.println("\n【演示 2】公平锁 - 严格 FIFO");
        demoFairLock();

        System.out.println("\n【演示 3】性能对比");
        demoPerformanceComparison();
    }

    /**
     * 演示 1: 非公平锁（默认）
     *
     * 特点：
     * - 新线程获取锁时，会先尝试直接获取（插队）
     * - 如果失败，再按队列顺序等待
     * - 吞吐量通常更高
     */
    private static void demoUnfairLock() throws InterruptedException {
        // 无参构造 = 非公平锁（默认）
        ReentrantLock unfairLock = new ReentrantLock();
        // 或者显式指定：new ReentrantLock(false)

        System.out.println("创建非公平锁: new ReentrantLock()");
        System.out.println("锁的类型: " + (unfairLock.isFair() ? "公平" : "非公平") + "锁\n");

        // 主线程先持有锁
        unfairLock.lock();
        System.out.println("主线程持有锁");

        // 启动 3 个等待线程
        for (int i = 1; i <= 3; i++) {
            final int threadNum = i;
            Thread thread = new Thread(() -> {
                System.out.println("  线程 " + threadNum + " 尝试获取锁");
                unfairLock.lock();
                try {
                    System.out.println("  线程 " + threadNum + " 获取锁成功");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("  线程 " + threadNum + " 释放锁");
                    unfairLock.unlock();
                }
            }, "Thread-" + i);
            thread.start();
            Thread.sleep(50); // 保证顺序启动
        }

        Thread.sleep(200);
        System.out.println("\n主线程准备释放锁，观察哪个线程先获取到...");

        // 在释放锁之前，启动一个"插队"线程
        Thread 插队线程 = new Thread(() -> {
            System.out.println("  [插队线程] 尝试获取锁（非公平锁允许插队）");
            unfairLock.lock();
            try {
                System.out.println("  [插队线程] 获取锁成功！可能比等待队列中的线程先获得");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println("  [插队线程] 释放锁");
                unfairLock.unlock();
            }
        }, "插队线程");
        插队线程.start();

        Thread.sleep(50);
        unfairLock.unlock();
        System.out.println("主线程释放锁\n");

        Thread.sleep(1000);
        System.out.println("非公平锁演示：注意观察插队线程可能先于等待队列中的线程获取锁\n");
    }

    /**
     * 演示 2: 公平锁
     *
     * 特点：
     * - 严格按照请求锁的先后顺序获取
     * - 新线程必须加入队列尾部等待
     * - 不会出现插队现象
     */
    private static void demoFairLock() throws InterruptedException {
        // 创建公平锁
        ReentrantLock fairLock = new ReentrantLock(true);

        System.out.println("创建公平锁: new ReentrantLock(true)");
        System.out.println("锁的类型: " + (fairLock.isFair() ? "公平" : "非公平") + "锁\n");

        // 主线程先持有锁
        fairLock.lock();
        System.out.println("主线程持有锁");

        // 启动 3 个等待线程
        for (int i = 1; i <= 3; i++) {
            final int threadNum = i;
            Thread thread = new Thread(() -> {
                System.out.println("  线程 " + threadNum + " 尝试获取锁");
                fairLock.lock();
                try {
                    System.out.println("  线程 " + threadNum + " 获取锁成功");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("  线程 " + threadNum + " 释放锁");
                    fairLock.unlock();
                }
            }, "Thread-" + i);
            thread.start();
            Thread.sleep(50); // 保证顺序启动
        }

        Thread.sleep(200);
        System.out.println("\n主线程准备释放锁，观察哪个线程先获取到...");

        // 在释放锁之前，启动一个新线程
        Thread 新线程 = new Thread(() -> {
            System.out.println("  [新线程] 尝试获取锁（公平锁必须排队）");
            fairLock.lock();
            try {
                System.out.println("  [新线程] 获取锁成功！但必须在等待队列中的线程之后");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println("  [新线程] 释放锁");
                fairLock.unlock();
            }
        }, "新线程");
        新线程.start();

        Thread.sleep(50);
        fairLock.unlock();
        System.out.println("主线程释放锁\n");

        Thread.sleep(1000);
        System.out.println("公平锁演示：新线程必须在队列末尾等待，严格按照 FIFO 顺序\n");
    }

    /**
     * 演示 3: 性能对比
     *
     * 非公平锁通常性能更好，因为：
     * 1. 减少了线程挂起和恢复的开销（插队直接获取）
     * 2. 避免了唤醒等待线程的开销
     */
    private static void demoPerformanceComparison() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final int OPERATIONS_PER_THREAD = 1000;

        System.out.println("性能对比测试：每个线程执行 " + OPERATIONS_PER_THREAD + " 次加锁/解锁操作\n");

        // 测试非公平锁
        ReentrantLock unfairLock = new ReentrantLock(false);
        long unfairStart = System.nanoTime();

        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    unfairLock.lock();
                    try {
                        // 模拟临界区操作
                    } finally {
                        unfairLock.unlock();
                    }
                }
            }).start();
        }

        Thread.sleep(2000); // 等待所有线程完成
        long unfairTime = System.nanoTime() - unfairStart;

        // 测试公平锁
        ReentrantLock fairLock = new ReentrantLock(true);
        long fairStart = System.nanoTime();

        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    fairLock.lock();
                    try {
                        // 模拟临界区操作
                    } finally {
                        fairLock.unlock();
                    }
                }
            }).start();
        }

        Thread.sleep(2000); // 等待所有线程完成
        long fairTime = System.nanoTime() - fairStart;

        System.out.println("结果：");
        System.out.println("  非公平锁耗时: " + (unfairTime / 1_000_000) + " ms");
        System.out.println("  公平锁耗时:   " + (fairTime / 1_000_000) + " ms");
        System.out.println("\n结论：非公平锁通常性能更好（减少线程挂起/恢复开销）");
    }
}
