package com.ym.common.tenant.core;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * 带租户标识的实体基类。
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantEntity extends BaseEntity {

    /**
     * 租户编号。
     */
    private String tenantId;

}
