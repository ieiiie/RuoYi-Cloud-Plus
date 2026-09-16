package com.ym.agriculture.farming.weather.event;

import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.field.support.SfMasterTenantDistrictAccessor;
import com.ym.agriculture.farming.weather.service.impl.SfWeatherI18nTenantRegistrar;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 天气提交后按关联租户最佳努力登记翻译短语。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SfWeatherI18nSnapshotListener {

    private static final String FAILURE_METRIC = "smartfarming.weather.i18n.registration.failures";

    private final SfMasterTenantDistrictAccessor tenantDistrictAccessor;
    private final SfWeatherI18nTenantRegistrar tenantRegistrar;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommitted(SfWeatherI18nSnapshotCommittedEvent event) {
        if (event == null || StringUtils.isBlank(event.adcode()) || CollUtil.isEmpty(event.sources())) {
            return;
        }
        try {
            for (String tenantId : tenantDistrictAccessor.listActiveTenantIdsByAdcode(event.adcode())) {
                registerTenant(event, tenantId);
            }
        } catch (RuntimeException exception) {
            recordFailure("tenant_lookup", exception);
            log.warn("天气翻译关联租户读取失败，adcode={}，cause={}", event.adcode(),
                exception.getClass().getSimpleName());
        }
    }

    private void registerTenant(SfWeatherI18nSnapshotCommittedEvent event, String tenantId) {
        try {
            tenantRegistrar.register(tenantId, event.sources());
        } catch (RuntimeException exception) {
            recordFailure("tenant_registration", exception);
            log.warn("天气翻译词条登记失败，tenantId={}，adcode={}，sourceCount={}，cause={}",
                tenantId, event.adcode(), event.sources().size(), exception.getClass().getSimpleName());
        }
    }

    private void recordFailure(String stage, RuntimeException exception) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry != null) {
            registry.counter(FAILURE_METRIC, "stage", stage, "cause", exception.getClass().getSimpleName())
                .increment();
        }
    }
}
