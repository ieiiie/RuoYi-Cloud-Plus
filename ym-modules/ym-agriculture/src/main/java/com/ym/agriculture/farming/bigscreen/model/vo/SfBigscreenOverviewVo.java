package com.ym.agriculture.farming.bigscreen.model.vo;

import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardWeatherCardVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 大屏顶栏与 KPI 聚合。
 */
@Data
public class SfBigscreenOverviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 驾驶舱标题 */
    private String cockpitTitle;

    /** 农场名称（租户企业名） */
    private String farmName;

    /** 租户编号 */
    private String tenantId;

    /** 行政区划展示，如阜康市 */
    private String region;

    /** 设备在线率 */
    private SfBigscreenOnlineRateVo onlineRate;

    /** 地块 KPI */
    private SfBigscreenFieldStatsVo fieldStats;

    /** 天气卡片 */
    private SfDashboardWeatherCardVo weather;

    /** 降雨展示模式：WEATHER_TEXT */
    private String rainForecastMode;
}
