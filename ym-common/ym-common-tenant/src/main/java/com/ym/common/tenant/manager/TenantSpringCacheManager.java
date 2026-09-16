package com.ym.common.tenant.manager;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import lombok.extern.slf4j.Slf4j;
import com.ym.common.core.constant.GlobalConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.redis.manager.PlusSpringCacheManager;
import com.ym.common.tenant.helper.TenantHelper;
import org.springframework.cache.Cache;

/**
 * 按租户隔离 Spring Cache 名称。
 *
 * @author Lion Li
 */
@Slf4j
public class TenantSpringCacheManager extends PlusSpringCacheManager {

    @Override
    public Cache getCache(String name) {
        if (InterceptorIgnoreHelper.willIgnoreTenantLine("")) {
            return super.getCache(name);
        }
        if (StringUtils.contains(name, GlobalConstants.GLOBAL_REDIS_KEY)) {
            return super.getCache(name);
        }
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            log.debug("无法获取有效的租户id，使用全局缓存名");
            return super.getCache(name);
        }
        String tenantPrefix = tenantId + StringUtils.COLON;
        if (StringUtils.startsWith(name, tenantPrefix)) {
            return super.getCache(name);
        }
        return super.getCache(tenantPrefix + name);
    }

}
