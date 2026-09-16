package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.tenant.core.TenantEntity;

/** 每个租户独立维护的字典值。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_dict_data")
public class SysTenantDictData extends TenantEntity {
    @TableId("dict_code")
    private Long dictCode;
    private Integer dictSort;
    private String dictLabel;
    private String dictValue;
    private String dictType;
    private String cssClass;
    private String listClass;
    private String isDefault;
    private String remark;
}
