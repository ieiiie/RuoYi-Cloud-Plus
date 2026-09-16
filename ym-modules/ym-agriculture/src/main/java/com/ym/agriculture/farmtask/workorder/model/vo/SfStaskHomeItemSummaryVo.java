package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 生产管理员主页任务包卡片：单条农事项摘要（按农事项统计大棚数）。
 */
@Data
public class SfStaskHomeItemSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务包农事明细主键，仅用于服务端定位翻译资源。
     */
    @JsonIgnore
    private Long itemId;

    /**
     * 农事项目 ID。
     */
    private Long workItemId;

    /**
     * 农事项目名称快照。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER_ITEM,
        idProperty = "itemId", fieldKey = "workItemNameSnapshot")
    private String workItemName;

    /**
     * 该农事项关联的大棚数量。
     */
    private Integer greenhouseCount;
}
