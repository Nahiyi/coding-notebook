package cn.clazs.jdk.simulationdemo;

import java.util.UUID;

/**
 * 5. 启动测试
 */
public class SimulationMain {
    public static void main(String[] args) throws InterruptedException {
        String taskId = "task-" + UUID.randomUUID();

        // 1. 初始化任务并存入Map
        Constants.CONTEXT_MAP.put(taskId, new SimulationContext(taskId));

        // 2. 启动后台计算线程；这里使用线程池模拟
        Constants.CUSTOM_EXECUTOR.execute(new SimulationEngine(taskId));

        // 3. 让它先跑一会儿 (1倍速)
        Thread.sleep(1000);

        // 4. 【模拟前端请求】：修改为 10倍速
        SpeedController controller = new SpeedController();
        controller.changeSpeed(taskId, 10.0);

        // 5. 让它跑一会儿 (观察控制台，步长应该瞬间变大)
        Thread.sleep(1000);

        // 6. 【模拟前端请求】：修改为 0.5倍速 (慢放)
        controller.changeSpeed(taskId, 0.5);

        Thread.sleep(1000);

        // 7. 停止任务
        Constants.CONTEXT_MAP.get(taskId).stop();
        Constants.CUSTOM_EXECUTOR.shutdown();
    }
}