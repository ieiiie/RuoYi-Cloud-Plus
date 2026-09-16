package com.ym.iot.api.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

@Data
public class RemoteFertilizerStateVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long deviceId;
    private String deviceCode;
    private String state;
    private Boolean online;
    private Float liquidLevel1;
    private Float liquidLevel2;
    private Float liquidLevel3;
}
