package cn.clazs.jdklearn.jucapi.completablefuture;

import java.util.concurrent.*;

/**
 * runAsync 能否调用 join()？
 */
public class runAsync能否调用join {

    public static void main(String[] args) throws Exception {
        System.out.println("=== runAsync 能否调用 join()？ ===\n");

        // ❓ 问题：runAsync 没有返回值，还能调用 join() 吗？

        // 1. runAsync 创建的 CompletableFuture<Void>
        System.out.println("1. runAsync 返回的类型:");
        CompletableFuture<Void> runFuture = CompletableFuture.runAsync(() -> {
            System.out.println("   🔨 做事情中...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("   ✅ 事情做完了");
        });
        System.out.println("   类型: " + runFuture.getClass().getSimpleName());
        System.out.println();

        // 2. runAsync 确实可以调用 join()
        System.out.println("2. runAsync 调用 join():");
        System.out.println("   等待任务完成...");

        Void result = runFuture.join(); // 🎯 可以调用！
        System.out.println("   join() 返回: " + result); // 总是 null
        System.out.println();

        // 3. 对比 supplyAsync 的 join()
        System.out.println("3. 对比 supplyAsync 的 join():");
        CompletableFuture<String> supplyFuture = CompletableFuture.supplyAsync(() -> {
            return "实际数据";
        });
        String data = supplyFuture.join();
        System.out.println("   supplyAsync join() 返回: " + data);
        System.out.println();

        // 4. 总结规则
        System.out.println("=== 总结 ===");
        System.out.println("✅ runAsync() + join(): 可以！");
        System.out.println("   - join() 等待任务完成");
        System.out.println("   - join() 返回 null (因为 Void)");
        System.out.println("   - 作用：确保任务执行完成");
        System.out.println();
        System.out.println("✅ supplyAsync() + join(): 当然可以！");
        System.out.println("   - join() 等待任务完成");
        System.out.println("   - join() 返回实际数据");
        System.out.println("   - 作用：获取任务结果");
        System.out.println();
        System.out.println("🎯 核心结论：");
        System.out.println("   任何 CompletableFuture 都能调用 join()");
        System.out.println("   区别只在于返回值：有数据 vs null");
    }
}