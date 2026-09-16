package com.ym.agriculture.farming.weather.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 实况天气读模型（字段与高德 lives / 落库列对齐）。
 */
@Data
public class SfWeatherLiveVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 实况行主键 */
    private Long liveId;

    /** 行政区划 adcode */
    private String adcode;

    /** 省级名称 */
    private String province;

    /** 城市名称 */
    private String cityName;

    /** 天气现象 */
    private String weather;

    /** 实时气温，单位：摄氏度 */
    private String temperature;

    /** 风向 */
    private String windDirection;

    /** 风力级别 */
    private String windPower;

    /** 空气湿度 */
    private String humidity;

    /** 高德数据发布时间 reporttime */
    private String reportTime;

    /** 本平台拉取/入库时间 */
    private Date pullTime;
}
