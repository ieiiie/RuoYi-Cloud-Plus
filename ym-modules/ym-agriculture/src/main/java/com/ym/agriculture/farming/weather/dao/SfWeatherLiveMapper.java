package com.ym.agriculture.farming.weather.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.weather.model.entity.SfWeatherLive;

/**
 * {@code sf_weather_live} Mapper。
 */
@InterceptorIgnore(tenantLine = "true")
public interface SfWeatherLiveMapper extends BaseMapper<SfWeatherLive> {

    default SfWeatherLive selectLatestOneByAdcode(String adcode) {
        return selectOne(Wrappers.<SfWeatherLive>lambdaQuery()
            .eq(SfWeatherLive::getAdcode, adcode)
            .orderByDesc(SfWeatherLive::getPullTime)
            .last("LIMIT 1"));
    }
}
