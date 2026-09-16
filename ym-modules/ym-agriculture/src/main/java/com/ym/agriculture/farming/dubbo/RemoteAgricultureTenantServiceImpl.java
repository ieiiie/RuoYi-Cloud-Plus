package com.ym.agriculture.farming.dubbo;

import com.ym.agriculture.farming.algback.service.ISfAlgBackCustomerBindingInitService;
import com.ym.agriculture.farming.algback.service.ISfAlgBackTenantProvisionService;
import com.ym.agriculture.api.farming.RemoteAgricultureTenantService;
import com.ym.agriculture.api.farming.domain.bo.RemoteTenantInitializationBo;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

/** 农业业务租户初始化入口。 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class RemoteAgricultureTenantServiceImpl implements RemoteAgricultureTenantService {
    private final ISfAlgBackCustomerBindingInitService bindingInitService;
    private final ISfAlgBackTenantProvisionService algBackTenantProvisionService;

    @Override
    public void initializeTenant(RemoteTenantInitializationBo command) {
        if (command == null || StringUtils.isBlank(command.getTenantId())
            || StringUtils.isBlank(command.getRequestId()) || StringUtils.isBlank(command.getBusinessId())) {
            throw new ServiceException("租户初始化命令缺少幂等标识或租户编号");
        }
        String tenantId = command.getTenantId().trim();
        TenantHelper.dynamic(tenantId, () -> bindingInitService.ensurePendingBindingForTenant(tenantId));
        try {
            algBackTenantProvisionService.provisionMidCustomerIfEnabled(command);
        } catch (Exception ex) {
            log.warn("租户 {} 算法中台初始化失败，requestId={}: {}", tenantId, command.getRequestId(), ex.getMessage());
        }
    }
}
