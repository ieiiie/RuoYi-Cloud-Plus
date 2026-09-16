package com.ym.agriculture.farming.weatheralert.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ym.agriculture.farming.weatheralert.model.entity.SfWeatherAlertSyncMeta;

/** {@code sf_weather_alert_sync_meta} Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfWeatherAlertSyncMetaMapper extends BaseMapper<SfWeatherAlertSyncMeta> {
}
