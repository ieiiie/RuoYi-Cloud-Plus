package com.ym.agriculture.api.farming.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 下一节气摘要。
 */
@Data
public class RemoteSolarTermSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String termCode;
    private String termName;
    private String occurredAt;
    private String gregorianDate;
}
