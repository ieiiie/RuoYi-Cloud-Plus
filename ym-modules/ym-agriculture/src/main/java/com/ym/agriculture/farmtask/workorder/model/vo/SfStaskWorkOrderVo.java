package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.math.BigDecimal;

/**
 * stask 工单列表视图对象。
 */
@Data
public class SfStaskWorkOrderVo {

    /**
     * 工单主键。
     */
    private Long orderId;

    /**
     * 工单编号。
     */
    private String orderNo;

    /**
     * 任务包ID。
     */
    private Long packageId;

    /**
     * 派工明细ID，工人列表使用。
     */
    private Long dispatchId;

    /**
     * 派工状态，工人列表使用。
     */
    private String dispatchStatus;

    /**
     * 计划作业日期，格式：yyyy-MM-dd。
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date planDate;

    /**
     * 大棚ID，拆分工单单棚；任务包头为 null。
     */
    private Long greenhouseId;

    /**
     * 大棚名称快照。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "greenhouseNameSnapshot")
    private String greenhouseNameSnapshot;

    /**
     * 任务包头关联的大棚列表；拆分工单可为 null 或空。
     */
    private List<SfStaskGreenhouseBriefVo> greenhouses;

    /**
     * 拆分工单单棚进行中的种植批次列表，与 {@link #greenhouseId} 对应；无批次或任务包时为 empty。
     */
    private List<SfPlantingBatchVo> plantingBatches;

    /**
     * 农事项目 ID，拆分工单有值。
     */
    private Long workItemId;

    /**
     * 农事项目名称快照。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "workItemNameSnapshot")
    private String workItemNameSnapshot;

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
     * 创建人员工ID。
     */
    private Long creatorEmployeeId;

    /**
     * 创建人姓名。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "creatorEmployeeId", fieldKey = "name")
    private String creatorEmployeeName;

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
     * 当前登录人是否与任务相关（本人发起或本人经手）。
     */
    private Boolean relatedToCurrentUser;

    /**
     * 工单状态。
     */
    private String status;

    /**
     * 状态展示文案（主页列表用，如：待验收、验收通过）。
     */
    private String statusLabel;

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
     * 完工时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date completedAt;

    /**
     * 主页列表展示时间（与状态对应：完成时间、派工完成时间、到达时间、验收时间等）。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date eventTime;

    /**
     * 主页列表时间行前缀文案，如：完成时间、派工完成时间、验收时间。
     */
    private String eventTimeLabel;

    /**
     * 距发出任务小时数（待组长接单/待组长派工）。
     */
    private Long elapsedHours;

    /**
     * 主操作编码（进行中列表用）：ACCEPT-去验收等。
     */
    private String primaryAction;

    /**
     * 组长接单时填写的工人数量（不含组长），作为后续结算人工取数依据。
     */
    private Double requiredWorkerCount;

    /**
     * 组长在该计划日维护的正式用工人数，不含组长本人。
     */
    private BigDecimal dailyLaborCount;

    /**
     * 当前组长和计划日对应的日用工记录乐观锁版本。
     */
    private Long laborRecordVersion;

    /**
     * 已接受工人数。
     */
    private Integer acceptedWorkerCount;

    /**
     * 创建时间。
     */
    private Date createTime;
}
