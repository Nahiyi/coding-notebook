package cn.clazs.jdk.simulationdemo;

import java.util.concurrent.TimeUnit;

/**
 * 3. 模拟后台计算引擎 (异步线程)
 */
public class SimulationEngine implements Runnable {
    private final String taskId;

    public SimulationEngine(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public void run() {
        // 从全局Map中获取上下文引用
        SimulationContext context = Constants.CONTEXT_MAP.get(taskId);
        
        System.out.println(">>> 任务[" + taskId + "] 引擎启动，默认倍率: " + context.getSpeedRate());

        double virtualTime = 0.0; // 仿真内部时间
        long beginTime = System.currentTimeMillis();

        while (context.isRunning() &&
                System.currentTimeMillis() - beginTime < context.getDuration().toMillis()) {

            // A. 【读】每次循环，直接读取 volatile 变量，获取最新倍率
            double currentRate = context.getSpeedRate();

            // B. 根据倍率计算步长
            // 基础步长 0.01s * 倍率 = 实际仿真跨度
            double stepSize = 0.01 * currentRate;

            // C. 模拟调用C++计算 (这里用打印代替)
            virtualTime += stepSize;
            
            // 打印日志证明我们读到了变化
            System.out.printf("[计算线程] 仿真时间: %.4fs | 当前倍率: %.1fx | 本次步长: %.4f%n", 
                    virtualTime, currentRate, stepSize);

            // D. 模拟固定频率发送 (比如每秒50次，睡20ms)
            try {
                System.out.println("[计算线程] 异步发送数据包到 Kafka 中...");
                TimeUnit.MILLISECONDS.sleep(200); // 为了演示效果，这里睡慢点(200ms)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println(">>> 任务[" + taskId + "] 引擎停止。");
    }
}