package cn.clazs.jna.struct.simulation.entity;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import java.util.Arrays;
import java.util.List;

/**
 * 卫星状态数据结构体
 *
 * C语言定义：
 * typedef struct {
 *     double position[3];      // 位置 [x, y, z] 单位：m（地心惯性坐标系）
 *     double velocity[3];      // 速度 [vx, vy, vz] 单位：m/s
 *     double attitude[4];      // 姿态四元数 [q0, q1, q2, q3]
 *     double timestamp;        // 时间戳（从仿真开始计算的秒数）
 *     int status;              // 状态码 0=正常 1=警告 2=错误
 *     double remainingFuel;    // 剩余燃料（kg）
 *     double power;            // 当前功率（W）
 * } SatelliteData;
 *
 * @author clazs
 */
@FieldOrder({
        "position",
        "velocity",
        "attitude",
        "timestamp",
        "status",
        "remainingFuel",
        "power"
})
public class SatelliteData extends Structure {

    /**
     * 位置向量 [x, y, z] 单位：m（地心惯性坐标系ECI）
     */
    public double[] position = new double[3];

    /**
     * 速度向量 [vx, vy, vz] 单位：m/s
     */
    public double[] velocity = new double[3];

    /**
     * 姿态四元数 [q0, q1, q2, q3]
     * q0: 标量部分
     * q1, q2, q3: 向量部分
     * 约束：q0² + q1² + q2² + q3² = 1
     */
    public double[] attitude = new double[4];

    /**
     * 时间戳（从仿真开始计算的秒数）
     */
    public double timestamp;

    /**
     * 状态码
     * 0: 正常
     * 1: 警告（如接近最大仰角）
     * 2: 错误（如轨道衰减）
     */
    public int status;

    /**
     * 剩余燃料（kg）
     */
    public double remainingFuel;

    /**
     * 当前功耗（W）
     */
    public double power;

    /**
     * 默认构造函数
     */
    public SatelliteData() {
        super();
    }

    /**
     * 按值传递的内部类（函数返回值使用）
     * C函数返回结构体值，必须用ByValue
     */
    public static class ByValue extends SatelliteData implements Structure.ByValue {
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "position",
                "velocity",
                "attitude",
                "timestamp",
                "status",
                "remainingFuel",
                "power"
        );
    }

    /**
     * 获取位置的大小（到地心的距离）
     */
    public double getPositionMagnitude() {
        return Math.sqrt(
                position[0] * position[0] +
                        position[1] * position[1] +
                        position[2] * position[2]
        );
    }

    /**
     * 获取速度的大小
     */
    public double getVelocityMagnitude() {
        return Math.sqrt(
                velocity[0] * velocity[0] +
                        velocity[1] * velocity[1] +
                        velocity[2] * velocity[2]
        );
    }

    /**
     * 状态码转文字描述
     */
    public String getStatusDescription() {
        switch (status) {
            case 0:
                return "正常";
            case 1:
                return "警告";
            case 2:
                return "错误";
            default:
                return "未知";
        }
    }

    @Override
    public String toString() {
        return String.format(
                "SatelliteData{位置=[%.1f,%.1f,%.1f]km, 速度=[%.1f,%.1f,%.1f]km/s, 时间=%.2fs, 状态=%s, 燃料=%.1fkg, 功率=%.1fW}",
                position[0] / 1000, position[1] / 1000, position[2] / 1000,
                velocity[0] / 1000, velocity[1] / 1000, velocity[2] / 1000,
                timestamp,
                getStatusDescription(),
                remainingFuel,
                power
        );
    }

    /**
     * 转换为JSON格式（用于发送到Kafka）
     */
    public String toJson() {
        return String.format(
                "{\"timestamp\":%.3f,\"position\":[%.2f,%.2f,%.2f],\"velocity\":[%.2f,%.2f,%.2f],\"status\":%d}",
                timestamp,
                position[0], position[1], position[2],
                velocity[0], velocity[1], velocity[2],
                status
        );
    }
}
