package cn.clazs.jdk.jucapi.completablefuture;

import java.util.concurrent.*;

/**
 * join() vs get() 的区别
 */
public class join与get的区别 {

    public static void main(String[] args) throws Exception {
        System.out.println("=== join() vs get() 的区别 ===\n");

        // 1. join() - 简单，不需要处理异常
        System.out.println("1. join() - 简单直接:");
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "数据1");
        String result1 = future1.join(); // 不需要 try-catch
        System.out.println("   join() 结果: " + result1);
        System.out.println();

        // 2. get() - 麻烦，必须处理异常
        System.out.println("2. get() - 麻烦:");
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "数据2");
        try {
            String result2 = future2.get(); // 必须 try-catch
            System.out.println("   get() 结果: " + result2);
        } catch (InterruptedException e) {
            System.out.println("   线程被中断");
        } catch (ExecutionException e) {
            System.out.println("   任务执行异常");
        }
        System.out.println();

        // 3. 异常情况的区别
        System.out.println("3. 异常情况的区别:");
        CompletableFuture<String> errorFuture = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("出错了！");
        });

        System.out.println("   join() 处理异常:");
        try {
            String result = errorFuture.join(); // 抛出未检查异常 CompletionException
            System.out.println("   结果: " + result);
        } catch (CompletionException e) {
            System.out.println("   异常: " + e.getCause().getMessage());
            System.out.println("   异常类型: " + e.getClass().getSimpleName());
        }

        System.out.println("   get() 处理异常:");
        CompletableFuture<String> errorFuture2 = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("出错了！");
        });
        try {
            String result = errorFuture2.get(); // 抛出检查异常 ExecutionException
            System.out.println("   结果: " + result);
        } catch (InterruptedException e) {
            System.out.println("   线程被中断");
        } catch (ExecutionException e) {
            System.out.println("   异常: " + e.getCause().getMessage());
            System.out.println("   异常类型: " + e.getClass().getSimpleName());
        }

        System.out.println();
        System.out.println("=== 结论 ===");
        System.out.println("✅ join(): 简单，不需要 try-catch");
        System.out.println("❌ get(): 麻烦，必须 try-catch");
        System.out.println("💡 平时开发用 join() 就够了！");
    }
}