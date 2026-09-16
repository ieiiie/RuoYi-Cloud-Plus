package com.ym.agriculture.farming.weatheralert.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.weatheralert.model.entity.SfWeatherAlert;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/** {@code sf_weather_alert} Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfWeatherAlertMapper extends BaseMapper<SfWeatherAlert> {

    default SfWeatherAlert selectByWarningId(String warningId) {
        return selectById(warningId);
    }

    default List<SfWeatherAlert> selectActiveByProvince(String provinceCode) {
        return selectList(Wrappers.<SfWeatherAlert>lambdaQuery()
            .eq(SfWeatherAlert::getActiveFlag, 1)
            .eq(SfWeatherAlert::getProvinceCode, provinceCode)
            .orderByDesc(SfWeatherAlert::getEffectiveAt));
    }

    default List<SfWeatherAlert> selectActiveAll() {
        return selectList(Wrappers.<SfWeatherAlert>lambdaQuery()
            .eq(SfWeatherAlert::getActiveFlag, 1)
            .orderByDesc(SfWeatherAlert::getEffectiveAt));
    }

    default void bumpMissingForProvinceExcept(String provinceCode, Collection<String> seenIds, Date now) {
        List<SfWeatherAlert> actives = selectActiveByProvince(provinceCode);
        for (SfWeatherAlert row : actives) {
            if (seenIds != null && seenIds.contains(row.getWarningId())) {
                continue;
            }
            int missing = row.getMissingCount() == null ? 0 : row.getMissingCount();
            row.setMissingCount(missing + 1);
            row.setUpdateTime(now);
            updateById(row);
        }
    }
}
