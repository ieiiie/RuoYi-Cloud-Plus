package com.ym.agriculture.farmtask.inventory.support;

import com.ym.agriculture.farmtask.inventory.InventoryBusinessException;
import com.ym.agriculture.farmtask.inventory.InventoryConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 库存数量定点规则的唯一入口。 */
public final class InventoryQuantity {

    public static final BigDecimal ZERO = new BigDecimal("0.0");

    public static BigDecimal positive(BigDecimal value, String fieldName) {
        BigDecimal normalized = scale(value, fieldName);
        if (normalized.compareTo(ZERO) <= 0) {
            throw InventoryBusinessException.rule(fieldName + "必须大于0");
        }
        return normalized;
    }

    public static BigDecimal nonNegative(BigDecimal value, String fieldName) {
        BigDecimal normalized = scale(value, fieldName);
        if (normalized.compareTo(ZERO) < 0) {
            throw InventoryBusinessException.rule(fieldName + "不能小于0");
        }
        return normalized;
    }

    public static BigDecimal signed(BigDecimal value, String fieldName) {
        return scale(value, fieldName);
    }

    public static BigDecimal scale(BigDecimal value, String fieldName) {
        if (value == null) {
            throw InventoryBusinessException.rule(fieldName + "不能为空");
        }
        if (value.stripTrailingZeros().scale() > InventoryConstants.QUANTITY_SCALE) {
            throw InventoryBusinessException.rule(fieldName + "最多保留一位小数");
        }
        return value.setScale(InventoryConstants.QUANTITY_SCALE, RoundingMode.UNNECESSARY);
    }

    private InventoryQuantity() {
    }
}
