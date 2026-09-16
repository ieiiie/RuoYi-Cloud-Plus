package com.ym.agriculture.farming.weatheralert.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 预警同步水位，对应 {@code sf_weather_alert_sync_meta}。 */
@Data
@NoArgsConstructor
@TableName("sf_weather_alert_sync_meta")
public class SfWeatherAlertSyncMeta implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String KEY_LAST_SUCCESS_AT = "last_success_at";
    public static final String KEY_LAST_ATTEMPT_AT = "last_attempt_at";
    public static final String KEY_LAST_STATUS = "last_status";

    @TableId(value = "meta_key", type = IdType.INPUT)
    private String metaKey;

    private String metaValue;

    private Date updateTime;
}
