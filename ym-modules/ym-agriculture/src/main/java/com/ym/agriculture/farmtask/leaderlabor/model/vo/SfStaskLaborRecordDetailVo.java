package com.ym.agriculture.farmtask.leaderlabor.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/** 日用工记录详情。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SfStaskLaborRecordDetailVo extends SfStaskLaborRecordVo {
    /** 当日关联任务。 */
    private List<SfStaskBatchTaskVo> tasks;
}
