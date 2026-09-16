package com.ym.agriculture.shared.dubbo.support;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.IntSupplier;

/** Dubbo 写命令幂等执行器。 */
@Component
@RequiredArgsConstructor
public class RemoteCommandIdempotencyExecutor {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 使用当前租户、动作和 requestId 唯一约束串行化重试。
     * 领域 Service 自身开启本地事务；幂等记录独立提交，因此失败原因不会随领域事务回滚。
     */
    public int execute(String action, String requestId, String businessId, IntSupplier command) {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("Dubbo 写命令缺少 tenantId");
        }
        if (StringUtils.isBlank(requestId)) {
            throw new ServiceException("Dubbo 写命令缺少 requestId");
        }
        String normalizedBusinessId = StringUtils.blankToDefault(businessId, requestId);
        try {
            jdbcTemplate.update("""
                INSERT INTO domain_command_idempotency
                    (id, tenant_id, action_name, business_id, request_id, status, retry_count, started_at, create_time)
                VALUES (?, ?, ?, ?, ?, 'RUNNING', 0, ?, ?)
                """, IdWorker.getId(), tenantId, action, normalizedBusinessId, requestId,
                LocalDateTime.now(), LocalDateTime.now());
        } catch (DuplicateKeyException duplicate) {
            Map<String, Object> previous = jdbcTemplate.queryForMap("""
                SELECT status, result_value FROM domain_command_idempotency
                WHERE tenant_id = ? AND action_name = ? AND request_id = ?
                """, tenantId, action, requestId);
            if ("SUCCEEDED".equals(previous.get("status"))) {
                Object result = previous.get("result_value");
                return result instanceof Number number ? number.intValue() : 1;
            }
            int claimed = jdbcTemplate.update("""
                UPDATE domain_command_idempotency
                SET status = 'RUNNING', retry_count = retry_count + 1,
                    business_id = ?, failure_reason = NULL, started_at = ?, finished_at = NULL
                WHERE tenant_id = ? AND action_name = ? AND request_id = ? AND status = 'FAILED'
                """, normalizedBusinessId, LocalDateTime.now(), tenantId, action, requestId);
            if (claimed == 0) {
                throw new ServiceException("相同 requestId 的命令正在执行");
            }
        }

        try {
            int result = command.getAsInt();
            jdbcTemplate.update("""
                UPDATE domain_command_idempotency
                SET status = 'SUCCEEDED', result_value = ?, finished_at = ?
                WHERE tenant_id = ? AND action_name = ? AND request_id = ?
                """, result, LocalDateTime.now(), tenantId, action, requestId);
            return result;
        } catch (RuntimeException ex) {
            jdbcTemplate.update("""
                UPDATE domain_command_idempotency
                SET status = 'FAILED', failure_reason = ?, finished_at = ?
                WHERE tenant_id = ? AND action_name = ? AND request_id = ?
                """, abbreviate(ex.getMessage()), LocalDateTime.now(), tenantId, action, requestId);
            throw ex;
        }
    }

    private static String abbreviate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
