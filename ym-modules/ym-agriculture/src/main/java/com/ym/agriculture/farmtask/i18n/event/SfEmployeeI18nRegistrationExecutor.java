package com.ym.agriculture.farmtask.i18n.event;

import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 在智慧农业数据源的新事务中登记人员姓名翻译资源。
 */
@Component
@RequiredArgsConstructor
public class SfEmployeeI18nRegistrationExecutor {

    private final ISfI18nTextService i18nTextService;

    /**
     * 登记人员姓名翻译待办。
     *
     * @param tenantId 租户编号
     * @param source 人员姓名翻译源
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void register(String tenantId, I18nTextSource source) {
        if (StringUtils.isBlank(tenantId) || source == null) {
            return;
        }
        TenantHelper.dynamic(tenantId, () -> i18nTextService.registerTexts(tenantId, List.of(source)));
    }
}
