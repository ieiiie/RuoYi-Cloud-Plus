package com.ym.agriculture.farming.field.layout.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 大棚布局棚位。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_greenhouse_layout_item")
public class SfGreenhouseLayoutItem extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("item_id")
    private Long itemId;
    private Long layoutId;
    private Long columnId;
    private Long fieldId;
    private Integer rowOrder;
}
