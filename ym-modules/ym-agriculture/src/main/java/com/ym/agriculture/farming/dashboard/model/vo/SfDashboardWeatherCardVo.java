package com.ym.agriculture.farming.dashboard.model.vo;

import com.ym.agriculture.farming.weather.model.vo.SfWeatherForecastVo;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherLiveVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 仪表盘天气卡片：预报读库（定时任务同步），实况按 6/12/18/0 点分时段按需拉取。
 * <p>
 * 首页温度/湿度/风优先展示 {@link SfWeatherLiveVo}；多日预报展示 {@link SfWeatherForecastVo}。
 *
 * @author ym-cloud
 */
@Data
public class SfDashboardWeatherCardVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实际查询使用的 adcode（由当前租户 {@code sys_tenant.district_code} 规范化得到）
     */
    private String adcode;

    /**
     * 城市名（优先取当天预报 {@link SfWeatherForecastVo#getCityName()}，否则取列表首条）
     */
    private String cityName;

    /**
     * 当天预报（按 {@code castDate} 等于今天匹配；无匹配时取今天及未来第一条）
     */
    private SfWeatherForecastVo today;

    /**
     * 整日预报列表（含今天之前历史、今天及未来数日，按 {@code castDate} 升序）
     */
    private List<SfWeatherForecastVo> forecasts = new ArrayList<>();

    /**
     * 实况天气（温度/湿度/风力；服务端按 6/12/18/0 点分时段缓存）
     */
    private SfWeatherLiveVo live;
}
