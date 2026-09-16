package com.ym.agriculture.farming.solarterms.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 下一节气摘要。 */
@Data
public class SfSolarTermSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String termCode;

    private String termName;

    private String occurredAt;

    private String gregorianDate;
}
