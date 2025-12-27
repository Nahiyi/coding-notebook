package cn.clazs.jdk.simulationdemo;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 模拟项目中的Constants常量类
 */
public class Constants {

    // 线程安全的Map，存储所有正在运行的任务上下文
    public static final Map<String, SimulationContext> CONTEXT_MAP = new ConcurrentHashMap<>();

    // 模拟通过@Bean注册的线程池
    public static final ThreadPoolExecutor CUSTOM_EXECUTOR = new ThreadPoolExecutor(
            5,  // corePoolSize
            10, // maximumPoolSize
            60, // keepAliveTime
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100), // 有界队列
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
}