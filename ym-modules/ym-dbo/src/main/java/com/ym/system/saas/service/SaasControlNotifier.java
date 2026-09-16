package com.ym.system.saas.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import com.ym.resource.api.SaasResourceControlService;
import com.ym.system.api.SaasSystemControlService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

/**
 * 在 SaaS 运营数据事务提交后直接通知运行服务刷新状态。
 */
@Slf4j
@Component
public class SaasControlNotifier {

    @DubboReference(check = false, retries = 0)
    private SaasSystemControlService systemControlService;

    @DubboReference(check = false, retries = 0)
    private SaasResourceControlService resourceControlService;

    public void refreshGlobalDictAfterCommit() {
        afterCommit("刷新全局字典缓存", systemControlService::refreshGlobalDict);
    }

    public void refreshTenantConfigAfterCommit(Collection<String> tenantIds) {
        List<String> targets = List.copyOf(tenantIds);
        afterCommit("刷新租户参数缓存", () -> targets.forEach(systemControlService::refreshTenantConfig));
    }

    public void invalidateTenantSessionsAfterCommit(Collection<String> tenantIds) {
        List<String> targets = List.copyOf(tenantIds);
        afterCommit("清理租户授权会话", () -> targets.forEach(systemControlService::invalidateTenantSessions));
    }

    public void refreshOssConfigAfterCommit(String configKey) {
        afterCommit("刷新OSS配置", () -> resourceControlService.refreshOssConfig(configKey));
    }

    private void afterCommit(String actionName, Runnable action) {
        Runnable safeAction = () -> {
            try {
                action.run();
            } catch (Exception ex) {
                log.warn("SaaS运营数据已保存，但{}失败", actionName, ex);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeAction.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeAction.run();
            }
        });
    }
}
