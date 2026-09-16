package com.ym.agriculture.farming.solarterms.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 当前节气 / 节气详情响应。
 * <p>本期不返回农事建议字段。
 */
@Data
public class SfSolarTermDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String termCode;

    private String termName;

    private Integer termOrder;

    private Integer termYear;

    private String occurredAt;

    private String gregorianDate;

    private String lunarDateText;

    private String intro;

    private String seasonalDescription;

    private List<String> customs = new ArrayList<>();

    private SfSolarTermSummaryVo nextTerm;
}
