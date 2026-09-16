package com.ym.system.dubbo;

import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.system.api.RemoteTenantService;
import com.ym.system.api.domain.vo.RemoteTenantInfoVo;
import com.ym.system.domain.bo.SysTenantBo;
import com.ym.system.domain.vo.SysTenantVo;
import com.ym.system.service.ISysTenantService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SaaS 租户运行信息服务实现。
 */
@Service
@DubboService
@RequiredArgsConstructor
public class RemoteTenantServiceImpl implements RemoteTenantService {

    private final ISysTenantService tenantService;

    @Override
    public Long getOssConfigId(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("租户编号不能为空");
        }
        SysTenantVo tenant = tenantService.queryByTenantId(tenantId);
        if (tenant == null) {
            throw new ServiceException("租户不存在");
        }
        if (tenant.getOssConfigId() == null) {
            throw new ServiceException("租户未绑定OSS配置");
        }
        return tenant.getOssConfigId();
    }

    @Override
    public RemoteTenantInfoVo getTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return null;
        }
        SysTenantVo tenant = tenantService.queryByTenantId(tenantId.trim());
        return tenant == null ? null : BeanUtil.toBean(tenant, RemoteTenantInfoVo.class);
    }

    @Override
    public List<RemoteTenantInfoVo> listActiveTenants() {
        LocalDateTime now = LocalDateTime.now();
        SysTenantBo query = new SysTenantBo();
        query.setStatus("0");
        return tenantService.queryList(query).stream()
            .filter(tenant -> tenant.getExpireTime() == null || now.isBefore(tenant.getExpireTime()))
            .map(tenant -> BeanUtil.toBean(tenant, RemoteTenantInfoVo.class))
            .toList();
    }
}
