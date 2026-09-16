package com.ym.system.resource.dubbo;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import com.ym.common.core.constant.CacheNames;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.oss.factory.OssFactory;
import com.ym.common.redis.utils.CacheUtils;
import com.ym.resource.api.SaasResourceControlService;
import com.ym.system.resource.service.ISysOssConfigService;
import org.springframework.stereotype.Service;

/** 资源服务 OSS 配置热刷新。 */
@Service
@DubboService
@RequiredArgsConstructor
public class SaasResourceControlServiceImpl implements SaasResourceControlService {

    private final ISysOssConfigService ossConfigService;

    @Override
    public void refreshOssConfig(String configKey) {
        if (StringUtils.isNotBlank(configKey) && !"*".equals(configKey)) {
            OssFactory.remove(configKey);
            CacheUtils.evict(CacheNames.SYS_OSS_CONFIG, configKey);
        } else {
            OssFactory.clear();
            CacheUtils.clear(CacheNames.SYS_OSS_CONFIG);
        }
        ossConfigService.init();
    }
}
