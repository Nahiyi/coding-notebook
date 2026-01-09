package cn.clazs.jna.struct.simulation.entity;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import java.util.Arrays;
import java.util.List;

/**
 * 卫星仿真参数结构体
 *
 * C语言定义：
 * typedef struct {
 *     int satelliteId;            // 卫星ID
 *     double initialDisturbance[3]; // 初始扰动 [x, y, z]
 *     double deflectionAngle;      // 偏向角（弧度） 、 还有“偏心率等等”
 *     double timeStep;             // 当前时间步进（毫秒级）
 *     double simulationSpeed;      // 仿真倍率 (0.1x ~ 10x)
 *     double orbitAltitude;        // 轨道高度（km）
 *     double inclination;          // 轨道倾角（度）
 * } SatelliteParams;
 *
 * @author clazs
 */
@FieldOrder({
    "satelliteId",
    "initialDisturbance",
    "deflectionAngle",
    "timeStep",
    "simulationSpeed",
    "orbitAltitude",
    "inclination"
})
public class SatelliteParams extends Structure {

    /**
     * 卫星ID（对应数据库中的型号ID）
     */
    public int satelliteId;

    /**
     * 初始扰动向量 [x, y, z] 单位：m/s²
     * 重要：数组必须初始化，否则JNA不知道数组大小
     */
    public double[] initialDisturbance = new double[3];

    /**
     * 偏向角（弧度）
     * 范围：0 ~ 2π
     */
    public double deflectionAngle;

    /**
     * 当前时间步进（秒）
     * 仿真循环中会不断更新这个值
     */
    public double timeStep;

    /**
     * 仿真倍率
     * 0.1x: 慢动作回放
     * 1.0x: 实时仿真
     * 10x: 快速预测
     */
    public double simulationSpeed;

    /**
     * 轨道高度（km）
     * 典型值：LEO 400km, GEO 35786km
     */
    public double orbitAltitude;

    /**
     * 轨道倾角（度）
     * 范围：0° ~ 180°
     * 0°: 赤道轨道
     * 90°: 极地轨道
     * 99°: 太阳同步轨道
     */
    public double inclination;

    /**
     * 默认构造函数
     */
    public SatelliteParams() {
        super();
    }

    /**
     * 按引用传递的内部类（参数传递使用）
     * SatelliteParams作为输入参数，只需要ByReference
     */
    public static class ByReference extends SatelliteParams implements Structure.ByReference {
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
            "satelliteId",
            "initialDisturbance",
            "deflectionAngle",
            "timeStep",
            "simulationSpeed",
            "orbitAltitude",
            "inclination"
        );
    }

    /**
     * 校验参数合法性
     */
    public void validate() {
        if (satelliteId <= 0) {
            throw new IllegalArgumentException("卫星ID必须大于0");
        }
        if (simulationSpeed < 0.1 || simulationSpeed > 10.0) {
            throw new IllegalArgumentException("仿真倍率必须在0.1x~10x之间");
        }
        if (orbitAltitude < 100 || orbitAltitude > 40000) {
            throw new IllegalArgumentException("轨道高度必须在100~40000km之间");
        }
        if (inclination < 0 || inclination > 180) {
            throw new IllegalArgumentException("轨道倾角必须在0~180度之间");
        }
        if (initialDisturbance.length != 3) {
            throw new IllegalArgumentException("初始扰动必须是3维向量");
        }
    }

    @Override
    public String toString() {
        return String.format(
            "SatelliteParams{卫星ID=%d, 初始扰动=[%.2f,%.2f,%.2f], 偏向角=%.3f°, 时间步进=%.2fs, 仿真倍率=%.1fx, 轨道高度=%.1fkm, 倾角=%.1f°}",
            satelliteId,
            initialDisturbance[0], initialDisturbance[1], initialDisturbance[2],
            Math.toDegrees(deflectionAngle),
            timeStep,
            simulationSpeed,
            orbitAltitude,
            inclination
        );
    }
}
