package com.ym.agriculture.farmtask.yieldrecord.model.bo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 产量记录查询条件。
 */
@Data
public class SfStaskYieldQueryBo {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate harvestDateStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate harvestDateEnd;

    private Long speciesId;

    private Long varietyId;
}
