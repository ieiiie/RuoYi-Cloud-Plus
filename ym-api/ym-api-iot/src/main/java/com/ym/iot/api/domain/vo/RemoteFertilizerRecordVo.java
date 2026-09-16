package com.ym.iot.api.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RemoteFertilizerRecordVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long recordId;
    private String tenantId;
    private Long deviceId;
    private String deviceCode;
    private Date recordTime;
    private Integer fertilizationType;
    private Integer fertilizationSeconds;
    private BigDecimal fertilizationQuantity;
    private String rawPayloadHex;
    private Date createTime;
}
