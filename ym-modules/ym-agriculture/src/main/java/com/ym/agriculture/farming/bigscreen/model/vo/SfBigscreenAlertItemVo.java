package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/** JetLinks native alarm fields used by the business dashboard. */
@Data
public class SfBigscreenAlertItemVo implements Serializable {
    @Serial private static final long serialVersionUID = 2L;
    private String id;
    private String alarmConfigId;
    private String name;
    private Integer level;
    private String state;
    private Long alarmTime;
    private Long lastAlarmTime;
    private Long handleTime;
    private String targetId;
    private String targetName;
    private String targetType;
    private String actualDesc;
    private String handleType;
    private String handleState;
}
