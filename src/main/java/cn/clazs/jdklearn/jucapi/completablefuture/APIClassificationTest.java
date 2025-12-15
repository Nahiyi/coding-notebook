package cn.clazs.jdklearn.jucapi.completablefuture;

import java.util.concurrent.CompletableFuture;

/**
 * CompletableFuture API 分类测试用例
 * 按照静态方法 vs 实例方法的功能分类进行演示
 */
public class APIClassificationTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== CompletableFuture API 分类测试用例 ===\n");

        // 一、静态方法：创建和启动任务
        testStaticMethods();

        // 二、实例方法：组装后续处理
        testInstanceMethods();

        // 三、组合操作演示
        testCompositionMethods();

        // 四、异常处理演示
        testExceptionHandling();
    }

    /**
     * 一、静态方法：创建和启动任务
     */
    private static void testStaticMethods() throws Exception {
        System.out.println("一、静态方法：创建和启动任务");
        System.out.println("=====================================");

        // 1. supplyAsync - 有返回值的异步任务
        System.out.println("1. supplyAsync - 有返回值的异步任务:");
        CompletableFuture<String> supplyFuture = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return "异步任务结果";
        });
        System.out.println("   结果: " + supplyFuture.join());

        // 2. runAsync - 无返回值的异步任务
        System.out.println("\n2. runAsync - 无返回值的异步任务:");
        CompletableFuture<Void> runFuture = CompletableFuture.runAsync(() -> {
            sleep(300);
            System.out.println("   执行无返回值的异步任务");
        });
        runFuture.join();

        // 3. completedFuture - 已完成的Future
        System.out.println("\n3. completedFuture - 已完成的Future:");
        CompletableFuture<String> completedFuture = CompletableFuture.completedFuture("已预设的结果");
        System.out.println("   立即可用: " + completedFuture.join());

        // 4. allOf - 等待所有任务完成
        System.out.println("\n4. allOf - 等待所有任务完成:");
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
            sleep(400);
            return "任务1";
        });
        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
            sleep(600);
            return "任务2";
        });
        CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "任务3";
        });

        CompletableFuture<Void> allOf = CompletableFuture.allOf(task1, task2, task3);
        allOf.thenRun(() -> {
            try {
                System.out.println("   所有任务完成: " + task1.get() + ", " + task2.get() + ", " + task3.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).join();

        // 5. anyOf - 等待任意任务完成·
        System.out.println("\n5. anyOf - 等待任意任务完成:");
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> {
                    sleep(800);
                    return "慢任务";
                }),
                CompletableFuture.supplyAsync(() -> {
                    sleep(300);
                    return "快任务";
                })
        );
        System.out.println("   最快的任务: " + anyOf.join());

        System.out.println();
    }

    /**
     * 二、实例方法：组装后续处理
     */
    private static void testInstanceMethods() throws Exception {
        System.out.println("二、实例方法：组装后续处理");
        System.out.println("=====================================");

        CompletableFuture<String> baseFuture = CompletableFuture.supplyAsync(() -> "Hello");

        // 1. thenApply - 转换结果，有返回值
        System.out.println("1. thenApply - 转换结果，有返回值:");
        baseFuture
                .thenApply(s -> s + " World")
                .thenApply(String::toUpperCase)
                .thenAccept(result -> System.out.println("   转换结果: " + result))
                .join();

        // 2. thenAccept - 消费结果，无返回值
        System.out.println("\n2. thenAccept - 消费结果，无返回值:");
        baseFuture
                .thenAccept(s -> System.out.println("   消费结果: " + s))
                .join();

        // 3. thenRun - 完成回调，不关心结果
        System.out.println("\n3. thenRun - 完成回调，不关心结果:");
        baseFuture
                .thenRun(() -> System.out.println("   任务完成后的回调"))
                .join();

        // 4. thenApplyAsync vs thenApply 对比
        System.out.println("\n4. thenApplyAsync vs thenApply 对比:");
        System.out.println("   thenApply (同步): 在前置任务线程中执行");
        System.out.println("   thenApplyAsync (异步): 在线程池中执行");

        CompletableFuture<String> syncApply = baseFuture
                .thenApply(s -> {
                    System.out.println("   thenApply 线程: " + Thread.currentThread().getName());
                    return s + " - 同步处理";
                });

        CompletableFuture<String> asyncApply = baseFuture
                .thenApplyAsync(s -> {
                    System.out.println("   thenApplyAsync 线程: " + Thread.currentThread().getName());
                    return s + " - 异步处理";
                });

        System.out.println("   同步结果: " + syncApply.join());
        System.out.println("   异步结果: " + asyncApply.join());

        System.out.println();
    }

    /**
     * 三、组合操作演示
     */
    private static void testCompositionMethods() throws Exception {
        System.out.println("三、组合操作演示");
        System.out.println("=====================================");

        // 1. thenCompose - 串行依赖
        System.out.println("1. thenCompose - 串行依赖:");
        CompletableFuture<String> sequential = CompletableFuture.supplyAsync(() -> "第一步")
                .thenCompose(result -> CompletableFuture.supplyAsync(() -> result + " -> 第二步"))
                .thenCompose(result -> CompletableFuture.supplyAsync(() -> result + " -> 第三步"));
        System.out.println("   串行结果: " + sequential.join());

        // 2. thenCombine - 并行组合
        System.out.println("\n2. thenCombine - 并行组合:");
        CompletableFuture<String> parallel = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return "数据源A";
        }).thenCombine(
                CompletableFuture.supplyAsync(() -> {
                    sleep(300);
                    return "数据源B";
                }),
                (a, b) -> a + " + " + b + " -> 组合结果"
        );
        System.out.println("   并行组合: " + parallel.join());

        // 3. applyToEither - 竞争选择
        System.out.println("\n3. applyToEither - 竞争选择:");
        CompletableFuture<String> either = CompletableFuture.supplyAsync(() -> {
            sleep(800);
            return "慢速结果";
        }).applyToEither(
                CompletableFuture.supplyAsync(() -> {
                    sleep(200);
                    return "快速结果";
                }),
                result -> "选择: " + result
        );
        System.out.println("   竞争选择: " + either.join());

        // 4. thenAcceptBoth - 并行消费
        System.out.println("\n4. thenAcceptBoth - 并行消费:");
        CompletableFuture.supplyAsync(() -> {
                    sleep(400);
                    return "用户数据";
                })
                .thenAcceptBoth(
                        CompletableFuture.supplyAsync(() -> {
                            sleep(300);
                            return "订单数据";
                        }),
                        (user, order) -> System.out.println("   并行消费: " + user + " + " + order)
                ).join();

        System.out.println();
    }

    /**
     * 四、异常处理演示
     */
    private static void testExceptionHandling() throws Exception {
        System.out.println("四、异常处理演示");
        System.out.println("=====================================");

        // 1. exceptionally - 异常恢复
        System.out.println("1. exceptionally - 异常恢复:");
        CompletableFuture<Object> exceptionRecovery = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("任务执行失败");
        }).exceptionally(ex -> {
            System.out.println("   捕获异常: " + ex.getMessage());
            return "默认恢复值";
        });
        System.out.println("   恢复结果: " + exceptionRecovery.join());

        // 2. handle - 统一处理成功和失败
        System.out.println("\n2. handle - 统一处理成功和失败:");

        // 成功情况
        CompletableFuture<String> successHandle = CompletableFuture.supplyAsync(() -> "成功结果")
                .handle((result, ex) -> {
                    if (ex != null) {
                        return "异常处理: " + ex.getMessage();
                    }
                    return "成功处理: " + result;
                });
        System.out.println("   成功处理: " + successHandle.join());

        // 失败情况
        CompletableFuture<String> errorHandle = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("模拟错误");
        }).handle((result, ex) -> {
            if (ex != null) {
                return "异常处理: " + ex.getMessage();
            }
            return "成功处理: " + result;
        });
        System.out.println("   失败处理: " + errorHandle.join());

        // 3. whenComplete - 完成回调（不改变结果）
        System.out.println("\n3. whenComplete - 完成回调:");
        CompletableFuture<String> whenCompleteDemo = CompletableFuture.supplyAsync(() -> "回调演示")
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.out.println("   回调 - 异常: " + ex.getMessage());
                    } else {
                        System.out.println("   回调 - 成功: " + result);
                    }
                });
        System.out.println("   最终结果: " + whenCompleteDemo.join());

        // 4. 链式异常处理
        System.out.println("\n4. 链式异常处理:");
        CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "步骤1";
        }).thenApply(step1 -> {
            sleep(200);
            if (true) throw new RuntimeException("步骤2失败");
            return step1 + " -> 步骤2";
        }).thenApply(step2 -> {
            return step2 + " -> 步骤3";
        }).exceptionally(ex -> {
            System.out.println("   链中捕获异常: " + ex.getMessage());
            return "异常后的恢复值";
        }).thenAccept(finalResult -> {
            System.out.println("   最终处理: " + finalResult);
        }).join();

        System.out.println();
    }

    /**
     * 综合示例：实际业务场景
     */
    private static void demonstrateBusinessScenario() {
        System.out.println("五、综合示例：实际业务场景");
        System.out.println("=====================================");

        // 模拟用户登录获取订单信息的完整流程
        CompletableFuture<String> userLogin = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return "用户123已登录";
        });

        CompletableFuture<String> fetchOrders = userLogin.thenCompose(loginResult -> {
            System.out.println("   " + loginResult + "，开始获取订单");
            return CompletableFuture.supplyAsync(() -> {
                sleep(600);
                return "获取到订单: [订单1, 订单2, 订单3]";
            });
        });

        CompletableFuture<String> calculateTotal = fetchOrders.thenApply(orders -> {
            System.out.println("   " + orders + "，开始计算总金额");
            sleep(300);
            return "订单总金额: ¥1580.00";
        });

        CompletableFuture<Void> sendNotification = calculateTotal.thenAccept(total -> {
            System.out.println("   " + total + "，发送通知");
        }).thenRun(() -> {
            System.out.println("   业务流程完成");
        });

        // 异常处理
        CompletableFuture<String> resilientProcess = userLogin
                .thenCombine(fetchOrders, (login, orders) -> login + " | " + orders)
                .exceptionally(ex -> {
                    System.out.println("   业务异常: " + ex.getMessage());
                    return "降级处理结果";
                })
                .whenComplete((result, ex) -> {
                    System.out.println("   流程完成: " + result);
                });

        sendNotification.join();
        resilientProcess.join();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}