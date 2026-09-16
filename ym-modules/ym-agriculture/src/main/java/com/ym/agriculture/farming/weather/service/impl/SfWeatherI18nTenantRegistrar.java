package com.ym.agriculture.farming.weather.service.impl;

import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 在独立业务库事务中为单个租户登记天气短语。 */
@Component
@RequiredArgsConstructor
public class SfWeatherI18nTenantRegistrar {

    private final ISfI18nTextService i18nTextService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void register(String tenantId, List<I18nTextSource> sources) {
        i18nTextService.registerTexts(tenantId, sources);
    }
}
