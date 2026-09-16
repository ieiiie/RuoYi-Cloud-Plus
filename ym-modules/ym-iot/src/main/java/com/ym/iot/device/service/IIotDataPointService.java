package com.ym.iot.device.service;

import com.ym.iot.device.domain.IotDataPoint;
import com.ym.iot.device.domain.vo.IotDataPointRecordVo;
import com.ym.iot.device.domain.vo.IotLatestVo;
import com.ym.iot.device.domain.vo.IotPestChartVo;
import com.ym.iot.device.domain.vo.IotPestNightChartVo;
import com.ym.iot.device.domain.vo.IotSeriesBatchVo;
import com.ym.iot.device.domain.vo.IotSeriesVo;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 测点时序数据业务接口。
 * <p>
 * 提供设备属性最新值（getLatest）和时序曲线数据（getSeries，支持时间范围与聚合步长）。
 * </p>
 *
 * @author ym-cloud
 */
public interface IIotDataPointService {

    /** 获取设备各属性最新上报数据（仪表盘用）。 */
    IotLatestVo getLatest(Long deviceId);

    /**
     * 批量获取多个设备的最新测点数据。
     * <p>一次加载所有设备信息，避免 N+1 查询 iot_device。</p>
     *
     * @param deviceIds 设备主键集合
     * @return deviceId -> IotLatestVo 映射
     */
    Map<Long, IotLatestVo> getLatestMap(Collection<Long> deviceIds);

    /**
     * 批量只读 Redis latest 缓存，不回查设备表和时序库。
     * <p>用于移动端列表等首屏快路径；返回结果只包含缓存命中的设备。</p>
     *
     * @param deviceIds 设备主键集合
     * @return deviceId -> IotLatestVo 映射，仅包含缓存命中项
     */
    Map<Long, IotLatestVo> getLatestMapCached(Collection<Long> deviceIds);

    /**
     * 批量获取多个设备的最新测点数据，绕过 Redis latest 缓存直查时序库。
     * <p>用于农事传感器预览等需要实时各测点最新值的场景；不读、不写 latest 缓存。</p>
     *
     * @param deviceIds 设备主键集合
     * @return deviceId -> IotLatestVo 映射
     */
    Map<Long, IotLatestVo> getLatestMapFresh(Collection<Long> deviceIds);

    /**
     * 批量预热多个设备的最新测点数据。
     * <p>绕过 Redis 直查时序库，并将查询结果写入 latest 缓存，供移动端列表和批量参数接口快速读取。</p>
     *
     * @param deviceIds 设备主键集合
     * @return deviceId -> IotLatestVo 映射
     */
    Map<Long, IotLatestVo> warmupLatestMap(Collection<Long> deviceIds);

    /** 获取指定 metric 的时序数据，支持时间范围与聚合步长。 */
    IotSeriesVo getSeries(Long deviceId, String metricCode, Date from, Date to, Integer step);

    /**
     * 查询设备单次采集记录。
     *
     * @param deviceId 设备主键
     * @param collectTime 指定采集时间；为空时取最新一条聚合记录
     * @return 同一采集时间下的测点记录；无数据时返回空记录
     */
    IotDataPointRecordVo getRecord(Long deviceId, Date collectTime);

    /**
     * 查询设备相邻一条采集记录。
     *
     * @param deviceId 设备主键
     * @param collectTime 当前采集时间
     * @param previous true 查询更早一条，false 查询更新一条
     * @return 相邻采集记录；边界无数据时返回空记录
     */
    IotDataPointRecordVo getAdjacentRecord(Long deviceId, Date collectTime, boolean previous);

    /**
     * 查询设备相邻一条采集记录的采集时间（轻量，不加载测点快照）。
     *
     * @param deviceId 设备主键
     * @param collectTime 当前采集时间
     * @param previous true 查询更早一条，false 查询更新一条
     * @return 相邻采集时间；边界无数据时返回 null
     */
    Date getAdjacentCollectTime(Long deviceId, Date collectTime, boolean previous);

    /**
     * 批量获取多个 metric 的时序数据（与 {@link #getSeries} 参数语义相同）。
     * <p>用于产品多属性场景，避免前端逐条请求。</p>
     */
    IotSeriesBatchVo getSeriesBatch(Long deviceId, List<String> metricCodes, Date from, Date to, Integer step);

    /**
     * 获取虫情设备图表与识别详情。
     *
     * @param deviceId 设备主键
     * @param from     起始时间，可为空
     * @param to       结束时间，可为空
     * @return 虫情总数趋势、虫种堆叠柱图、识别记录和默认选中记录
     */
    IotPestChartVo getPestChart(Long deviceId, Date from, Date to);

    /**
     * 获取虫情夜间聚合柱状图。
     *
     * @param deviceId 设备主键
     * @param from     起始时间，可为空；为空时默认最近 7 天
     * @param to       结束时间，可为空；为空时默认当前时间
     * @return 夜间聚合虫种堆叠柱图数据
     */
    IotPestNightChartVo getPestNightChart(Long deviceId, Date from, Date to);

    /**
     * 写入单条测点（接入层/MQTT 等调用；租户默认取当前登录租户）。
     */
    void appendPoint(IotDataPoint point);

    /**
     * 仅写入测点存储，不刷新设备 {@code last_report_time}；由调用方在批量写入后统一刷新。
     */
    void appendDataPointStorageOnly(IotDataPoint point);

    /**
     * 批量写入测点存储，不刷新设备 {@code last_report_time}；由调用方在批量写入后统一刷新。
     */
    void appendDataPointsStorageOnly(List<IotDataPoint> points);
}
