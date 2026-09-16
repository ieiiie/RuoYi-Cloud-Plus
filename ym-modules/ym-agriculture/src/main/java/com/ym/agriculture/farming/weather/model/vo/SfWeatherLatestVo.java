package com.ym.agriculture.farming.weather.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 某 adcode 合并后的预报视图：含今天之前的历史日期与最新同步的当天及未来预报。
 *
 * @author ym-cloud
 */
@Data
public class SfWeatherLatestVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 高德行政区划 adcode
     */
    private String adcode;

    /**
     * 预报列表，按 {@code castDate} 升序；可能含今天之前的历史日期
     */
    private List<SfWeatherForecastVo> forecasts = new ArrayList<>();

    /**
     * 实况天气（温度/湿度/风力等，按需从高德拉取并缓存）
     */
    private SfWeatherLiveVo live;
}
