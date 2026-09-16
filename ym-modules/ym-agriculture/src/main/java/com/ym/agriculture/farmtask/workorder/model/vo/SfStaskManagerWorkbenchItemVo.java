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
 * 生产管理员工作台统一列表项（任务包 PACKAGE / 拆单 SPLIT）。
 */
@Data
public class SfStaskManagerWorkbenchItemVo implements StaskI18nComposite {

    /**
     * 卡片类型：PACKAGE-任务包，SPLIT-拆分工单。
     */
    private String cardType;

    /**
     * 工单 ID。
     */
    private Long orderId;

    /**
     * 工单编号（拆单常用）。
     */
    private String orderNo;

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
     * 状态展示文案。
     */
    private String statusLabel;

    /**
     * 主操作：EDIT-编辑，PROCESS-处理，ACCEPT-去验收。
     */
    private String primaryAction;

    /**
     * 是否与当前登录人相关。
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
     * 创建人姓名（任务包卡片）。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "creatorEmployeeId", fieldKey = "name")
    private String creatorEmployeeName;

    /**
     * 创建人角色名称，对应 ym.employee.app-roles 配置中的 name（任务包卡片）。
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
     * 任务包农事项总数（PACKAGE）。
     */
    private Integer totalItemCount;

    /**
     * 农事项摘要，最多 2 条（PACKAGE）。
     */
    private List<SfStaskHomeItemSummaryVo> itemSummaries;

    /**
     * 未展示农事项个数（PACKAGE）。
     */
    private Integer moreItemCount;

    /**
     * 任务包关联大棚列表（PACKAGE）；每项含进行中种植批次。
     */
    private List<SfStaskGreenhouseBriefVo> greenhouses;

    /**
     * 大棚 ID（SPLIT）。
     */
    private Long greenhouseId;

    /**
     * 大棚名称快照（SPLIT）。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "greenhouseNameSnapshot")
    private String greenhouseNameSnapshot;

    /**
     * 拆分工单对应大棚的进行中种植批次（SPLIT）；无则为空数组。
     */
    private List<SfPlantingBatchVo> plantingBatches;

    /**
     * 农事项目名称快照（SPLIT）。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "workItemNameSnapshot")
    private String workItemNameSnapshot;

    /**
     * 农事项目 ID（SPLIT）。
     */
    private Long workItemId;

    /**
     * 拆分工单列表标题，格式：大棚名称 · 农事项目名称；仅 cardType=SPLIT 时有值。
     */
    private String title;

    /**
     * 组长员工 ID（SPLIT）。
     */
    private Long leaderId;

    /**
     * 组长姓名（SPLIT）。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "leaderId", fieldKey = "name")
    private String leaderName;

    /**
     * 创建人员工 ID（PACKAGE / SPLIT）。
     */
    private Long creatorEmployeeId;

    /**
     * 组长接单时填写的工人数量（不含组长，SPLIT）。
     */
    private Double requiredWorkerCount;

    /**
     * 已接受工人数（SPLIT）。
     */
    private Integer acceptedWorkerCount;

    /**
     * 验收结果：PASS-通过，REJECT-不通过（SPLIT 已完成）。
     */
    private String acceptanceResult;

    /**
     * 验收时间（SPLIT 已完成）。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date acceptedAt;

    /**
     * 完工时间（SPLIT 待验收）。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date completedAt;

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
     * 距发出任务小时数（SPLIT 待组长接单/派工）。
     */
    private Long elapsedHours;

    /**
     * 使用本地化后的大棚与农事名称重建标题。
     */
    @Override
    public void rebuildLocalizedText() {
        title = StaskI18nTextCompositions.splitTitle(greenhouseNameSnapshot, workItemNameSnapshot);
    }
}
