package com.ym.agriculture.farming.market.config;

import com.ym.agriculture.farming.market.model.constants.SfMarketConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** 农业行情归一和告警配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "smart-farming.market")
public class SfMarketProperties {
    /** 只告警不阻断发布的绝对涨跌幅阈值，单位%。 */
    private BigDecimal jumpWarningPercent = new BigDecimal("50");
    /** 来源品类名称到平台固定品类的别名映射。 */
    private Map<String, String> categoryAliases = new LinkedHashMap<>(SfMarketConstants.CATEGORY_ALIASES);
}
