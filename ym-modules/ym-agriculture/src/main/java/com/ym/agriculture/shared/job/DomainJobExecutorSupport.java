package com.ym.agriculture.shared.job;

import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.api.RemoteTenantService;
import com.ym.system.api.domain.vo.RemoteTenantInfoVo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tools.jackson.databind.json.JsonMapper;

/** 农业 Snail Job 的租户上下文执行边界。 */
@Component
@RequiredArgsConstructor
public class DomainJobExecutorSupport {

    public static final String SYSTEM_TENANT_ID = "000000";

    private final JsonMapper jsonMapper;

    @DubboReference
    private RemoteTenantService tenantService;

    /** 按参数中显式租户执行，保留给手工调度场景。 */
    public void execute(JobArgs jobArgs, Runnable action) {
        TenantJobParams params = parse(jobArgs);
        if (StringUtils.isBlank(params.getTenantId())) {
            throw new ServiceException("Snail Job 手工执行必须显式提供 tenantId");
        }
        TenantHelper.dynamic(params.getTenantId(), action);
    }

    /** 执行不受租户行隔离影响的全局任务。 */
    public void executeGlobal(Runnable action) {
        TenantHelper.ignore(action);
    }

    /** 在系统租户上下文执行任务。 */
    public void executeSystemTenant(Runnable action) {
        TenantHelper.dynamic(SYSTEM_TENANT_ID, action);
    }

    /**
     * 遍历全部有效租户。显式传入 tenantId 时仅执行该租户，便于手工补偿。
     * 单租户失败不中断后续租户，全部完成后统一向 SnailJob 报告失败。
     */
    public void executeAllTenants(JobArgs jobArgs, Runnable action) {
        TenantJobParams params = parse(jobArgs);
        if (StringUtils.isNotBlank(params.getTenantId())) {
            TenantHelper.dynamic(params.getTenantId(), action);
            return;
        }
        List<RuntimeException> failures = new ArrayList<>();
        for (String tenantId : listActiveTenantIds()) {
            try {
                TenantHelper.dynamic(tenantId, action);
            } catch (RuntimeException exception) {
                failures.add(exception);
            }
        }
        if (!failures.isEmpty()) {
            ServiceException aggregate = new ServiceException("Snail Job 执行失败租户数: " + failures.size());
            failures.forEach(aggregate::addSuppressed);
            throw aggregate;
        }
    }

    private List<String> listActiveTenantIds() {
        List<RemoteTenantInfoVo> tenants = tenantService.listActiveTenants();
        if (tenants == null || tenants.isEmpty()) {
            return List.of();
        }
        Set<String> tenantIds = new LinkedHashSet<>();
        for (RemoteTenantInfoVo tenant : tenants) {
            if (tenant != null && StringUtils.isNotBlank(tenant.getTenantId())) {
                tenantIds.add(tenant.getTenantId());
            }
        }
        return List.copyOf(tenantIds);
    }

    private TenantJobParams parse(JobArgs args) {
        Object raw = args == null ? null : args.getJobParams();
        if (raw == null) {
            return new TenantJobParams();
        }
        try {
            if (raw instanceof String text) {
                return StringUtils.isBlank(text)
                    ? new TenantJobParams() : jsonMapper.readValue(text, TenantJobParams.class);
            }
            return jsonMapper.convertValue(raw, TenantJobParams.class);
        } catch (RuntimeException exception) {
            throw new ServiceException("Snail Job 参数格式错误");
        }
    }

    @Data
    public static class TenantJobParams {
        private String tenantId;
    }
}
