package com.ym.agriculture.farming.weather.service;

import com.ym.agriculture.farming.weather.model.vo.SfWeatherLatestVo;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherLiveVo;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherSyncResultVo;

/**
 * 高德天气：拉取、落库、查询。
 */
public interface ISfWeatherService {

    /**
     * 调用高德 weatherInfo（{@code extensions=all}）写入预报表。
     * 返回的 {@code casts} 中第 1 条为当天，其后为未来数日（通常再 3 天）。
     * 落库前仅删除该 adcode 下 {@code cast_date >= 今天} 的行，今天之前的历史预报保留。
     */
    SfWeatherSyncResultVo syncFromAmap(String adcode);

    /**
     * 当前登录租户：按 {@code sys_tenant.district_code} 解析 adcode；
     * 预报读库（依赖定时任务同步），实况按日时段边界（默认 6/12/18/0 点）按需拉取。
     */
    SfWeatherLatestVo getLatest();

    /**
     * 按 adcode 获取实况：当前日时段内已有数据则读库，否则调用高德 {@code extensions=base} 落库后返回。
     */
    SfWeatherLiveVo getLiveByAdcode(String adcode);
}
