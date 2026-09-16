package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.shared.i18n.StaskI18nComposite;
import com.ym.agriculture.farmtask.i18n.StaskI18nTextCompositions;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 工人首页任务卡片视图对象。
 */
@Data
public class SfStaskWorkerTaskItemVo implements StaskI18nComposite {

    /**
     * 派工明细 ID，接受/拒绝/查看说明时使用。
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
     * 大棚 ID。
     */
    private Long greenhouseId;

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
     * 计划作业日期，格式：yyyy-MM-dd。
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date planDate;

    /**
     * 计划日期相对文案，如：今天、明天、3天后。
     */
    private String planDateLabel;

    /**
     * 派工状态：PENDING-待确认，ACCEPTED-已接受。
     */
    private String dispatchStatus;

    /**
     * 主操作编码：PENDING 时为 ACCEPT；已接受时为 VIEW_INSTRUCTION。
     */
    private String primaryAction;

    /**
     * 是否为再次邀请（存在同工单历史拒绝/撤销记录）。
     */
    private Boolean reinvited;

    /**
     * 使用本地化后的大棚与农事名称重建标题。
     */
    @Override
    public void rebuildLocalizedText() {
        title = StaskI18nTextCompositions.splitTitle(greenhouseNameSnapshot, workItemNameSnapshot);
    }
}
