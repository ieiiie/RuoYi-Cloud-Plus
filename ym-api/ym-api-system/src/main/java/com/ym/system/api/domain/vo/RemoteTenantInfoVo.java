package com.ym.system.api.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RemoteTenantInfoVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long id;
    private String tenantId;
    private String companyName;
    private String address;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String regionName;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Long packageId;
    private Long ossConfigId;
    private LocalDateTime expireTime;
    private String status;
}
