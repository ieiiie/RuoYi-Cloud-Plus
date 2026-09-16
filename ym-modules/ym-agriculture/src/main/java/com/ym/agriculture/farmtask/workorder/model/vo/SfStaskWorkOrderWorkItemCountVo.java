package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

/**
 * 工单具体农事数量聚合结果。
 */
@Data
public class SfStaskWorkOrderWorkItemCountVo {

    /**
     * 农事项目名称快照；空白名称由 SQL 归一为“未命名农事”。
     */
    private String workItemName;

    /**
     * 具体农事下的有效运营工单数量。
     */
    private long orderCount;
}
