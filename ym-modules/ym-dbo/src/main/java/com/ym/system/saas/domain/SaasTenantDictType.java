package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/** Dbo 维护的租户字典类型定义。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_dict_type")
public class SaasTenantDictType extends BaseEntity {
    @TableId("dict_id")
    private Long dictId;
    private String dictName;
    private String dictType;
    private String remark;
}
