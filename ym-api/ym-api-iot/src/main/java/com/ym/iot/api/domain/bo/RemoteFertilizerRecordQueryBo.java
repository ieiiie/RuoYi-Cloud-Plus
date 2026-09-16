package com.ym.iot.api.domain.bo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class RemoteFertilizerRecordQueryBo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long deviceId;
    private String deviceCode;
    private Integer fertilizationType;
    private Date beginTime;
    private Date endTime;
    private Integer limit;
}
