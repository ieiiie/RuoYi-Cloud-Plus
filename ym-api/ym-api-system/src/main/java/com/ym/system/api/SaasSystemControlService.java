package com.ym.system.api;

/** Dbo 提交 SaaS 运营数据后的系统服务控制契约。 */
public interface SaasSystemControlService {
    void refreshGlobalDict();
    void refreshTenantConfig(String tenantId);
    void invalidateTenantSessions(String tenantId);
}
