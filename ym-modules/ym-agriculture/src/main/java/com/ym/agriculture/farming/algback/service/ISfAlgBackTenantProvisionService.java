package com.ym.agriculture.farming.algback.service;

import com.ym.agriculture.api.farming.domain.bo.RemoteTenantInitializationBo;

/**
 * 租户创建后向算法中台自动建客户、启用并回写本地绑定。
 */
public interface ISfAlgBackTenantProvisionService {

    void provisionMidCustomerIfEnabled(RemoteTenantInitializationBo event);
}
