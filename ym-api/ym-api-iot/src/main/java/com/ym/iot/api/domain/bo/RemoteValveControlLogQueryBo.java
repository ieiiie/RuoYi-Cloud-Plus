package com.ym.iot.api.domain.bo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

@Data
public class RemoteValveControlLogQueryBo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long deviceId;
    private String deviceCode;
    private String commandType;
    private String status;
    private String beginTime;
    private String endTime;
    private Integer limit;
}
