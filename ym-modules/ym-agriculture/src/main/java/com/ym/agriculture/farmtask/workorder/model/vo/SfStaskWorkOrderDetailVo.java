package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.farmtask.voice.model.vo.SfStaskVoiceBroadcastVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * stask 工单详情视图对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SfStaskWorkOrderDetailVo extends SfStaskWorkOrderVo {

    /**
     * 关联领料单ID；拆分工单返回唯一关联单，任务包头为 null。
     */
    private Long materialReceiptId;

    /**
     * 关联领料单号；拆分工单返回唯一关联单，任务包头为 null。
     */
    private String materialReceiptNo;

    /**
     * 申请人作业要求。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "managerRequirement")
    private String managerRequirement;

    /**
     * 申请人照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String managerPhotos;

    /**
     * 技术员针对农事项说明。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "techInstruction")
    private String techInstruction;

    /**
     * 技术员参考照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String techPhotos;

    /**
     * 整体技术说明。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "overallTechNote")
    @StaskI18nField(resourceType = I18nResourceType.STASK_TASK_PACKAGE,
        idProperty = "packageId", fieldKey = "overallTechNote")
    private String overallTechNote;

    /**
     * 是否展示技术补充说明区块（技术员说明/照片/整体说明）。
     * 执行侧（组长/工人）查看时：生产管理员发起且存在技术补充内容为 true，技术员自行发起为 false。
     */
    private Boolean showTechSupplement;

    /**
     * 创建人角色编码。
     */
    private String creatorRoleCode;

    /**
     * 创建人角色名称，对应 ym.employee.app-roles 配置中的 name。
     */
    private String creatorRoleName;

    /**
     * 当前登录人是否可编辑生产侧字段。
     */
    private Boolean editable;

    /**
     * 当前登录人是否可保存草稿。
     */
    private Boolean canSaveDraft;

    /**
     * 当前登录人是否可提交或重新提交。
     */
    private Boolean canSubmit;

    /**
     * 当前登录人是否可删除或撤销。
     */
    private Boolean canDelete;

    /**
     * 当前登录人是否可撤回为草稿。
     */
    private Boolean canWithdraw;

    /**
     * 当前登录人是否可作废。
     */
    private Boolean canVoid;

    /**
     * 只读原因。
     */
    private String readonlyReason;

    /**
     * 任务包农事项编辑回显列表。
     */
    private List<SfStaskPackageItemVo> packageItems;

    /**
     * 待确认工人数。
     */
    private Long pendingWorkerCount;

    /**
     * 已拒绝工人数。
     */
    private Long rejectedWorkerCount;

    /**
     * 当前登录人是否可派工。
     */
    private Boolean canDispatch;

    /**
     * 当前登录人是否可选择「不需要工人」完成派工。
     */
    private Boolean canDispatchWithoutWorkers;

    /**
     * 派工区只读原因。
     */
    private String dispatchReadonlyReason;

    /**
     * 作业照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String workPhotos;

    /**
     * 作业完成备注。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_COMPLETION,
        idProperty = "completionId", fieldKey = "completionRemark")
    private String completionRemark;

    /**
     * 完工记录主键，仅用于服务端定位翻译资源。
     */
    @JsonIgnore
    private Long completionId;

    /**
     * 完工提交时间。
     */
    private Date completedAt;

    /**
     * 完工资料历史版本，按提交时间倒序排列。
     */
    private List<SfStaskCompletionHistoryVo> completionHistory;

    /**
     * 验收结果：PASS-通过 REJECT-不通过。
     */
    private String acceptanceResult;

    /**
     * 不合格原因。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_ACCEPTANCE,
        idProperty = "acceptanceId", fieldKey = "rejectReason")
    private String rejectReason;

    /**
     * 验收记录主键，仅用于服务端定位翻译资源。
     */
    @JsonIgnore
    private Long acceptanceId;

    /**
     * 最新一次技术退回原因；status=TECH_REJECTED 时有值。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_FLOW_LOG,
        idProperty = "techRejectLogId", fieldKey = "remark")
    private String techRejectReason;

    /**
     * 技术退回日志主键，仅用于服务端定位翻译资源。
     */
    @JsonIgnore
    private Long techRejectLogId;

    /**
     * 技术退回时间；status=TECH_REJECTED 时有值。
     */
    private Date techRejectedAt;

    /**
     * 技术退回操作人姓名；status=TECH_REJECTED 时有值。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "techRejectOperatorEmployeeId", fieldKey = "name")
    private String techRejectOperatorEmployeeName;

    /**
     * 技术退回操作人员工 ID，仅用于服务端定位姓名翻译资源。
     */
    @JsonIgnore
    private Long techRejectOperatorEmployeeId;

    /**
     * 验收照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String acceptancePhotos;

    /**
     * 验收时间。
     */
    private Date acceptedAt;

    /**
     * 验收人员工ID。
     */
    private Long acceptorEmployeeId;

    /**
     * 验收人姓名。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "acceptorEmployeeId", fieldKey = "name")
    private String acceptorEmployeeName;

    /**
     * 验收人角色编码。
     */
    private String acceptorRoleCode;

    /**
     * 验收人角色名称，对应 ym.employee.app-roles 配置中的 name。
     */
    private String acceptorRoleName;

    /**
     * 当前登录人是否可提交验收。
     */
    private Boolean canAcceptance;

    /**
     * 当前登录人是否可技术确认通过。
     */
    private Boolean canTechConfirm;

    /**
     * 当前登录人是否可技术退回。
     */
    private Boolean canTechReject;

    /**
     * 验收操作区是否可编辑。
     */
    private Boolean acceptanceEditable;

    /**
     * 验收区只读原因。
     */
    private String acceptanceReadonlyReason;

    /**
     * 派工明细。
     */
    private List<SfStaskDispatchVo> dispatches;

    /**
     * 作业工人（已接受派工的工人列表，只读展示用）。
     */
    private List<SfStaskDispatchVo> workWorkers;

    /**
     * 流转日志。
     */
    private List<SfStaskFlowLogVo> flowLogs;

    /**
     * 任务包下拆分工单摘要；生产管理员查看技术员已拆任务包且存在多条待验收拆单时有值。
     */
    private List<SfStaskWorkOrderVo> splitOrders;

    /**
     * 当前登录人可见任务的维语播报信息。
     * 组长、生产管理员、技术员和领导详情均可读取；无生成记录时返回 {@code null} 或未生成状态。
     */
    private SfStaskVoiceBroadcastVo voiceBroadcast;
}
