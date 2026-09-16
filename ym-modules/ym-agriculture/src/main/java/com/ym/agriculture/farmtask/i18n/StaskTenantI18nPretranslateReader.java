package com.ym.agriculture.farmtask.i18n;

import com.ym.common.core.utils.StringUtils;
import com.ym.system.api.RemoteTenantService;
import com.ym.system.api.domain.vo.RemoteTenantInfoVo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.List;

/** 查询可参与全量业务翻译预处理的租户。 */
@Component
public class StaskTenantI18nPretranslateReader {
    @DubboReference
    private RemoteTenantService remoteTenantService;

    public List<String> listActiveTenantIds() {
        return remoteTenantService.listActiveTenants().stream()
            .map(RemoteTenantInfoVo::getTenantId)
            .filter(StringUtils::isNotBlank)
            .distinct().sorted().toList();
    }
}
