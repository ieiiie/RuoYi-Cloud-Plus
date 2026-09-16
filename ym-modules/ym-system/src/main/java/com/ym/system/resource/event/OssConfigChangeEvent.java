package com.ym.system.resource.event;

/**
 * OSS 配置变更事件。
 *
 * @param configKey     当前配置 key
 * @param oldConfigKey  变更前配置 key
 * @param configJson    当前配置 JSON，为空表示清理缓存
 */
public record OssConfigChangeEvent(
    String configKey,
    String oldConfigKey,
    String configJson
) {

    /**
     * 创建保存 OSS 配置后的变更事件。
     *
     * @param configKey    当前配置 key
     * @param oldConfigKey 变更前配置 key
     * @param configJson   当前配置 JSON
     * @return OSS 配置变更事件
     */
    public static OssConfigChangeEvent save(String configKey, String oldConfigKey, String configJson) {
        return new OssConfigChangeEvent(configKey, oldConfigKey, configJson);
    }

    /**
     * 创建删除 OSS 配置后的变更事件。
     *
     * @param configKey 配置 key
     * @return OSS 配置变更事件
     */
    public static OssConfigChangeEvent remove(String configKey) {
        return new OssConfigChangeEvent(configKey, null, null);
    }

}
