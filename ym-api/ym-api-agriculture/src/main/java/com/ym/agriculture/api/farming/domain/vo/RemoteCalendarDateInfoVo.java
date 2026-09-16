package com.ym.agriculture.api.farming.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 公历时间对应的农历与节气信息。
 */
@Data
public class RemoteCalendarDateInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String gregorianDateTime;
    private String lunarDateText;
    private String solarTermCode;
    private String solarTermName;
    private String solarTermOccurredAt;
    private String timeZone;
    private String algorithmVersion;
}
