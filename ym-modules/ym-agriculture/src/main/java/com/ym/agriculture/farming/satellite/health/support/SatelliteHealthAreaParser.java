package com.ym.agriculture.farming.satellite.health.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.ym.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** 解析遥感回调中的五档面积数组，并集中执行有效性校验。 */
@Component
public class SatelliteHealthAreaParser {

    /** 面积数组必须恰有五个非负数且总和大于零。 */
    public List<BigDecimal> parseValidArea(String areaJson) {
        if (StringUtils.isBlank(areaJson)) {
            return List.of();
        }
        try {
            JSONArray array = JSON.parseArray(areaJson);
            if (array == null || array.size() != 5) {
                return List.of();
            }
            List<BigDecimal> values = array.stream().map(this::toNonNegativeDecimal).toList();
            if (values.stream().anyMatch(value -> value == null)
                || values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.ZERO) <= 0) {
                return List.of();
            }
            return values;
        } catch (JSONException | NumberFormatException e) {
            return List.of();
        }
    }

    /** 云量兼容 0~1 小数和 0~100 百分数两种回调格式。 */
    public BigDecimal parseCloudCoverPercent(String areaJson) {
        if (StringUtils.isBlank(areaJson)) {
            return null;
        }
        try {
            JSONArray array = JSON.parseArray(areaJson);
            if (array == null || array.isEmpty()) {
                return null;
            }
            BigDecimal value = toNonNegativeDecimal(array.get(0));
            if (value == null) {
                return null;
            }
            if (value.compareTo(BigDecimal.ONE) <= 0) {
                return value.multiply(BigDecimal.valueOf(100));
            }
            return value.compareTo(BigDecimal.valueOf(100)) <= 0 ? value : null;
        } catch (JSONException | NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal toNonNegativeDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            BigDecimal decimal = new BigDecimal(String.valueOf(value));
            return decimal.compareTo(BigDecimal.ZERO) >= 0 ? decimal : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
