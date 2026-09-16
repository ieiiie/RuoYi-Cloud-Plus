package com.ym.agriculture.farmtask.workorder.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * stask 工单主表，表 {@code sf_stask_work_order}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_stask_work_order")
public class SfStaskWorkOrder extends TenantEntity {

    /**
     * 工单主键。
     */
    @TableId("order_id")
    private Long orderId;

    /**
     * 工单编号。
     */
    private String orderNo;

    /**
     * 任务包ID，同一批创建任务共享。
     */
    private Long packageId;

    /**
     * 创建人员工ID。
     */
    private Long creatorEmployeeId;

    /**
     * 创建人角色编码。
     */
    private String creatorRoleCode;

    /**
     * 最终经手技术员员工ID，从任务包继承。
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
     * 大棚ID，拆分后工单必填。
     */
    private Long greenhouseId;

    /**
     * 大棚编码快照。
     */
    private String greenhouseCodeSnapshot;

    /**
     * 大棚名称快照。
     */
    private String greenhouseNameSnapshot;

    /**
     * 农事项目ID，拆分后工单必填。
     */
    private Long workItemId;

    /**
     * 农事项目名称快照。
     */
    private String workItemNameSnapshot;

    /**
     * 农事项目编码快照。
     */
    private String workItemCodeSnapshot;

    /**
     * 农事分类ID快照。
     */
    private Long categoryIdSnapshot;

    /**
     * 农事分类名称快照。
     */
    private String categoryNameSnapshot;

    /**
     * 申请人作业要求。
     */
    private String managerRequirement;

    /**
     * 申请人照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String managerPhotos;

    /**
     * 技术员针对农事项说明。
     */
    private String techInstruction;

    /**
     * 技术员参考照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String techPhotos;

    /**
     * 整体技术说明。
     */
    private String overallTechNote;

    /**
     * 组长员工ID。
     */
    private Long leaderId;

    /**
     * 工单状态。
     */
    private String status;

    /**
     * 组长接单时填写的工人数量（不含组长），作为后续结算人工取数依据。
     */
    private Double requiredWorkerCount;

    /**
     * 已接受工人数。
     */
    private Integer acceptedWorkerCount;

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
