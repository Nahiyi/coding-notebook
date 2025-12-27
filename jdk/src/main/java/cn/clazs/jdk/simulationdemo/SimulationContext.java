package cn.clazs.jdk.simulationdemo;

import java.time.Duration;

/**
 * 1. 任务上下文对象 (存放在堆内存中，作为共享数据)
 */
public class SimulationContext {

    private String taskId;

    private Duration duration = Duration.ofSeconds(10 * 60L);

    // 【关键点】：使用 volatile 修饰！
    // 保证 线程A(控制端) 修改后，线程B(计算端) 能立刻由主内存读到最新值
    // 因为是直接赋值操作，不需要 synchronized 加锁
    private volatile double speedRate = 1.0;

    // 控制任务结束的标志位
    private volatile boolean running = true;

    public SimulationContext(String taskId) {
        this.taskId = taskId;
    }

    // --- Getters & Setters ---
    public double getSpeedRate() {
        return speedRate;
    }

    public void setSpeedRate(double speedRate) {
        this.speedRate = speedRate;
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        this.running = false;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }
}