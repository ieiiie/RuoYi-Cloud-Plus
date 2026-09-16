package com.ym.agriculture.farming.satellite.health.config;

import com.ym.agriculture.farming.satellite.health.support.SatelliteHealthWeights;
import org.springframework.stereotype.Component;

/** 卫星健康评分权重的领域校验器。 */
@Component
public class SatelliteHealthWeightConfigValidator {

    public boolean supports(String configKey) {
        return SatelliteHealthWeights.CONFIG_KEY.equals(configKey);
    }

    public void validate(String configValue) {
        SatelliteHealthWeights.validate(configValue);
    }
}
