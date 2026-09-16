package com.ym.agriculture.farming.field.layout.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 租户大棚二维布局根记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_greenhouse_layout")
public class SfGreenhouseLayout extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("layout_id")
    private Long layoutId;
    private Long version;
}
