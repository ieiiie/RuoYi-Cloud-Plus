package com.ym.agriculture.farming.market.support;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ym.agriculture.farming.market.config.SfMarketProperties;
import com.ym.agriculture.farming.market.model.constants.SfMarketConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 农业行情涨跌计算及价格跳变告警。 */
@Component
@RequiredArgsConstructor
public class SfMarketPriceSupport {

    private final SfMarketProperties properties;

    /** 计算上一期价格、涨跌额、涨跌幅和趋势。 */
    public Calculation calculate(BigDecimal previousPrice, BigDecimal price) {
        if (previousPrice == null || previousPrice.compareTo(BigDecimal.ZERO) == 0) {
            return new Calculation(null, null, null, SfMarketConstants.TREND_UNKNOWN);
        }
        BigDecimal amount = price.subtract(previousPrice).setScale(6, RoundingMode.HALF_UP);
        BigDecimal percent = amount.divide(previousPrice, 8, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_UP);
        String trend = amount.compareTo(BigDecimal.ZERO) > 0 ? SfMarketConstants.TREND_UP
            : amount.compareTo(BigDecimal.ZERO) < 0 ? SfMarketConstants.TREND_DOWN : SfMarketConstants.TREND_FLAT;
        return new Calculation(previousPrice, amount, percent, trend);
    }

    /** 超过50%时生成只告警、不阻断发布的 JSON。 */
    public String warningJson(BigDecimal changePercent) {
        if (changePercent == null || changePercent.abs().compareTo(properties.getJumpWarningPercent()) <= 0) {
            return null;
        }
        JSONArray warnings = new JSONArray();
        JSONObject warning = new JSONObject();
        warning.put("code", "PRICE_JUMP");
        warning.put("message", "较上一有效报价涨跌超过50%，请核验来源和单位");
        warning.put("changePercent", changePercent);
        warnings.add(warning);
        return warnings.toJSONString();
    }

    /** 一次涨跌计算结果。 */
    public record Calculation(BigDecimal previousPrice, BigDecimal changeAmount,
                              BigDecimal changePercent, String trend) {
    }
}
