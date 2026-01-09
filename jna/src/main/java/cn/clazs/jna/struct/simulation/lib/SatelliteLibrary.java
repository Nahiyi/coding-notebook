package cn.clazs.jna.struct.simulation.lib;

import cn.clazs.jna.struct.simulation.entity.SatelliteParams;
import cn.clazs.jna.struct.simulation.entity.SatelliteData;
import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * 卫星仿真算法库接口
 *
 * C语言原型：
 * SatelliteData calculate_satellite_state(SatelliteParams* params);
 *
 * 设计要点：
 * 1. 参数SatelliteParams按指针传递（避免拷贝大结构体）
 * 2. 返回值SatelliteData按值返回（内存管理清晰）
 * 3. C库内部实现：根据params计算当前时刻的卫星状态
 *
 * @author clazs
 */
public interface SatelliteLibrary extends Library {

    /**
     * 加载卫星仿真算法库
     *
     * Windows: satellite.dll
     * Linux: libsatellite.so
     */
    SatelliteLibrary INSTANCE = Native.load("satellite", SatelliteLibrary.class);

    /**
     * 计算卫星状态
     *
     * C语言原型：
     * SatelliteData calculate_satellite_state(SatelliteParams* params);
     *
     * 功能：
     * - 接收卫星参数（指针传入）
     * - 根据timeStep计算当前时刻的卫星状态
     * - 返回SatelliteData结构体值
     *
     * @param params 卫星参数（按引用传递，JNA自动转换为指针）
     * @return 卫星状态数据（按值返回，JNA自动处理内存拷贝）
     */
    SatelliteData.ByValue calculate_satellite_state(SatelliteParams params);

    /**
     * 批量计算卫星状态（性能优化版本）
     *
     * C语言原型：
     * void calculate_satellite_states_batch(SatelliteParams* params, SatelliteData* results, int count);
     *
     * 功能：
     * - 一次性计算多个时刻的卫星状态
     * - 减少JNA调用次数，提升性能
     *
     * @param params 参数数组
     * @param results 结果数组（输出参数）
     * @param count 数组长度
     */
    void calculate_satellite_states_batch(SatelliteParams params, SatelliteData results, int count);
}
