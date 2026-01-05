package cn.clazs.jdk.aqs.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

/**
 * ReentrantLock API 使用演示
 *
 * ReentrantLock 是基于 AQS 实现的可重入互斥锁
 * 相比 synchronized，它提供了更灵活的锁机制
 *
 * @author clazs
 */
public class ReentrantLockDemo {

    private static final Lock lock = new ReentrantLock();
    private static int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== ReentrantLock API 演示 ==========\n");

        System.out.println("【演示 1】基本 lock/unlock");
        demoBasicLockUnlock();

        System.out.println("\n【演示 2】可重入性验证");
        demoReentrant();

        System.out.println("\n【演示 3】tryLock 非阻塞尝试获取");
        demoTryLock();

        System.out.println("\n【演示 4】tryLock 带超时");
        demoTryLockWithTimeout();

        System.out.println("\n【演示 5】lockInterruptibly 可中断获取锁");
        demoLockInterruptibly();

        System.out.println("\n【演示 6】Condition 条件变量");
        demoCondition();

        System.out.println("\n【演示 7】公平锁 vs 非公平锁");
        demoFairVsUnfair();

        System.out.println("\n【演示 8】多线程竞争");
        demoMultiThreadContention();
    }

    /**
     * 演示 1: 基本的 lock/unlock
     */
    private static void demoBasicLockUnlock() {
        System.out.println("使用 lock.lock() 获取锁，lock.unlock() 释放锁");

        lock.lock();
        try {
            System.out.println("获取锁成功，执行临界区代码");
            counter++;
            System.out.println("counter = " + counter);
        } finally {
            System.out.println("释放锁");
            lock.unlock();
        }
    }

    /**
     * 演示 2: 可重入性验证
     * 同一个线程可以多次获取同一个锁
     */
    private static void demoReentrant() {
        ReentrantLock reentrantLock = new ReentrantLock();

        System.out.println("第 1 次获取锁");
        reentrantLock.lock();
        try {
            System.out.println("  重入次数: " + reentrantLock.getHoldCount());

            System.out.println("第 2 次获取锁（重入）");
            reentrantLock.lock();
            try {
                System.out.println("  重入次数: " + reentrantLock.getHoldCount());

                System.out.println("第 3 次获取锁（重入）");
                reentrantLock.lock();
                try {
                    System.out.println("  重入次数: " + reentrantLock.getHoldCount());
                } finally {
                    System.out.println("释放 1 次，重入次数: " + reentrantLock.getHoldCount());
                    reentrantLock.unlock();
                }
            } finally {
                System.out.println("再释放 1 次，重入次数: " + reentrantLock.getHoldCount());
                reentrantLock.unlock();
            }
        } finally {
            System.out.println("最后释放 1 次，重入次数: " + reentrantLock.getHoldCount());
            reentrantLock.unlock();
        }
    }

    /**
     * 演示 3: tryLock 非阻塞尝试获取锁
     */
    private static void demoTryLock() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();

        lock.lock();
        try {
            System.out.println("主线程已持有锁");

            // 另一个线程尝试获取锁
            Thread thread = new Thread(() -> {
                boolean acquired = lock.tryLock();
                if (acquired) {
                    try {
                        System.out.println("  子线程获取锁成功");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    System.out.println("  子线程 tryLock 失败（非阻塞）");
                }
            });

            thread.start();
            thread.join();

        } finally {
            lock.unlock();
        }
    }

    /**
     * 演示 4: tryLock 带超时
     */
    private static void demoTryLockWithTimeout() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();

        lock.lock();
        try {
            System.out.println("主线程已持有锁");

            Thread thread = new Thread(() -> {
                try {
                    System.out.println("  子线程尝试在 2 秒内获取锁...");
                    boolean acquired = lock.tryLock(2, TimeUnit.SECONDS);

                    if (acquired) {
                        try {
                            System.out.println("  子线程在超时前获取锁成功");
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        System.out.println("  子线程 tryLock 超时失败");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            thread.start();

            // 主线程持有 3 秒
            Thread.sleep(3000);

            lock.unlock();
            System.out.println("主线程释放锁");
            thread.join();

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 演示 5: lockInterruptibly 可中断获取锁
     */
    private static void demoLockInterruptibly() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();

        lock.lock();
        try {
            System.out.println("主线程已持有锁");

            Thread thread = new Thread(() -> {
                try {
                    System.out.println("  子线程尝试获取锁（可中断）...");
                    lock.lockInterruptibly(); // 可以响应中断
                    System.out.println("  子线程获取锁成功");
                    lock.unlock();
                } catch (InterruptedException e) {
                    System.out.println("  子线程被中断，退出等待");
                }
            });

            thread.start();

            // 1 秒后中断子线程
            Thread.sleep(1000);
            System.out.println("主线程中断子线程");
            thread.interrupt();

            thread.join();

        } finally {
            lock.unlock();
        }
    }

    /**
     * 演示 6: Condition 条件变量
     * 类似于 wait/notify，但更灵活
     */
    private static void demoCondition() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        System.out.println("演示生产者-消费者模式");

        // 消费者线程
        Thread consumer = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("  [消费者] 等待数据...");
                condition.await(); // 释放锁并等待
                System.out.println("  [消费者] 被唤醒，处理数据");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "Consumer");

        // 生产者线程
        Thread producer = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("  [生产者] 准备数据中...");
                Thread.sleep(1000);
                System.out.println("  [生产者] 数据准备完毕，唤醒消费者");
                condition.signal(); // 唤醒一个等待线程
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "Producer");

        consumer.start();
        Thread.sleep(100);
        producer.start();

        consumer.join();
        producer.join();
    }

    /**
     * 演示 7: 公平锁 vs 非公平锁
     */
    private static void demoFairVsUnfair() throws InterruptedException {
        System.out.println("公平锁：严格按照请求时间顺序获取锁");
        System.out.println("非公平锁：允许插队，性能更好（默认）");

        // 公平锁
        ReentrantLock fairLock = new ReentrantLock(true);
        System.out.println("\n公平锁测试：");
        testLockType(fairLock, "公平");

        // 非公平锁（默认）
        ReentrantLock unfairLock = new ReentrantLock(false);
        System.out.println("\n非公平锁测试：");
        testLockType(unfairLock, "非公平");
    }

    private static void testLockType(ReentrantLock lock, String type) throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            final int threadNum = i;
            new Thread(() -> {
                lock.lock();
                try {
                    System.out.println("  [" + type + "锁] 线程 " + threadNum + " 获取锁");
                } finally {
                    lock.unlock();
                }
            }).start();
            Thread.sleep(50);
        }

        Thread.sleep(1000);
    }

    /**
     * 演示 8: 多线程竞争
     */
    private static void demoMultiThreadContention() throws InterruptedException {
        counter = 0;
        int threadCount = 5;

        System.out.println("启动 " + threadCount + " 个线程竞争锁");

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                System.out.println("线程 " + threadNum + " 尝试获取锁...");
                lock.lock();
                try {
                    System.out.println("线程 " + threadNum + " 获取锁成功");
                    Thread.sleep(100);
                    counter++;
                    System.out.println("线程 " + threadNum + " 执行完毕，counter = " + counter);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("线程 " + threadNum + " 释放锁");
                    lock.unlock();
                }
            }, "Thread-" + i);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("最终 counter 值: " + counter);
        System.out.println("预期值: " + threadCount + "（证明锁有效保证了线程安全）");
    }
}
