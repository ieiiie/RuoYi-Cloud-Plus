package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * SaaS 租户部门，仅供新租户初始化。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SaasDept extends BaseEntity {
    @TableId("dept_id")
    private Long deptId;
    private String tenantId;
    private Long parentId;
    private String ancestors;
    private String deptName;
    private Integer orderNum;
    private Long leader;
    private String phone;
    private String status;
    @TableLogic
    private String delFlag;
}
