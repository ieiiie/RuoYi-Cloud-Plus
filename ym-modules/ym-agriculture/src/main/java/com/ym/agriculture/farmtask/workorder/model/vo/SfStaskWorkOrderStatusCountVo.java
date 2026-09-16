package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

/**
 * 工单状态数量聚合结果。
 */
@Data
public class SfStaskWorkOrderStatusCountVo {

    /**
     * 工单当前状态。
     */
    private String status;

    /**
     * 当前状态下的工单数量。
     */
    private long orderCount;
}
