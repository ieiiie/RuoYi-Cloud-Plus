package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 后台农事任务联合分页的数据库投影。
 *
 * <p>该对象只承载任务包和拆分工单 {@code UNION ALL} 的公共字段，展示摘要由查询服务批量补齐。</p>
 */
@Data
public class SfStaskAdminTaskListRow {

    /**
     * 行类型：PACKAGE-任务包，SPLIT-拆分工单。
     */
    private String taskType;

    /**
     * 外层排序使用的稳定标识。
     */
    private Long recordId;

    /**
     * 任务包 ID。
     */
    private Long packageId;

    /**
     * 任务包编号。
     */
    private String packageNo;

    /**
     * 拆分工单 ID。
     */
    private Long orderId;

    /**
     * 拆分工单编号。
     */
    private String orderNo;

    /**
     * 创建人员工 ID。
     */
    private Long creatorEmployeeId;

    /**
     * 创建人 stask 应用角色编码。
     */
    private String creatorRoleCode;

    /**
     * 计划作业日期。
     */
    private Date planDate;

    /**
     * 任务状态。
     */
    private String status;

    /**
     * 拆分工单代表大棚 ID。
     */
    private Long greenhouseId;

    /**
     * 拆分工单代表大棚名称快照。
     */
    private String greenhouseNameSnapshot;

    /**
     * 拆分工单农事项 ID。
     */
    private Long workItemId;

    /**
     * 拆分工单农事项名称快照。
     */
    private String workItemNameSnapshot;

    /**
     * 拆分工单组长员工 ID。
     */
    private Long leaderId;

    /**
     * 组长接单填写的工人数量，不含组长。
     */
    private Double requiredWorkerCount;

    /**
     * 创建时间。
     */
    private Date createTime;
}
