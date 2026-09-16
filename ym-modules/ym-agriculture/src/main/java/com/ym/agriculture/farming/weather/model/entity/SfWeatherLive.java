package com.ym.agriculture.farming.weather.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 高德实况天气落库，对应 {@code sf_weather_live}。
 * <p>
 * 表无租户、逻辑删除与通用审计字段；多租户 SQL 拦截需排除本表（见 {@code tenant.excludes}）。
 */
@Data
@NoArgsConstructor
@TableName("sf_weather_live")
public class SfWeatherLive implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "live_id", type = IdType.ASSIGN_ID)
    private Long liveId;

    /** 高德区域编码 */
    private String adcode;

    /** 省级名称 */
    private String province;

    /** 城市/区名称 */
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

    /** 本服务拉取入库时间 */
    private Date pullTime;
}
