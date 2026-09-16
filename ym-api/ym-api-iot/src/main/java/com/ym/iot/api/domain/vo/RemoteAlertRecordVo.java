package com.ym.iot.api.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/** JetLinks native alarm aggregate; IDs are opaque strings, times are epoch milliseconds. */
@Data
public class RemoteAlertRecordVo implements Serializable {
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
