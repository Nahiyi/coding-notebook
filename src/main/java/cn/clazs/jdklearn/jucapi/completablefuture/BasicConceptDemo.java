package cn.clazs.jdklearn.jucapi.completablefuture;

import java.util.concurrent.CompletableFuture;

/**
 * CompletableFuture 基础概念演示
 * 展示继承关系和核心思想
 */
public class BasicConceptDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== CompletableFuture 核心思想演示 ===\n");

        // 1. 作为Future的结果容器能力
        demonstrateFutureInterface();

        // 2. 作为CompletionStage的流水线编排能力
        demonstrateCompletionStageInterface();

        // 3. 主动完成能力（Completable）
        demonstrateCompletableCapability();

        // 4. 构建异步计算图
        demonstrateAsyncComputationGraph();
    }

    /**
     * 演示作为Future接口的能力
     */
    private static void demonstrateFutureInterface() throws Exception {
        System.out.println("1. Future接口能力 - 作为结果容器:");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
                return "任务完成";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // 传统的Future能力
        System.out.println("   - 是否完成: " + future.isDone());
        System.out.println("   - 阻塞获取结果: " + future.get());
        System.out.println("   - 现在是否完成: " + future.isDone());
        System.out.println();
    }

    /**
     * 演示CompletionStage接口的流水线编排能力
     */
    private static void demonstrateCompletionStageInterface() {
        System.out.println("2. CompletionStage接口能力 - 流水线编排:");

        CompletableFuture.supplyAsync(() -> "原始数据")
            .thenApply(data -> {
                System.out.println("   - 第一步处理: " + data);
                return data + " -> 加工后";
            })
            .thenApply(processed -> {
                System.out.println("   - 第二步处理: " + processed);
                return processed + " -> 最终结果";
            })
            .thenAccept(result -> System.out.println("   - 最终输出: " + result))
            .thenRun(() -> System.out.println("   - 清理工作完成"));

        sleep(2000);
        System.out.println();
    }

    /**
     * 演示Completable的主动完成能力
     */
    private static void demonstrateCompletableCapability() {
        System.out.println("3. Completable能力 - 主动完成:");

        CompletableFuture<String> promise = new CompletableFuture<>();

        // 注册回调
        promise.thenAccept(result -> System.out.println("   - 接收到结果: " + result));
        promise.exceptionally(ex -> {
            System.out.println("   - 发生异常: " + ex.getMessage());
            return null;
        });

        System.out.println("   - Promise创建状态: " + promise.isDone());

        // 模拟其他线程在某个时刻主动完成Promise
        new Thread(() -> {
            sleep(1000);
            System.out.println("   - 外部事件触发，即将主动完成Promise");
            promise.complete("外部事件的结果");
        }).start();

        sleep(2000);
        System.out.println();
    }

    /**
     * 演示构建异步计算图
     */
    private static void demonstrateAsyncComputationGraph() {
        System.out.println("4. 构建异步计算图:");

        // 并行的数据源
        CompletableFuture<String> source1 = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return "数据源1";
        });

        CompletableFuture<String> source2 = CompletableFuture.supplyAsync(() -> {
            sleep(800);
            return "数据源2";
        });

        CompletableFuture<String> source3 = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "数据源3";
        });

        // 组合计算图
        CompletableFuture<String> combined = source1
            .thenCombine(source2, (s1, s2) -> s1 + " + " + s2)
            .thenCombine(source3, (combined12, s3) -> combined12 + " + " + s3);

        combined.thenAccept(result -> System.out.println("   - 组合结果: " + result));

        // 等所有完成后执行
        CompletableFuture<Void> allOf = CompletableFuture.allOf(source1, source2, source3);
        allOf.thenRun(() -> System.out.println("   - 所有数据源都已就绪"));

        // 任一完成就执行
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(source1, source2, source3);
        anyOf.thenAccept(result -> System.out.println("   - 最快的数据源: " + result));

        sleep(2000);
        System.out.println();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}