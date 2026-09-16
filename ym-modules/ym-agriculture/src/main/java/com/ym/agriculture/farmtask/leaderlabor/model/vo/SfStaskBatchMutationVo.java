package com.ym.agriculture.farmtask.leaderlabor.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/** 批量接单或批量打卡成功结果。 */
@Data
public class SfStaskBatchMutationVo {
    /** 成功处理数量。 */
    private Integer count;
    /** 成功处理工单ID。 */
    private List<Long> orderIds;
    /** 日用工记录ID，仅批量接单返回。 */
    private Long laborRecordId;
    /** 计划日期，仅批量接单返回。 */
    private LocalDate planDate;
    /** 日用工人数，仅批量接单返回。 */
    private BigDecimal dailyLaborCount;
    /** 日用工记录版本，仅批量接单返回。 */
    private Long version;
    /** 到岗时间，仅批量打卡返回。 */
    private Date arrivedAt;
}
