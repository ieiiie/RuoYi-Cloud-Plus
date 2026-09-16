package com.ym.agriculture.farmtask.yieldrecord.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 年度产量按品种汇总行。
 */
@Data
public class SfStaskAnnualYieldRowVo {

    private Long varietyId;

    private String varietyName;

    private BigDecimal yieldKg;
}
