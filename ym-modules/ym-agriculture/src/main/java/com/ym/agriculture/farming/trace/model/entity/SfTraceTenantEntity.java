package com.ym.agriculture.farming.trace.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 溯源模块实体基类：溯源表不含 {@code create_dept} 列，屏蔽父类字段映射。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class SfTraceTenantEntity extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField(exist = false)
    private Long createDept;
}
