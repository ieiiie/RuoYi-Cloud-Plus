package com.ym.agriculture.farming.weatheralert.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 预警列表项 / 详情。 */
@Data
public class SfWeatherAlertVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预警 ID，字符串，禁止按 Number 处理。 */
    private String warningId;

    private String title;

    private String headline;

    private String alertTypeCode;

    private String alertTypeName;

    private String levelCode;

    private String levelName;

    private String publisher;

    private String effectiveAt;

    private String expiresAt;

    private String areaText;

    private String description;

    private List<String> instructions = new ArrayList<>();

    private String sourceUrl;

    private String freshnessStatus;

    private Boolean active;
}
