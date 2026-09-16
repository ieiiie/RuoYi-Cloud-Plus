package com.ym.agriculture.farming.dashboard.service;

import com.ym.iot.api.domain.vo.RemoteSeriesVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchDashboardStatsVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardMapDataVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardSensorSummaryVo;
import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardWeatherCardVo;

import java.util.Date;

/**
 * 智慧农业首页仪表盘聚合数据。
 *
 * @author ym-cloud
 */
public interface ISfDashboardService {

    /**
     * 农田地图：地块边界、展示状态、进行中批次、传感器锚点（数据范围同地块列表）。
     */
    SfDashboardMapDataVo getMapData();

    /**
     * 天气卡片：读库最新预报（须先同步或他处已同步）；按当前租户 {@code sys_tenant.district_code} 解析 adcode。
     */
    SfDashboardWeatherCardVo getWeatherCard();

    /**
     * 传感器侧栏：三段栏目（农业传感器 / 摄像头 / 其它设备）按类型行汇总在线/离线；
     * {@code device_category} 与字典 {@code iot_device_type} 对齐，未命中字典归一为 OTHER；
     * 在线优先 {@code online_status=ONLINE}，否则最近上报/远程拉取在 5 分钟内兜底。
     */
    SfDashboardSensorSummaryVo getSensorSummary();

    /**
     * 地块状态概览：各 {@code batch_status} 批次数（权限同批次列表）。
     */
    SfPlantingBatchDashboardStatsVo getBatchStatusStats();

    /**
     * 传感器历史曲线：设备须已绑定到当前用户权限内的任一块地；参数语义同 {@code GET /iot/device/{id}/series}。
     */
    RemoteSeriesVo getSensorSeries(Long deviceId, String metricCode, Date from, Date to, Integer step);
}
