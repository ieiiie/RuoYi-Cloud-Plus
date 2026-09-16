package com.ym.agriculture.farmtask.worker.model.vo;

import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * stask 小程序工人最近任务记录视图。
 */
@Data
public class SfStaskWorkerRecentTaskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 派工明细ID。
     */
    private Long dispatchId;

    /**
     * 工单ID。
     */
    private Long orderId;

    /**
     * 工单编号。
     */
    private String orderNo;

    /**
     * 大棚ID。
     */
    private Long greenhouseId;

    /**
     * 大棚名称。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "greenhouseNameSnapshot")
    private String greenhouseName;

    /**
     * 农事项目ID。
     */
    private Long workItemId;

    /**
     * 农事项目名称。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "workItemNameSnapshot")
    private String workItemName;

    /**
     * 计划作业日期。
     */
    private Date planDate;

    /**
     * 验收完成时间。
     */
    private Date completedAt;

    /**
     * 组长员工ID。
     */
    private Long leaderId;

    /**
     * 组长姓名。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "leaderId", fieldKey = "name")
    private String leaderName;

    /**
     * 组长评价编码。
     */
    private String leaderEvaluation;

    /**
     * 组长评价名称。
     */
    private String leaderEvaluationLabel;
}
