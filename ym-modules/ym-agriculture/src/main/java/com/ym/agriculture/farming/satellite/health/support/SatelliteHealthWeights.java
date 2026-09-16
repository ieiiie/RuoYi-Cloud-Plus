package com.ym.agriculture.farming.satellite.health.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ym.common.core.constant.TenantConstants;
import com.ym.system.api.RemoteConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 从平台租户读取并校验卫星健康评分权重。 */
@Slf4j
@Component
public class SatelliteHealthWeights {

    /** 平台级参数键。 */
    public static final String CONFIG_KEY = "smartfarming.satellite.health-score.weights";

    /** 评分指标固定顺序。 */
    public static final List<String> TYPES = List.of(
        "growth", "chlorophyll", "nitrogen", "droughtlevel", "soilmoisture", "health", "seedlinggrowth");

    private static final Map<String, BigDecimal> DEFAULT_WEIGHTS = Map.of(
        "growth", new BigDecimal("20"), "soilmoisture", new BigDecimal("30"), "nitrogen", new BigDecimal("25"),
        "seedlinggrowth", new BigDecimal("10"), "health", new BigDecimal("5"), "chlorophyll", new BigDecimal("5"),
        "droughtlevel", new BigDecimal("5"));

    @DubboReference
    private RemoteConfigService configService;

    /** 获取平台统一权重；配置异常时回退内置默认值并记录告警。 */
    public Map<String, BigDecimal> getWeights() {
        try {
            String config = configService.getConfigValue(TenantConstants.DEFAULT_TENANT_ID, CONFIG_KEY);
            return validate(config);
        } catch (Exception e) {
            log.warn("卫星健康评分权重配置异常，已回退默认权重", e);
            return DEFAULT_WEIGHTS;
        }
    }

    /** 校验 JSON 结构、七项完整性、非负性及 100.0000 总和。 */
    public static Map<String, BigDecimal> validate(String configValue) {
        JSONObject object = JSON.parseObject(configValue);
        if (object == null || object.size() != TYPES.size() || !object.keySet().containsAll(TYPES)) {
            throw new IllegalArgumentException("权重必须完整包含七个卫星评分指标");
        }
        LinkedHashMap<String, BigDecimal> weights = new LinkedHashMap<>();
        for (String type : TYPES) {
            BigDecimal value = object.getBigDecimal(type);
            if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("权重必须为非负数");
            }
            weights.put(type, value);
        }
        BigDecimal total = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("100.0000")) != 0) {
            throw new IllegalArgumentException("七项权重合计必须严格等于 100.0000");
        }
        return weights;
    }
}
