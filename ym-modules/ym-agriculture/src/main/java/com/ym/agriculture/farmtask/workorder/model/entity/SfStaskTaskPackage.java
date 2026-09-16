package com.ym.agriculture.farmtask.workorder.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * stask 任务包主表，表 {@code sf_stask_task_package}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_stask_task_package")
public class SfStaskTaskPackage extends TenantEntity {

    /**
     * 任务包主键。
     */
    @TableId("package_id")
    private Long packageId;

    /**
     * 任务包编号。
     */
    private String packageNo;

    /**
     * 创建人员工ID。
     */
    private Long creatorEmployeeId;

    /**
     * 创建人角色编码。
     */
    private String creatorRoleCode;

    /**
     * 最终经手技术员员工ID；技术员自建任务或成功技术审核时写入。
     */
    private Long handlerTechnicianEmployeeId;

    /**
     * 经手技术员姓名快照，仅用于展示，权限以员工ID为准。
     */
    private String handlerTechnicianEmployeeNameSnapshot;

    /**
     * 计划作业日期。
     */
    private Date planDate;

    /**
     * 整体技术说明。
     */
    private String overallTechNote;

    /**
     * 任务包状态。
     */
    private String status;

    /**
     * 乐观锁版本。
     */
    @Version
    private Integer version;

    /**
     * 备注。
     */
    private String remark;
}
