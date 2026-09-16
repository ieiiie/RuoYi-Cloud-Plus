package com.ym.agriculture.farming.weatheralert.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 气象预警缓存，对应 {@code sf_weather_alert}。
 * <p>平台全局表，无 tenant_id，需加入 tenant.excludes。
 */
@Data
@NoArgsConstructor
@TableName("sf_weather_alert")
public class SfWeatherAlert implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "warning_id", type = IdType.INPUT)
    private String warningId;

    private String provinceCode;

    private String cityCode;

    private String districtCode;

    private String title;

    private String headline;

    private String alertTypeCode;

    private String alertTypeName;

    private String levelCode;

    private String levelName;

    private String publisher;

    private Date effectiveAt;

    private Date expiresAt;

    private String areaText;

    private String description;

    private String instructionJson;

    private String sourceUrl;

    private Integer activeFlag;

    private String freshnessStatus;

    private Date lastSeenAt;

    private Integer missingCount;

    private Date createTime;

    private Date updateTime;
}
