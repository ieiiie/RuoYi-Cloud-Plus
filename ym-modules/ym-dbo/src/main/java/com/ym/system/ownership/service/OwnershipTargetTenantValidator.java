package com.ym.system.ownership.service;

import com.ym.common.core.exception.ServiceException;
import com.ym.system.ownership.model.OwnershipTenantVo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

/** Explicit physical SaaS datasource: validation cannot read the IoT or DBO database by accident. */
@Component
public class OwnershipTargetTenantValidator {
    private final JdbcTemplate tenants;
    public OwnershipTargetTenantValidator(@Qualifier("ownershipTenantJdbc") JdbcTemplate tenants) {
        this.tenants = tenants;
    }
    public List<OwnershipTenantVo> activeTenants() {
        return tenants.query("SELECT tenant_id,company_name FROM sys_tenant WHERE del_flag='0' AND status='0' AND (expire_time IS NULL OR expire_time>CURRENT_TIMESTAMP) ORDER BY tenant_id",
            (rs,n) -> new OwnershipTenantVo(rs.getString(1),rs.getString(2)));
    }
    public void requireActive(String tenantId) {
        if (tenantId == null || !tenantId.matches("[A-Za-z0-9_-]{1,20}"))
            throw new ServiceException("目标租户编号无效");
        Long found=tenants.queryForObject("SELECT COUNT(*) FROM sys_tenant WHERE tenant_id=? AND del_flag='0' AND status='0' AND (expire_time IS NULL OR expire_time>CURRENT_TIMESTAMP)",Long.class,tenantId);
        if (found == null || found != 1) throw new ServiceException("目标租户不存在、已停用或已过期");
    }
}
