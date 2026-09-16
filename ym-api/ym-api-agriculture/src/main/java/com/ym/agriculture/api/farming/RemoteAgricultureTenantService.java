package com.ym.agriculture.api.farming;

import com.ym.agriculture.api.farming.domain.bo.RemoteTenantInitializationBo;

/** 农业业务租户初始化契约。 */
public interface RemoteAgricultureTenantService {
    void initializeTenant(RemoteTenantInitializationBo command);
}
