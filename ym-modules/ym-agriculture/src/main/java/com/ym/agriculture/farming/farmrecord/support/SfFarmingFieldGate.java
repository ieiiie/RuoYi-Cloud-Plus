package com.ym.agriculture.farming.farmrecord.support;

import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.entity.SfField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 校验地块存在且与当前租户一致（农事等业务共用）。
 */
@Component
@RequiredArgsConstructor
public class SfFarmingFieldGate {

    private final SfFieldMapper fieldMapper;

    /**
     * @param fieldId  地块主键
     * @param tenantId 当前租户编号
     */
    public void assertAccessible(Long fieldId, String tenantId) {
        SfField f = fieldMapper.selectById(fieldId);
        if (f == null || !SystemConstants.NORMAL.equals(f.getDelFlag())) {
            throw new ServiceException("地块不存在");
        }
        if (!tenantId.equals(f.getTenantId())) {
            throw new ServiceException("地块不属于当前租户");
        }
    }
}
