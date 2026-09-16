package com.ym.resource.api;

/** Dbo 提交 OSS 配置后的资源服务控制契约。 */
public interface SaasResourceControlService {
    void refreshOssConfig(String configKey);
}
