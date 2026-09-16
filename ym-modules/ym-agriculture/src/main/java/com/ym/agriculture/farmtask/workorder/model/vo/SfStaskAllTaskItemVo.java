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
 * stask 小程序全部任务统一列表项（任务包 PACKAGE / 拆分工单 SPLIT）。
 */
@Data
public class SfStaskAllTaskItemVo implements StaskI18nComposite {

    /**
     * 卡片类型：PACKAGE-任务包，SPLIT-拆分工单。
     */
    private String cardType;

    /**
     * 工单 ID；cardType=SPLIT 时有值。
     */
    private Long orderId;

    /**
     * 工单编号；cardType=SPLIT 时有值。
     */
    private String orderNo;

    /**
     * 任务包 ID；两类卡片均有值。
     */
    private Long packageId;

    /**
     * 计划作业日期，格式：yyyy-MM-dd。
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date planDate;

    /**
     * 任务状态编码。
     */
    private String status;

    /**
     * 状态展示文案。
     */
    private String statusLabel;

    /**
     * 主操作：EDIT-编辑，PROCESS-处理，ACCEPT-去验收。
     */
    private String primaryAction;

    /**
     * 是否可展示到岗打卡；组长派工完成且计划日期为当日时为 true。
     */
    private Boolean clockInEnabled;

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
     * 创建人角色编码，如 stask:production_admin、stask:expert。
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
     * 当前登录人是否与任务相关。
     */
    private Boolean relatedToCurrentUser;

    /**
     * 当前登录人是否可技术确认。
     */
    private Boolean canTechConfirm;

    /**
     * 当前登录人是否可技术退回。
     */
    private Boolean canTechReject;

    /**
     * 当前登录人是否可验收。
     */
    private Boolean canAcceptance;

    /**
     * 当前任务只读原因。
     */
    private String readonlyReason;

    /**
     * 任务包农事项总数；cardType=PACKAGE 时有值。
     */
    private Integer totalItemCount;

    /**
     * 任务包农事项摘要，最多返回 2 条；cardType=PACKAGE 时有值。
     */
    private List<SfStaskHomeItemSummaryVo> itemSummaries;

    /**
     * 任务包未展示农事项数量；cardType=PACKAGE 时有值。
     */
    private Integer moreItemCount;

    /**
     * 任务包关联大棚列表（去重）；cardType=PACKAGE 时有值，每项含 {@link SfStaskGreenhouseBriefVo#getPlantingBatches()}。
     */
    private List<SfStaskGreenhouseBriefVo> greenhouses;

    /**
     * 大棚 ID；cardType=SPLIT 时有值。
     */
    private Long greenhouseId;

    /**
     * 大棚名称快照；cardType=SPLIT 时有值。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "greenhouseNameSnapshot")
    private String greenhouseNameSnapshot;

    /**
     * 拆分工单对应大棚的进行中种植批次；cardType=SPLIT 时有值，无则为空数组。
     */
    private List<SfPlantingBatchVo> plantingBatches;

    /**
     * 农事项目 ID；cardType=SPLIT 时有值。
     */
    private Long workItemId;

    /**
     * 农事项目名称快照；cardType=SPLIT 时有值。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "workItemNameSnapshot")
    private String workItemNameSnapshot;

    /**
     * 拆分工单列表标题，格式：大棚名称 · 农事项目名称；cardType=SPLIT 时有值。
     */
    private String title;

    /**
     * 组长员工 ID；cardType=SPLIT 时有值。
     */
    private Long leaderId;

    /**
     * 组长姓名；cardType=SPLIT 时有值。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "leaderId", fieldKey = "name")
    private String leaderName;

    /**
     * 组长接单时填写的工人数量（不含组长）；cardType=SPLIT 时有值。
     */
    private Double requiredWorkerCount;

    /** 组长计划日用工人数。 */
    private java.math.BigDecimal dailyLaborCount;

    /** 当前组长和计划日对应的日用工记录乐观锁版本。 */
    private Long laborRecordVersion;

    /**
     * 已接受工人数；cardType=SPLIT 时有值。
     */
    private Integer acceptedWorkerCount;

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
     * 完工时间；待验收工单有值。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date completedAt;

    /**
     * 列表展示时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date eventTime;

    /**
     * 时间行前缀文案，如创建时间、提交时间、完成时间、验收时间。
     */
    private String eventTimeLabel;

    /**
     * 距发出任务小时数；待组长接单/待组长派工时有值，单位：小时。
     */
    private Long elapsedHours;

    /**
     * 创建时间；用于列表同日期内排序。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    /**
     * 使用本地化后的大棚与农事名称重建标题。
     */
    @Override
    public void rebuildLocalizedText() {
        title = StaskI18nTextCompositions.splitTitle(greenhouseNameSnapshot, workItemNameSnapshot);
    }
}
