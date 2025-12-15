package cn.clazs.jdklearn.jucapi.completablefuture;

import java.util.concurrent.*;

/**
 * 最简单的 CompletableFuture - join() 的理解
 */
public class join最简单的理解 {

    public static void main(String[] args) throws Exception {
        System.out.println("=== CompletableFuture 最简单的理解 ===\n");

        // 🎯 概念：CompletableFuture = 一个"许诺盒子"

        // 1. 📦 造盒子 + 放入数据生产机器
        System.out.println("1. 创建盒子，里面有个数据生产机器:");
        CompletableFuture<String> box = CompletableFuture.supplyAsync(() -> {
            System.out.println("   🏭 机器开始生产数据...");
            try {
                Thread.sleep(2000); // 生产需要2秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String data = "生产完成的数据";
            System.out.println("   ✅ 机器生产完成，数据: " + data);
            return data; // 👈 数据在这里产生！
        });

        System.out.println("   📦 盒子创建好了，但数据还在生产中...");
        System.out.println("   主线程可以先做其他事\n");

        // 2. ⏳ 等盒子里的数据
        System.out.println("2. 现在我们要等盒子里的数据:");
        System.out.println("   🤚 调用 join() - 伸手进盒子拿数据...");

        // 🚨 关键点：join()会卡住，直到数据生产完成
        String data = box.join(); // 👈 这里BLOCK住等数据！

        System.out.println("   🎉 拿到数据了: " + data);
        System.out.println("   👉 join() 返回的就是 supplyAsync 里面 return 的数据！\n");

        // 3. 🔄 对比：completedFuture
        System.out.println("3. 对比：completedFuture - 盒子已经有数据了:");
        CompletableFuture<String> readyBox = CompletableFuture.completedFuture("现成的数据");
        String readyData = readyBox.join(); // 立即返回，不用等
        System.out.println("   立即拿到: " + readyData + "\n");

        // 4. 🚫 runAsync - 盒子只做事情，不生产数据
        System.out.println("4. runAsync - 盒子只做事情，不返回数据:");
        CompletableFuture<Void> actionBox = CompletableFuture.runAsync(() -> {
            System.out.println("   🔨 盒子里的人在做事...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("   ✅ 事情做完了，但不生产数据");
        });

        System.out.println("   🤚 等他做完事...");
        actionBox.join(); // 等事情做完，但返回null
        System.out.println("   👉 事情做完了\n");

        // 5. 📋 总结
        System.out.println("=== 总结 ===");
        System.out.println("📦 CompletableFuture = 盒子");
        System.out.println("🏭 supplyAsync(() -> { return 数据; }) = 放数据生产机器");
        System.out.println("🤚 join() = 伸手进盒子等数据拿来");
        System.out.println("📋 runAsync(() -> { 做事 }) = 盒子里只做事，不生产数据");
    }
}