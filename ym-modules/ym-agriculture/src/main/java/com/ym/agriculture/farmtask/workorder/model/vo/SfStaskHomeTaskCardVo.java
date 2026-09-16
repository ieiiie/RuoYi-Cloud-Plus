package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 生产管理员主页「待处理的任务」任务包卡片视图对象。
 */
@Data
public class SfStaskHomeTaskCardVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 卡片类型，固定为 PACKAGE。
     */
    private String cardType;

    /**
     * 工单 ID（任务包头 orderId，与 packageId 相同）。
     */
    private Long orderId;

    /**
     * 任务包 ID。
     */
    private Long packageId;

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
     * 状态展示文案，如：草稿、技术退回。
     */
    private String statusLabel;

    /**
     * 主操作编码：EDIT-编辑，PROCESS-处理。
     */
    private String primaryAction;

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
     * 当前登录人是否可验收。
     */
    private Boolean canAcceptance;

    /**
     * 当前任务只读原因。
     */
    private String readonlyReason;

    /**
     * 任务包农事项总数。
     */
    private Integer totalItemCount;

    /**
     * 任务包卡片展示的前若干条农事项摘要（最多 2 条）。
     */
    private List<SfStaskHomeItemSummaryVo> itemSummaries;

    /**
     * 未在 itemSummaries 中展示的农事项个数；大于 0 时前端展示「等其他 n 项任务」。
     */
    private Integer moreItemCount;

    /**
     * 任务包关联大棚列表（去重）；每项含进行中种植批次。
     */
    private List<SfStaskGreenhouseBriefVo> greenhouses;

    /**
     * 卡片辅助时间（创建/退回等）。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date eventTime;

    /**
     * 辅助时间标签，如：创建时间、退回时间。
     */
    private String eventTimeLabel;
}
