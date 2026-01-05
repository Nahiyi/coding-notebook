package cn.clazs.jdk.aqs.latch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * CountDownLatch API 使用演示
 *
 * CountDownLatch 是基于 AQS 实现的同步工具类
 * 用来协调多个线程之间的同步，或者等待多个线程完成
 *
 * 典型应用场景：
 * 1. 主线程等待多个工作线程完成
 * 2. 并行计算：等待所有子任务完成后再汇总结果
 * 3. 多线程初始化：等待所有组件初始化完成
 *
 * @author clazs
 */
public class CountDownLatchDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== CountDownLatch API 演示 ==========\n");

        System.out.println("【演示 1】基本 await/countDown");
        demoBasicAwaitCountDown();

        System.out.println("\n【演示 2】主线程等待多个工作线程");
        demoWaitForWorkers();

        System.out.println("\n【演示 3】并行计算");
        demoParallelComputation();

        System.out.println("\n【演示 4】await 带超时");
        demoAwaitWithTimeout();

        System.out.println("\n【演示 5】getCount 查询剩余计数");
        demoGetCount();

        System.out.println("\n【演示 6】多阶段初始化");
        demoMultiPhaseInitialization();

        System.out.println("\n【演示 7】多任务并发执行");
        demoConcurrentTasks();
    }

    /**
     * 演示 1: 基本的 await/countDown
     */
    private static void demoBasicAwaitCountDown() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        System.out.println("创建计数为 3 的 CountDownLatch");
        System.out.println("初始 count: " + latch.getCount());

        // 启动 3 个线程
        for (int i = 1; i <= 3; i++) {
            final int threadNum = i;
            new Thread(() -> {
                try {
                    Thread.sleep((long) (Math.random() * 1000 + 500));
                    System.out.println("线程 " + threadNum + " 完成工作");
                    latch.countDown();
                    System.out.println("线程 " + threadNum + " 调用 countDown，剩余 count: " + latch.getCount());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, "Worker-" + i).start();
        }

        System.out.println("主线程等待所有工作线程完成...");
        latch.await();
        System.out.println("所有工作线程已完成！count = " + latch.getCount());
    }

    /**
     * 演示 2: 主线程等待多个工作线程
     *
     * 场景：模拟下载多个文件，等待全部下载完成后再进行后续处理
     */
    private static void demoWaitForWorkers() throws InterruptedException {
        final int FILE_COUNT = 5;
        CountDownLatch downloadLatch = new CountDownLatch(FILE_COUNT);

        System.out.println("开始下载 " + FILE_COUNT + " 个文件...");

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= FILE_COUNT; i++) {
            final int fileNum = i;
            Thread downloader = new Thread(() -> {
                try {
                    System.out.println("开始下载文件 " + fileNum + "...");
                    int downloadTime = (int) (Math.random() * 2000 + 1000);
                    Thread.sleep(downloadTime);
                    System.out.println("文件 " + fileNum + " 下载完成，耗时 " + downloadTime + "ms");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    downloadLatch.countDown();
                }
            }, "Downloader-" + i);
            downloader.start();
        }

        // 主线程等待所有下载完成
        downloadLatch.await();

        long endTime = System.currentTimeMillis();
        System.out.println("所有文件下载完成！总耗时: " + (endTime - startTime) + "ms");

        // 继续后续处理
        System.out.println("开始解压和安装文件...");
    }

    /**
     * 演示 3: 并行计算
     *
     * 场景：将大任务分解为多个子任务并行执行，最后汇总结果
     */
    private static void demoParallelComputation() throws InterruptedException {
        final int TASK_COUNT = 4;
        final int[] results = new int[TASK_COUNT];
        CountDownLatch computationLatch = new CountDownLatch(TASK_COUNT);

        System.out.println("开始并行计算，将任务分解为 " + TASK_COUNT + " 个子任务");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TASK_COUNT; i++) {
            final int taskNum = i;
            Thread worker = new Thread(() -> {
                try {
                    System.out.println("子任务 " + taskNum + " 开始计算");
                    // 模拟计算耗时
                    Thread.sleep((long) (Math.random() * 1500 + 500));

                    // 计算结果（这里用随机数模拟）
                    int result = (int) (Math.random() * 100);
                    results[taskNum] = result;
                    System.out.println("子任务 " + taskNum + " 计算完成，结果: " + result);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    computationLatch.countDown();
                }
            }, "Worker-" + i);
            worker.start();
        }

        // 等待所有子任务完成
        computationLatch.await();

        long endTime = System.currentTimeMillis();
        System.out.println("\n所有子任务计算完成！总耗时: " + (endTime - startTime) + "ms");

        // 汇总结果
        int sum = 0;
        System.out.print("各子任务结果: ");
        for (int i = 0; i < TASK_COUNT; i++) {
            System.out.print(results[i] + " ");
            sum += results[i];
        }
        System.out.println("\n总和: " + sum);
    }

    /**
     * 演示 4: await 带超时
     */
    private static void demoAwaitWithTimeout() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        System.out.println("创建计数为 3 的 CountDownLatch");

        // 只启动 1 个线程
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("工作线程完成 1 个任务");
                latch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("主线程等待 2 秒...");
        boolean success = latch.await(2, TimeUnit.SECONDS);

        if (success) {
            System.out.println("所有任务在超时前完成");
        } else {
            System.out.println("等待超时！当前 count: " + latch.getCount());
        }
    }

    /**
     * 演示 5: getCount 查询剩余计数
     */
    private static void demoGetCount() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);

        System.out.println("初始 count: " + latch.getCount());

        // 启动 3 个线程
        for (int i = 0; i < 3; i++) {
            final int threadNum = i;
            new Thread(() -> {
                latch.countDown();
                System.out.println("线程 " + threadNum + " 完成任务，剩余 count: " + latch.getCount());
            }).start();
            Thread.sleep(100);
        }

        Thread.sleep(500);
        System.out.println("当前剩余 count: " + latch.getCount());
    }

    /**
     * 演示 6: 多阶段初始化
     *
     * 场景：应用程序启动时需要等待多个组件初始化完成
     */
    private static void demoMultiPhaseInitialization() throws InterruptedException {
        System.out.println("应用程序启动中...");

        // 第一阶段：加载配置（2 个任务）
        CountDownLatch configLatch = new CountDownLatch(2);
        System.out.println("[阶段 1] 加载配置...");

        new Thread(() -> {
            try {
                Thread.sleep(500);
                System.out.println("  - 数据库配置加载完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                configLatch.countDown();
            }
        }, "ConfigLoader-1").start();

        new Thread(() -> {
            try {
                Thread.sleep(700);
                System.out.println("  - 应用配置加载完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                configLatch.countDown();
            }
        }, "ConfigLoader-2").start();

        configLatch.await();
        System.out.println("[阶段 1] 配置加载完成\n");

        // 第二阶段：初始化服务（3 个任务）
        CountDownLatch serviceLatch = new CountDownLatch(3);
        System.out.println("[阶段 2] 初始化服务...");

        new Thread(() -> {
            try {
                Thread.sleep(800);
                System.out.println("  - 数据库连接池初始化完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                serviceLatch.countDown();
            }
        }, "ServiceInit-1").start();

        new Thread(() -> {
            try {
                Thread.sleep(600);
                System.out.println("  - 缓存服务初始化完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                serviceLatch.countDown();
            }
        }, "ServiceInit-2").start();

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("  - 消息队列初始化完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                serviceLatch.countDown();
            }
        }, "ServiceInit-3").start();

        serviceLatch.await();
        System.out.println("[阶段 2] 服务初始化完成\n");

        // 第三阶段：启动完成
        System.out.println("[阶段 3] 应用程序启动完成，可以处理请求了！");
    }

    /**
     * 演示 7: 多任务并发执行
     */
    private static void demoConcurrentTasks() throws InterruptedException {
        final int TASK_COUNT = 6;
        CountDownLatch startLatch = new CountDownLatch(1); // 发令枪
        CountDownLatch doneLatch = new CountDownLatch(TASK_COUNT); // 完成计数

        System.out.println("准备 " + TASK_COUNT + " 个跑步运动员...");

        // 创建 6 个运动员线程
        for (int i = 1; i <= TASK_COUNT; i++) {
            final int athleteNum = i;
            new Thread(() -> {
                try {
                    // 等待发令枪响
                    System.out.println("运动员 " + athleteNum + " 准备就绪，等待发令枪...");
                    startLatch.await();

                    // 开始跑步
                    System.out.println("运动员 " + athleteNum + " 起跑！");
                    long startTime = System.currentTimeMillis();
                    Thread.sleep((long) (Math.random() * 2000 + 1000));
                    long endTime = System.currentTimeMillis();

                    System.out.println("运动员 " + athleteNum + " 到达终点，用时: " + (endTime - startTime) + "ms");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }, "Athlete-" + i).start();
        }

        Thread.sleep(1000);
        System.out.println("\n裁判：准备...");

        Thread.sleep(500);
        System.out.println("裁判：预备...");
        System.out.println("裁判：跑！\n");
        startLatch.countDown(); // 鸣枪

        // 等待所有运动员到达终点
        doneLatch.await();
        System.out.println("\n所有运动员已完成比赛！");
    }
}
