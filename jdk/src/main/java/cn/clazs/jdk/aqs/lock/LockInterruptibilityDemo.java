package cn.clazs.jdk.aqs.lock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * lock() vs lockInterruptibly() vs tryLock() 对比演示
 *
 * 重点演示：
 * 1. lock() 和 lockInterruptibly() 都是阻塞获取（同步）
 * 2. 区别在于 lockInterruptibly() 可以响应中断
 * 3. tryLock() 是非阻塞获取（异步）
 *
 *         ╔═══════════════════════════════════════════════════════════════╗
 *         ║                    三种获取锁方式对比                             ║
 *         ╠═══════════════════════════════════════════════════════════════╣
 *         ║  方法                  │ 阻塞 │ 可中断 │ 适用场景                  ║
 *         ╠═══════════════════════════════════════════════════════════════╣
 *         ║  lock()               │  是  │   否   │ 标准获取锁               ║
 *         ║  lockInterruptibly()  │  是  │   是   │ 可取消的等待             ║
 *         ║  tryLock()            │  否  │   -    │ 快速尝试，不等待          ║
 *         ╚═══════════════════════════════════════════════════════════════╝
 *
 *         关键点：
 *         • lock() 和 lockInterruptibly() 都是同步阻塞获取
 *         • lockInterruptibly() 可以被中断，lock() 不能
 *         • tryLock() 是异步非阻塞，立即返回
 *
 * @author clazs
 */
public class LockInterruptibilityDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== lock() vs lockInterruptibly() vs tryLock() ==========\n");

        System.out.println("【演示 1】lock() - 阻塞且不响应中断");
        demoLockNotInterruptible();

        System.out.println("\n【演示 2】lockInterruptibly() - 阻塞但可响应中断");
        demoLockInterruptibly();

        System.out.println("\n【演示 3】tryLock() - 非阻塞");
        demoTryLock();
    }

    /**
     * 演示 1: lock() 不响应中断
     */
    private static void demoLockNotInterruptible() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();

        // 主线程先获取锁
        lock.lock();
        System.out.println("主线程持有锁");

        Thread waitingThread = new Thread(() -> {
            System.out.println("  [等待线程] 尝试获取锁（使用 lock()）...");

            // 使用 lock() - 会阻塞且不响应中断
            lock.lock();

            try {
                System.out.println("  [等待线程] 成功获取锁");
            } finally {
                lock.unlock();
            }
        }, "WaitingThread");

        waitingThread.start();
        Thread.sleep(1000); // 让等待线程先执行

        // 尝试中断等待线程
        System.out.println("主线程调用 waitingThread.interrupt()");
        waitingThread.interrupt();

        Thread.sleep(1000);
        System.out.println("等待线程状态: " + waitingThread.getState() + " (仍在阻塞)");
        System.out.println("即使被中断，lock() 也不会停止等待！");

        lock.unlock();
        System.out.println("主线程释放锁");

        Thread.sleep(100);
        System.out.println("等待线程终于获取到锁了\n");
    }

    /**
     * 演示 2: lockInterruptibly() 响应中断
     */
    private static void demoLockInterruptibly() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();

        // 主线程先获取锁
        lock.lock();
        System.out.println("主线程持有锁");

        Thread waitingThread = new Thread(() -> {
            System.out.println("  [等待线程] 尝试获取锁（使用 lockInterruptibly()）...");

            try {
                // 使用 lockInterruptibly() - 阻塞但可响应中断
                lock.lockInterruptibly();
                System.out.println("  [等待线程] 成功获取锁");
                lock.unlock();
            } catch (InterruptedException e) {
                System.out.println("  [等待线程] 被中断，停止等待！抛出 InterruptedException");
                boolean interrupted = Thread.currentThread().isInterrupted();
                System.out.println("interrupted = " + interrupted); // 返回false，中断标志被重置了；interrupt的细节知识
            }
        }, "WaitingThread");

        waitingThread.start();
        Thread.sleep(1000); // 让等待线程先执行

        // 中断等待线程
        System.out.println("主线程调用 waitingThread.interrupt()");
        waitingThread.interrupt();

        Thread.sleep(100);
        System.out.println("等待线程状态: " + waitingThread.getState() + " (已终止)");
        System.out.println("lockInterruptibly() 响应中断，停止等待！");

        lock.unlock();
        System.out.println("主线程释放锁\n");
    }

    /**
     * 演示 3: tryLock() 非阻塞
     */
    private static void demoTryLock() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();

        // 主线程先获取锁
        lock.lock();
        System.out.println("主线程持有锁");

        Thread thread = new Thread(() -> {
            System.out.println("  [子线程] 尝试获取锁（使用 tryLock()）...");

            // 使用 tryLock() - 非阻塞
            boolean acquired = lock.tryLock();

            if (acquired) {
                try {
                    System.out.println("  [子线程] 成功获取锁");
                } finally {
                    lock.unlock();
                }
            } else {
                System.out.println("  [子线程] 获取失败，立即返回（不阻塞）");
                System.out.println("  [子线程] 可以做其他事情，而不是傻等");
            }
        }, "TryLockThread");

        thread.start();
        thread.join();

        System.out.println("tryLock() 不会阻塞，立即返回结果！");

        lock.unlock();
        System.out.println("主线程释放锁\n");
    }
}
