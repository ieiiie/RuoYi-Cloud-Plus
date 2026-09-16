package com.ym.agriculture.api.farming.domain.bo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

@Data
public class RemoteTenantInitializationBo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private String requestId;
    private String businessId;
    private String tenantId;
    private Long tenantDbId;
    private String companyName;
    private Long packageId;
    private String contactPhone;
    private String contactUserName;
    private String regionCode;
}
