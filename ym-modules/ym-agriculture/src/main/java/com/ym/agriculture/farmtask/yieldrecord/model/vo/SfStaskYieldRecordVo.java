package com.ym.agriculture.farmtask.yieldrecord.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 产量记录展示对象。
 */
@Data
public class SfStaskYieldRecordVo {

    private Long yieldId;

    private LocalDate harvestDate;

    private Long speciesId;

    private Long varietyId;

    private String speciesName;

    private String varietyName;

    private BigDecimal yieldKg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
