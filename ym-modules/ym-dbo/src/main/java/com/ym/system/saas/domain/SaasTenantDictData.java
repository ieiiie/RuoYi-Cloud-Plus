package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/** 租户实际字典值。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_dict_data")
public class SaasTenantDictData extends BaseEntity {
    @TableId("dict_code")
    private Long dictCode;
    private String tenantId;
    private Integer dictSort;
    private String dictLabel;
    private String dictValue;
    private String dictType;
    private String cssClass;
    private String listClass;
    private String isDefault;
    private String remark;
}
