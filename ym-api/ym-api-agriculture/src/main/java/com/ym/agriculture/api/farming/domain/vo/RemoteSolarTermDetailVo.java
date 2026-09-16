package com.ym.agriculture.api.farming.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 小程序节气详情。
 */
@Data
public class RemoteSolarTermDetailVo implements Serializable {

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
    private RemoteSolarTermSummaryVo nextTerm;
}
