package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

/**
 * 工单分类数量聚合结果。
 */
@Data
public class SfStaskWorkOrderCategoryCountVo {

    /**
     * 工单分类名称快照；空白分类由 SQL 归一为“未分类”。
     */
    private String categoryName;

    /**
     * 分类下的有效运营工单数量。
     */
    private long orderCount;
}
