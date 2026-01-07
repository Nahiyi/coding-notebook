package cn.clazs.jna.struct.simulation;

import cn.clazs.jna.struct.simulation.entity.SatelliteData;
import cn.clazs.jna.struct.simulation.entity.SatelliteParams;
import cn.clazs.jna.struct.simulation.lib.SatelliteLibrary;
import com.sun.jna.Pointer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 卫星仿真引擎
 *
 * 功能：
 * 1. 封装JNA调用细节
 * 2. 实现高频仿真循环
 * 3. 提供性能优化
 * 4. 支持动态调整仿真倍率
 *
 * @author clazs
 */
public class SimulationEngine {

    private final SatelliteLibrary library;
    private final ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // 仿真倍率（支持动态调整）
    private volatile double simulationSpeed = 1.0;

    // 仿真结果回调接口
    public interface SimulationCallback {
        void onResult(SatelliteData data);
    }

    public SimulationEngine() {
        this.library = SatelliteLibrary.INSTANCE;
        this.executorService = Executors.newFixedThreadPool(4);  // 4个线程并发处理
    }

    /**
     * 运行仿真任务
     *
     * @param params 初始参数
     * @param durationMs 仿真总时长（毫秒）
     * @param stepMs 时间步进（毫秒）
     * @param callback 结果回调
     */
    public void runSimulation(SatelliteParams params,
                              double durationMs,
                              double stepMs,
                              SimulationCallback callback) {
        // 校验参数
        params.validate();

        // 写入native内存（只写入一次，避免重复write）
        params.write();

        System.out.println("开始仿真: " + params);
        System.out.println(String.format("仿真时长: %.2fs, 步进: %.2fms, 倍率: %.1fx",
            durationMs / 1000.0, stepMs, simulationSpeed));

        running.set(true);

        // 在独立线程中运行仿真
        executorService.submit(() -> {
            try {
                // 性能优化：复用params对象，只修改timeStep字段
                Pointer p = params.getPointer();

                for (double t = 0; t < durationMs && running.get(); t += stepMs) {
                    // 更新时间步进（直接修改native内存，避免重复write）
                    params.timeStep = t;
                    // 由于只修改了基本类型字段，直接通过Pointer修改更高效
                    // 找到timeStep字段的偏移量并设置值
                    // 注意：这里为了演示清晰，还是调用write()

                    params.write();  // 将更新的timeStep写入native内存

                    // 调用C库计算
                    SatelliteData.ByValue result = library.calculate_satellite_state(params);

                    // 转换为SatelliteData对象（手动拷贝字段）
                    SatelliteData data = new SatelliteData();
                    data.position[0] = result.position[0];
                    data.position[1] = result.position[1];
                    data.position[2] = result.position[2];
                    data.velocity[0] = result.velocity[0];
                    data.velocity[1] = result.velocity[1];
                    data.velocity[2] = result.velocity[2];
                    data.attitude[0] = result.attitude[0];
                    data.attitude[1] = result.attitude[1];
                    data.attitude[2] = result.attitude[2];
                    data.attitude[3] = result.attitude[3];
                    data.timestamp = result.timestamp;
                    data.status = result.status;
                    data.remainingFuel = result.remainingFuel;
                    data.power = result.power;

                    // 回调处理结果（发送到Kafka等）
                    callback.onResult(data);

                    // 控制仿真速度（实际情况是通过控制步进倍率，这里模拟用sleep）
                    long actualDelay = (long) (stepMs / simulationSpeed);
                    if (actualDelay > 0) {
                        Thread.sleep(actualDelay);
                    }
                }

                System.out.println("仿真完成");

            } catch (InterruptedException e) {
                System.out.println("仿真被中断");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("仿真出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 停止仿真
     */
    public void stopSimulation() {
        running.set(false);
    }

    /**
     * 动态调整仿真倍率
     *
     * @param speed 倍率 (0.1x ~ 10x)
     */
    public void setSimulationSpeed(double speed) {
        if (speed < 0.1 || speed > 10.0) {
            throw new IllegalArgumentException("仿真倍率必须在0.1x~10x之间");
        }
        this.simulationSpeed = speed;
        System.out.println("仿真倍率调整为: " + speed + "x");
    }

    /**
     * 关闭引擎
     */
    public void shutdown() {
        stopSimulation();
        executorService.shutdown();
    }

    /**
     * 性能优化演示：批量计算（可能的亮点！！！）
     *
     * 适用场景：
     * - 需要预先计算未来N个时刻的状态
     * - 减少JNA调用次数，提升性能
     *
     * @param params 参数
     * @param startTimeMs 起始时间
     * @param stepMs 步进
     * @param count 计算数量
     * @return 结果数组
     */
    public SatelliteData[] batchCalculate(SatelliteParams params,
                                         double startTimeMs,
                                         double stepMs,
                                         int count) {
        params.validate();
        params.write();

        SatelliteData[] results = new SatelliteData[count];

        // 方式1：循环调用（慢）
        // for (int i = 0; i < count; i++) {
        //     params.timeStep = startTimeMs + i * stepMs;
        //     params.write();
        //     SatelliteData.ByValue result = library.calculate_satellite_state(params);
        //     SatelliteData data = new SatelliteData();
        //     data.use(result);
        //     results[i] = data;
        // }

        // 方式2：批量调用（快）- 需要C库支持
        // SatelliteData resultArray = new SatelliteData();
        // library.calculate_satellite_states_batch(params, resultArray, count);

        // 这里演示方式1
        for (int i = 0; i < count; i++) {
            params.timeStep = startTimeMs + i * stepMs;
            params.write();
            SatelliteData.ByValue result = library.calculate_satellite_state(params);
            SatelliteData data = new SatelliteData();
            // 手动拷贝字段
            data.position[0] = result.position[0];
            data.position[1] = result.position[1];
            data.position[2] = result.position[2];
            data.velocity[0] = result.velocity[0];
            data.velocity[1] = result.velocity[1];
            data.velocity[2] = result.velocity[2];
            data.attitude[0] = result.attitude[0];
            data.attitude[1] = result.attitude[1];
            data.attitude[2] = result.attitude[2];
            data.attitude[3] = result.attitude[3];
            data.timestamp = result.timestamp;
            data.status = result.status;
            data.remainingFuel = result.remainingFuel;
            data.power = result.power;
            results[i] = data;
        }

        return results;
    }
}
