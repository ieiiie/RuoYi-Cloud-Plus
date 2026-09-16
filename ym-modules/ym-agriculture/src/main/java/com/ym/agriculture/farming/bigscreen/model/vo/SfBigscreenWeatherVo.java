package com.ym.agriculture.farming.bigscreen.model.vo;

import com.ym.agriculture.farming.dashboard.model.vo.SfDashboardWeatherCardVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 大屏天气接口出参（透传 dashboard 天气 + 降雨模式说明）。
 */
@Data
public class SfBigscreenWeatherVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 天气卡片 */
    private SfDashboardWeatherCardVo weather;

    /** 降雨展示模式：WEATHER_TEXT */
    private String rainForecastMode;
}
