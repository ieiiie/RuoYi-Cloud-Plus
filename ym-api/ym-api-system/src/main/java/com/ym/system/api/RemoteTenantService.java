package com.ym.system.api;

import com.ym.system.api.domain.vo.RemoteTenantInfoVo;
import java.util.List;

/**
 * SaaS 租户运行信息服务。
 */
public interface RemoteTenantService {

    /**
     * 查询租户绑定的 OSS 配置 ID。
     *
     * @param tenantId 租户编号
     * @return OSS 配置 ID
     */
    Long getOssConfigId(String tenantId);

    RemoteTenantInfoVo getTenant(String tenantId);

    List<RemoteTenantInfoVo> listActiveTenants();
}
