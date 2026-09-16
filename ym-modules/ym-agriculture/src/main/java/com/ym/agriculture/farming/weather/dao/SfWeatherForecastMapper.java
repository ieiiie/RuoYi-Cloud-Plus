package com.ym.agriculture.farming.weather.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.weather.model.entity.SfWeatherForecast;

import java.util.List;

/**
 * {@code sf_weather_forecast} Mapper。
 */
public interface SfWeatherForecastMapper extends BaseMapper<SfWeatherForecast> {

    default SfWeatherForecast selectLatestOneByAdcode(String adcode) {
        return selectOne(Wrappers.<SfWeatherForecast>lambdaQuery()
            .eq(SfWeatherForecast::getAdcode, adcode)
            .orderByDesc(SfWeatherForecast::getPullTime)
            .last("LIMIT 1"));
    }

    default List<SfWeatherForecast> selectListBySyncIdOrderByCastDateAsc(Long syncId) {
        return selectList(Wrappers.<SfWeatherForecast>lambdaQuery()
            .eq(SfWeatherForecast::getSyncId, syncId)
            .orderByAsc(SfWeatherForecast::getCastDate));
    }

    default int deleteByAdcode(String adcode) {
        return delete(Wrappers.<SfWeatherForecast>lambdaQuery()
            .eq(SfWeatherForecast::getAdcode, adcode));
    }

    /**
     * 删除指定 adcode 下 {@code cast_date >= castDateFrom} 的预报行（同步时覆盖当天及未来）。
     */
    default int deleteByAdcodeAndCastDateFrom(String adcode, String castDateFrom) {
        return delete(Wrappers.<SfWeatherForecast>lambdaQuery()
            .eq(SfWeatherForecast::getAdcode, adcode)
            .ge(SfWeatherForecast::getCastDate, castDateFrom));
    }

    /**
     * 查询指定 adcode 下 {@code cast_date < castDateBefore} 的历史预报，按日期升序。
     */
    default List<SfWeatherForecast> selectListByAdcodeAndCastDateBeforeOrderByCastDateAsc(
        String adcode, String castDateBefore) {
        return selectList(Wrappers.<SfWeatherForecast>lambdaQuery()
            .eq(SfWeatherForecast::getAdcode, adcode)
            .lt(SfWeatherForecast::getCastDate, castDateBefore)
            .orderByAsc(SfWeatherForecast::getCastDate));
    }
}
