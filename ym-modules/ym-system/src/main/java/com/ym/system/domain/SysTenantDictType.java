package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/** Dbo 定义、SaaS 只读的租户字典类型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_dict_type")
public class SysTenantDictType extends BaseEntity {
    @TableId("dict_id")
    private Long dictId;
    private String dictName;
    private String dictType;
    private String remark;
}
