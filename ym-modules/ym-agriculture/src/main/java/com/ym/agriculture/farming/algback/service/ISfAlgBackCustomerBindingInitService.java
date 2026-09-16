package com.ym.agriculture.farming.algback.service;

/**
 * 租户创建后的算法中台绑定占位（幂等）。
 */
public interface ISfAlgBackCustomerBindingInitService {

    void ensurePendingBindingForTenant(String tenantId);

    /**
     * 中台自动建客户成功后回写 {@code customer_no} 并置为 {@code ACTIVE}；无绑定行时插入一行。
     */
    void applyCustomerNoFromMidPlatform(String tenantId, String customerNo);
}
