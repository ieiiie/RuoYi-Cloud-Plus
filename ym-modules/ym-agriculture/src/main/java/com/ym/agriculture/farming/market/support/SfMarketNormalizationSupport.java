package com.ym.agriculture.farming.market.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.ym.agriculture.farming.market.config.SfMarketProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;

/** 农业行情品类、单位、来源地址和价格序列标准化。 */
@Component
@RequiredArgsConstructor
public class SfMarketNormalizationSupport {

    private final SfMarketProperties properties;

    /** 将来源品类转换为平台固定品类。 */
    public String normalizeCategory(String sourceCategory) {
        String value = StrUtil.trim(sourceCategory);
        String category = properties.getCategoryAliases().get(value);
        if (category == null) {
            throw new IllegalArgumentException("无法识别商品品类：" + StrUtil.nullToEmpty(sourceCategory));
        }
        return category;
    }

    /** 标准化商品匹配名称。 */
    public String normalizeProductName(String productName) {
        String value = StrUtil.trim(productName);
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException("商品名称不能为空");
        }
        return value.replaceAll("\\s+", "").toLowerCase();
    }

    /** 转换为平台标准价格和单位。 */
    public NormalizedPrice normalizePrice(BigDecimal sourcePrice, String sourceUnit) {
        if (sourcePrice == null || sourcePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("价格必须大于0");
        }
        String unit = StrUtil.trim(sourceUnit);
        if (StrUtil.isBlank(unit)) {
            throw new IllegalArgumentException("计价单位不能为空");
        }
        String lower = unit.toLowerCase();
        if (List.of("元/公斤", "元/kg", "元/千克").contains(lower)) {
            return new NormalizedPrice(sourcePrice.setScale(6, RoundingMode.HALF_UP), "元/公斤");
        }
        if ("元/吨".equals(lower)) {
            return new NormalizedPrice(sourcePrice.multiply(new BigDecimal("0.001")).setScale(6, RoundingMode.HALF_UP), "元/公斤");
        }
        if (List.of("元/斤", "元/500克").contains(lower)) {
            return new NormalizedPrice(sourcePrice.multiply(new BigDecimal("2")).setScale(6, RoundingMode.HALF_UP), "元/公斤");
        }
        if (List.of("元/头", "元/只", "元/羽").contains(lower)) {
            return new NormalizedPrice(sourcePrice.setScale(6, RoundingMode.HALF_UP), unit);
        }
        throw new IllegalArgumentException("未配置计价单位：" + unit);
    }

    /** 校验来源 URL 的协议和域名。 */
    public void validateSourceUrl(String sourceUrl, List<String> allowedDomains) {
        try {
            URI uri = URI.create(sourceUrl);
            if (!List.of("http", "https").contains(StrUtil.nullToEmpty(uri.getScheme()).toLowerCase())) {
                throw new IllegalArgumentException("来源地址只允许HTTP或HTTPS");
            }
            String host = StrUtil.nullToEmpty(uri.getHost()).toLowerCase();
            boolean allowed = allowedDomains.stream().map(String::toLowerCase)
                .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
            if (!allowed) {
                throw new IllegalArgumentException("来源地址域名不在白名单");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("来源地址格式错误");
        }
    }

    /** 生成稳定价格序列摘要。 */
    public String seriesKey(String sourceCode, String externalProductKey, String specification,
                            String quoteType, String origin, String marketName, String unit) {
        List<String> dimensions = List.of(
            StrUtil.nullToEmpty(StrUtil.trim(sourceCode)),
            StrUtil.nullToEmpty(StrUtil.trim(externalProductKey)),
            StrUtil.nullToEmpty(StrUtil.trim(specification)),
            StrUtil.nullToEmpty(StrUtil.trim(quoteType)),
            StrUtil.nullToEmpty(StrUtil.trim(origin)),
            StrUtil.nullToEmpty(StrUtil.trim(marketName)),
            StrUtil.nullToEmpty(StrUtil.trim(unit)));
        return SecureUtil.sha256(JSON.toJSONString(dimensions));
    }

    /** 标准价格及单位。 */
    public record NormalizedPrice(BigDecimal price, String unit) {
    }
}
