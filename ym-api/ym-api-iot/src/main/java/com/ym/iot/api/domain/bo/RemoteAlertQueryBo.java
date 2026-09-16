package com.ym.iot.api.domain.bo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/** Native alarm filters; tenant visibility is supplied by the authenticated service. */
@Data
public class RemoteAlertQueryBo implements Serializable {
    @Serial private static final long serialVersionUID = 2L;
    private String id;
    private String alarmConfigId;
    private String targetId;
    private String name;
    private String state;
    private Integer level;
    private Long beginTime;
    private Long endTime;
    private Integer pageNum;
    private Integer pageSize;
}
