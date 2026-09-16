package com.ym.agriculture.farmtask.leaderlabor.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 批量接单或打卡页面的任务行。 */
@Data
public class SfStaskBatchTaskVo {
    /** 工单ID。 */
    private Long orderId;
    /** 任务计划日期。 */
    private LocalDate planDate;
    /** 大棚ID。 */
    private Long greenhouseId;
    /** 大棚名称。 */
    private String greenhouseName;
    /** 作物展示名称。 */
    private String cropDisplayName;
    /** 农事项ID。 */
    private Long workItemId;
    /** 农事项名称。 */
    private String workItemName;
    /** 当前状态。 */
    private String status;
    /** 当前状态本地化展示文案。 */
    private String statusLabel;
    /** 该计划日用工人数，批量打卡任务有值。 */
    private BigDecimal dailyLaborCount;
}
