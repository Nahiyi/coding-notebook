package cn.clazs.jdk.simulationdemo;

/**
 * 4. 模拟 Controller 层 (修改线程)
 */
public class SpeedController {
    public void changeSpeed(String taskId, double newSpeed) {
        SimulationContext context = Constants.CONTEXT_MAP.get(taskId);
        if (context != null) {
            // 这里没必要判断running，无意义，即便外部running，可能内部一进来就false了
            System.out.println("\n****** [控制线程] 收到请求，将任务[" + taskId + "] 倍率修改为: " + newSpeed + " ******\n");
            
            // 【写】原子操作，直接赋值，无需加锁
            context.setSpeedRate(newSpeed); 
        }
    }
}