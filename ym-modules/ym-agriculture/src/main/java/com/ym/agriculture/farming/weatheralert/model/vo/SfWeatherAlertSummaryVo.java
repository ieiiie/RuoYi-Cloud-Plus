package com.ym.agriculture.farming.weatheralert.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 四格入口预警摘要。 */
@Data
public class SfWeatherAlertSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean hasAlert;

    private int alertCount;

    private String highestLevelCode;

    private String highestLevelName;

    private String summaryTitle;

    private String latestEffectiveAt;

    /** FRESH / STALE / UNAVAILABLE */
    private String freshnessStatus;
}
