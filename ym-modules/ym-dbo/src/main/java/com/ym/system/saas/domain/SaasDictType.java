package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * SaaS 全局字典类型。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SaasDictType extends BaseEntity {
    @TableId("dict_id")
    private Long dictId;
    private String dictName;
    private String dictType;
    private String remark;
}
