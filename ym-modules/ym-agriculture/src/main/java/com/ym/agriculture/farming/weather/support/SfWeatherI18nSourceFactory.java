package com.ym.agriculture.farming.weather.support;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.farming.weather.model.entity.SfWeatherForecast;
import com.ym.agriculture.farming.weather.model.entity.SfWeatherLive;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherForecastVo;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherLiveVo;

import java.util.ArrayList;
import java.util.List;

/** 提取天气响应中允许进入中维翻译核心的动态中文。 */
public final class SfWeatherI18nSourceFactory {

    private SfWeatherI18nSourceFactory() {
    }

    public static List<I18nTextSource> sources(SfWeatherForecast row) {
        if (row == null) {
            return List.of();
        }
        return forecastSources(row.getForecastId(), row.getProvince(), row.getCityName(), row.getDayWeather(),
            row.getNightWeather(), row.getDayWind(), row.getNightWind());
    }

    public static List<I18nTextSource> sources(SfWeatherForecastVo row) {
        if (row == null) {
            return List.of();
        }
        return forecastSources(row.getForecastId(), row.getProvince(), row.getCityName(), row.getDayWeather(),
            row.getNightWeather(), row.getDayWind(), row.getNightWind());
    }

    public static List<I18nTextSource> sources(SfWeatherLive row) {
        if (row == null) {
            return List.of();
        }
        return liveSources(row.getLiveId(), row.getProvince(), row.getCityName(), row.getWeather(),
            row.getWindDirection());
    }

    public static List<I18nTextSource> sources(SfWeatherLiveVo row) {
        if (row == null) {
            return List.of();
        }
        return liveSources(row.getLiveId(), row.getProvince(), row.getCityName(), row.getWeather(),
            row.getWindDirection());
    }

    private static List<I18nTextSource> forecastSources(Long id, String province, String cityName,
        String dayWeather, String nightWeather, String dayWind, String nightWind) {
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.WEATHER_FORECAST, id, "province", province);
        add(sources, I18nResourceType.WEATHER_FORECAST, id, "cityName", cityName);
        add(sources, I18nResourceType.WEATHER_FORECAST, id, "dayWeather", dayWeather);
        add(sources, I18nResourceType.WEATHER_FORECAST, id, "nightWeather", nightWeather);
        add(sources, I18nResourceType.WEATHER_FORECAST, id, "dayWind", dayWind);
        add(sources, I18nResourceType.WEATHER_FORECAST, id, "nightWind", nightWind);
        return List.copyOf(sources);
    }

    private static List<I18nTextSource> liveSources(Long id, String province, String cityName, String weather,
        String windDirection) {
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, I18nResourceType.WEATHER_LIVE, id, "province", province);
        add(sources, I18nResourceType.WEATHER_LIVE, id, "cityName", cityName);
        add(sources, I18nResourceType.WEATHER_LIVE, id, "weather", weather);
        add(sources, I18nResourceType.WEATHER_LIVE, id, "windDirection", windDirection);
        return List.copyOf(sources);
    }

    private static void add(List<I18nTextSource> sources, String resourceType, Long resourceId, String fieldKey,
        String sourceText) {
        if (StringUtils.isNotBlank(sourceText)) {
            sources.add(new I18nTextSource(resourceType, resourceId, fieldKey, sourceText));
        }
    }
}
