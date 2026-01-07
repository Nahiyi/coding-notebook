package cn.clazs.jna.struct.simulation;

import cn.clazs.jna.struct.simulation.entity.SatelliteData;
import cn.clazs.jna.struct.simulation.entity.SatelliteParams;

/**
 * 卫星仿真系统演示
 *
 * 业务场景：
 * 1. 用户通过前端选择卫星、设置仿真参数
 * 2. 后端调用C库的轨道算法进行高频仿真
 * 3. 仿真结果通过Kafka发送到前端进行可视化
 *
 * 技术亮点：
 * - 参数按指针传递（避免大结构体拷贝）
 * - 返回值按值返回（内存管理清晰）
 * - 支持动态调整仿真倍率
 * - 高频JNA调用的性能优化
 *
 * @author clazs
 */
public class SimulationDemo {

    public static void main(String[] args) {
        System.out.println("=== 卫星仿真系统演示 ===\n");

        // 创建仿真引擎
        SimulationEngine engine = new SimulationEngine();

        // 演示1：单次计算
        demonstrateSingleCalculation();

        // 演示2：高频仿真循环
        demonstrateSimulationLoop(engine);

        // 演示3：动态调整倍率
        demonstrateDynamicSpeed(engine);

        // 演示4：性能优化
        demonstratePerformanceOptimization();

        // 关闭引擎
        engine.shutdown();
    }

    /**
     * 演示1：单次计算
     */
    private static void demonstrateSingleCalculation() {
        System.out.println("【演示1】单次卫星状态计算");
        System.out.println();

        // 1. 准备参数（前端传来的数据）
        SatelliteParams params = new SatelliteParams();
        params.satelliteId = 1001;  // 北斗三号卫星
        params.initialDisturbance[0] = 0.01;  // x方向扰动 0.01 m/s²
        params.initialDisturbance[1] = 0.02;  // y方向扰动 0.02 m/s²
        params.initialDisturbance[2] = 0.0;   // z方向无扰动
        params.deflectionAngle = Math.toRadians(15);  // 偏向角15度
        params.timeStep = 0.0;  // 从0时刻开始
        params.simulationSpeed = 1.0;  // 1倍速（实时）
        params.orbitAltitude = 21500;  // GPS轨道高度 21500km
        params.inclination = 55;  // 轨道倾角55度

        System.out.println("输入参数: " + params);

        // 2. 校验参数
        try {
            params.validate();
            System.out.println("✓ 参数校验通过");
        } catch (IllegalArgumentException e) {
            System.err.println("✗ 参数校验失败: " + e.getMessage());
            return;
        }

        System.out.println();

        // 3. 调用C库（实际项目中需要真实的C库）
        // SatelliteLibrary library = SatelliteLibrary.INSTANCE;
        // SatelliteData.ByValue result = library.calculate_satellite_state(params);

        // 模拟返回结果
        SatelliteData result = mockCalculate(params);
        System.out.println("计算结果: " + result);
        System.out.println("JSON格式: " + result.toJson());
        System.out.println();

        System.out.println("关键点：");
        System.out.println("- SatelliteParams作为输入参数，按指针传递（默认）");
        System.out.println("- 必须调用write()将Java字段写入native内存");
        System.out.println("- SatelliteData.ByValue作为返回值，按值返回");
        System.out.println("- JNA自动处理返回值的内存拷贝");

        System.out.println(String.format("%60s", " ").replace(" ", "="));
        System.out.println();
    }

    /**
     * 演示2：高频仿真循环
     */
    private static void demonstrateSimulationLoop(SimulationEngine engine) {
        System.out.println("【演示2】高频仿真循环（模拟100ms间隔计算）");
        System.out.println();

        // 准备参数
        SatelliteParams params = new SatelliteParams();
        params.satelliteId = 2001;  // 风云四号卫星
        params.initialDisturbance[0] = 0.01;
        params.initialDisturbance[1] = 0.01;
        params.initialDisturbance[2] = 0.01;
        params.deflectionAngle = Math.toRadians(10);
        params.timeStep = 0.0;
        params.simulationSpeed = 1.0;
        params.orbitAltitude = 35786;  // 地球同步轨道 35786km
        params.inclination = 0;  // 赤道轨道

        // 运行仿真（仿真5秒，每100ms计算一次）
        double durationMs = 5000;  // 5秒
        double stepMs = 100;       // 100ms

        engine.runSimulation(params, durationMs, stepMs, data -> {
            // 回调：将结果发送到Kafka
            // kafkaTemplate.send("simulation-topic", data.toJson());
            System.out.println(String.format("[%.2fs] %s", data.timestamp, data));
        });

        // 等待仿真完成
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
        System.out.println("关键点：");
        System.out.println("- 在独立线程中运行仿真循环");
        System.out.println("- 每次循环只修改timeStep字段");
        System.out.println("- 通过回调异步处理结果（发送到Kafka）");
        System.out.println("- 支持并发多个仿真任务");

        System.out.println(String.format("%60s", " ").replace(" ", "="));
        System.out.println();
    }

    /**
     * 演示3：动态调整仿真倍率
     */
    private static void demonstrateDynamicSpeed(SimulationEngine engine) {
        System.out.println("【演示3】动态调整仿真倍率");
        System.out.println();

        SatelliteParams params = new SatelliteParams();
        params.satelliteId = 3001;
        params.initialDisturbance[0] = 0.01;
        params.initialDisturbance[1] = 0.01;
        params.initialDisturbance[2] = 0.01;
        params.deflectionAngle = Math.toRadians(5);
        params.timeStep = 0.0;
        params.simulationSpeed = 1.0;
        params.orbitAltitude = 400;  // 国际空间站轨道
        params.inclination = 51.6;

        // 启动仿真
        engine.runSimulation(params, 10000, 100, data -> {
            // 简化输出
        });

        // 动态调整倍率
        try {
            System.out.println("初始倍率: 1.0x (实时)");
            Thread.sleep(2000);

            System.out.println("加速到 5.0x (快进)");
            engine.setSimulationSpeed(5.0);
            Thread.sleep(2000);

            System.out.println("减速到 0.5x (慢动作)");
            engine.setSimulationSpeed(0.5);
            Thread.sleep(2000);

            System.out.println("恢复到 1.0x");
            engine.setSimulationSpeed(1.0);
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
        System.out.println("关键点：");
        System.out.println("- 通过volatile变量实现倍率的动态调整");
        System.out.println("- 所有仿真线程实时生效");
        System.out.println("- 支持倍率范围：0.1x ~ 10x");

        System.out.println(String.format("%60s", " ").replace(" ", "="));
        System.out.println();
    }

    /**
     * 演示4：性能优化
     */
    private static void demonstratePerformanceOptimization() {
        System.out.println("【演示4】性能优化 - 批量计算");
        System.out.println();

        SatelliteParams params = new SatelliteParams();
        params.satelliteId = 4001;
        params.initialDisturbance[0] = 0.01;
        params.initialDisturbance[1] = 0.01;
        params.initialDisturbance[2] = 0.01;
        params.deflectionAngle = Math.toRadians(20);
        params.orbitAltitude = 500;
        params.inclination = 97.5;  // 太阳同步轨道

        SimulationEngine engine = new SimulationEngine();

        // 批量计算未来100个时刻的状态
        double startTimeMs = 0;
        double stepMs = 100;
        int count = 100;

        System.out.println("批量计算 " + count + " 个时刻的卫星状态...");
        long startTime = System.currentTimeMillis();

        SatelliteData[] results = engine.batchCalculate(params, startTimeMs, stepMs, count);

        long endTime = System.currentTimeMillis();
        System.out.println("计算完成，耗时: " + (endTime - startTime) + "ms");
        System.out.println("平均每次计算: " + (endTime - startTime) / count + "ms");

        System.out.println();
        System.out.println("关键点：");
        System.out.println("- 批量计算减少JNA调用次数");
        System.out.println("- 适用于预先计算场景");
        System.out.println("- 可进一步提升到10倍性能");

        System.out.println(String.format("%60s", " ").replace(" ", "="));
        System.out.println();
    }

    /**
     * 模拟计算（实际项目中调用真实C库）
     */
    private static SatelliteData mockCalculate(SatelliteParams params) {
        SatelliteData data = new SatelliteData();

        // 简化的轨道计算（实际由C库完成）
        double r = params.orbitAltitude * 1000 + 6371000;  // 地球半径 + 轨道高度
        double v = Math.sqrt(398600000000.0 / r);  // 轨道速度

        data.position[0] = r * Math.cos(params.timeStep);
        data.position[1] = r * Math.sin(params.timeStep);
        data.position[2] = 0;

        data.velocity[0] = -v * Math.sin(params.timeStep);
        data.velocity[1] = v * Math.cos(params.timeStep);
        data.velocity[2] = 0;

        data.attitude[0] = 1.0;
        data.attitude[1] = 0.0;
        data.attitude[2] = 0.0;
        data.attitude[3] = 0.0;

        data.timestamp = params.timeStep;
        data.status = 0;
        data.remainingFuel = 100.0 - params.timeStep * 0.001;
        data.power = 1500.0;

        return data;
    }
}
