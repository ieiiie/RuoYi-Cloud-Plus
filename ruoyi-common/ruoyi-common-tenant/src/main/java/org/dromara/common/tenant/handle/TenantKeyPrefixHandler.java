package org.dromara.common.tenant.handle;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.handler.KeyPrefixHandler;
import org.dromara.common.tenant.helper.TenantHelper;

/**
 * 多租户 Redis Key 前缀处理器。
 *
 * @author Lion Li
 */
@Slf4j
public class TenantKeyPrefixHandler extends KeyPrefixHandler {

    public TenantKeyPrefixHandler(String keyPrefix) {
        super(keyPrefix);
    }

    @Override
    public String map(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        try {
            if (InterceptorIgnoreHelper.willIgnoreTenantLine("")) {
                return super.map(name);
            }
        } catch (NoClassDefFoundError ignore) {
            // 不依赖 MyBatis-Plus 的服务无需处理租户拦截器状态。
        }
        if (StringUtils.contains(name, GlobalConstants.GLOBAL_REDIS_KEY)) {
            return super.map(name);
        }
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            log.debug("无法获取有效的租户id -> Null");
            return super.map(name);
        }
        if (StringUtils.startsWith(name, tenantId + StringUtils.COLON)) {
            return super.map(name);
        }
        return super.map(tenantId + StringUtils.COLON + name);
    }

    @Override
    public String unmap(String name) {
        String unmap = super.unmap(name);
        if (StringUtils.isBlank(unmap)) {
            return null;
        }
        try {
            if (InterceptorIgnoreHelper.willIgnoreTenantLine("")) {
                return unmap;
            }
        } catch (NoClassDefFoundError ignore) {
            // 不依赖 MyBatis-Plus 的服务无需处理租户拦截器状态。
        }
        if (StringUtils.contains(name, GlobalConstants.GLOBAL_REDIS_KEY)) {
            return unmap;
        }
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            log.debug("无法获取有效的租户id -> Null");
            return unmap;
        }
        String tenantPrefix = tenantId + StringUtils.COLON;
        if (StringUtils.startsWith(unmap, tenantPrefix)) {
            return unmap.substring(tenantPrefix.length());
        }
        return unmap;
    }

}
