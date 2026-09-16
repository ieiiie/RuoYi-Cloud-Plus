package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.shared.i18n.StaskI18nComposite;
import com.ym.agriculture.farmtask.i18n.StaskI18nTextCompositions;
import lombok.Data;

import java.util.Date;

/**
 * 工人历史任务列表项视图对象。
 */
@Data
public class SfStaskWorkerHistoryItemVo implements StaskI18nComposite {

    /**
     * 派工明细 ID。
     */
    private Long dispatchId;

    /**
     * 工单 ID。
     */
    private Long orderId;

    /**
     * 列表标题：大棚名称 · 农事项目名称。
     */
    private String title;

    /**
     * 组长姓名。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "leaderId", fieldKey = "name")
    private String leaderName;

    /**
     * 组长员工 ID，仅用于服务端定位姓名翻译资源。
     */
    @JsonIgnore
    private Long leaderId;

    /**
     * 大棚名称快照。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "greenhouseNameSnapshot")
    private String greenhouseNameSnapshot;

    /**
     * 农事项目名称快照。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER,
        idProperty = "orderId", fieldKey = "workItemNameSnapshot")
    private String workItemNameSnapshot;

    /**
     * 计划作业日期，格式：yyyy-MM-dd。
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date planDate;

    /**
     * 计划日期相对文案，如：今天、昨天、3天前。
     */
    private String planDateLabel;

    /**
     * 验收通过时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date completedAt;

    /**
     * 组长评价编码，可能为空。
     */
    private String leaderEvaluation;

    /**
     * 组长评价展示文案，无评价时为「待评价」。
     */
    private String leaderEvaluationLabel;

    /**
     * 使用本地化后的大棚与农事名称重建标题。
     */
    @Override
    public void rebuildLocalizedText() {
        title = StaskI18nTextCompositions.splitTitle(greenhouseNameSnapshot, workItemNameSnapshot);
    }
}
