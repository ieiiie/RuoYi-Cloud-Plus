package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.shared.i18n.StaskI18nComposite;
import com.ym.agriculture.farmtask.i18n.StaskI18nTextCompositions;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 组长工作台拆分工单列表项。
 */
@Data
public class SfStaskLeaderWorkbenchItemVo implements StaskI18nComposite {

    /**
     * 卡片类型，固定为 SPLIT。
     */
    private String cardType;

    /**
     * 工单 ID。
     */
    private Long orderId;

    /**
     * 工单编号。
     */
    private String orderNo;

    /**
     * 任务包 ID。
     */
    private Long packageId;

    /**
     * 列表标题：大棚名称 · 农事项目名称。
     */
    private String title;

    /**
     * 计划作业日期，格式：yyyy-MM-dd。
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date planDate;

    /**
     * 工单状态编码。
     */
    private String status;

    /**
     * 状态展示文案。
     */
    private String statusLabel;

    /**
     * 主操作：LEADER_ACCEPT-接单，DISPATCH-安排工人，CLOCK_IN-到岗打卡，APPLY_COMPLETE-申请验收，REAPPLY_ACCEPTANCE-重新申请验收。
     */
    private String primaryAction;

    /**
     * 领导视角始终为只读。
     */
    private Boolean relatedToCurrentUser;
    private Boolean canAcceptance;
    private Boolean canTechConfirm;
    private Boolean canTechReject;
    private String readonlyReason;

    /**
     * 是否可展示到岗打卡（派工完成且计划日期为当日）。
     */
    private Boolean clockInEnabled;

    /**
     * 大棚 ID。
     */
    private Long greenhouseId;

    /**
     * 大棚名称快照。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "greenhouseNameSnapshot")
    private String greenhouseNameSnapshot;

    /**
     * 该大棚进行中的种植批次列表；无则为空数组。
     */
    private List<SfPlantingBatchVo> plantingBatches;

    /**
     * 农事项目名称快照。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "workItemNameSnapshot")
    private String workItemNameSnapshot;

    /**
     * 农事项目 ID。
     */
    private Long workItemId;

    /**
     * 创建人员工 ID。
     */
    private Long creatorEmployeeId;

    /**
     * 创建人姓名。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "creatorEmployeeId", fieldKey = "name")
    private String creatorEmployeeName;

    /**
     * 创建人角色编码。
     */
    private String creatorRoleCode;

    /**
     * 创建人角色名称，对应 ym.employee.app-roles 配置中的 name。
     */
    private String creatorRoleName;

    /**
     * 经手技术员员工ID。
     */
    private Long handlerTechnicianEmployeeId;

    /**
     * 经手技术员姓名。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "handlerTechnicianEmployeeId", fieldKey = "name")
    private String handlerTechnicianEmployeeName;

    /**
     * 组长员工 ID。
     */
    private Long leaderId;

    /**
     * 组长姓名。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "leaderId", fieldKey = "name")
    private String leaderName;

    /**
     * 组长接单时填写的工人数量（不含组长）。
     */
    private Double requiredWorkerCount;

    /** 组长计划日用工人数，兼容字段 requiredWorkerCount 与其值一致。 */
    private java.math.BigDecimal dailyLaborCount;

    /** 当前组长和计划日对应的日用工记录乐观锁版本。 */
    private Long laborRecordVersion;

    /**
     * 已接受工人数。
     */
    private Integer acceptedWorkerCount;

    /**
     * 列表展示时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date eventTime;

    /**
     * 时间行前缀文案。
     */
    private String eventTimeLabel;

    /**
     * 距发出任务小时数（待接单/进行中）。
     */
    private Long elapsedHours;

    /**
     * 完工时间（待验收）。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date completedAt;

    /**
     * 最近一次验收结果：PASS-通过，REJECT-不通过；重新申请验收后仍可有值。
     */
    private String acceptanceResult;

    /**
     * 最近一次验收时间；存在验收历史时有值。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date acceptedAt;

    /**
     * 使用本地化后的大棚与农事名称重建标题。
     */
    @Override
    public void rebuildLocalizedText() {
        title = StaskI18nTextCompositions.splitTitle(greenhouseNameSnapshot, workItemNameSnapshot);
    }
}
