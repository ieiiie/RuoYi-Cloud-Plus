package com.ym.common.translation.core.impl;

import cn.hutool.core.convert.Convert;
import lombok.AllArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import com.ym.common.core.utils.StreamUtils;
import com.ym.common.translation.annotation.TranslationType;
import com.ym.common.translation.constant.TransConstant;
import com.ym.common.translation.core.TranslationInterface;
import com.ym.resource.api.RemoteFileService;
import com.ym.resource.api.domain.RemoteFile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OSS翻译实现
 *
 * @author Lion Li
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.OSS_ID_TO_URL)
@ConditionalOnProperty(prefix = "ym.common.translation", name = "remote-resource-enabled", havingValue = "true", matchIfMissing = true)
public class OssUrlTranslationImpl implements TranslationInterface<String> {

    @DubboReference(mock = "true")
    private RemoteFileService remoteFileService;

    /**
     * 将 OSS ID 或 ID 集合翻译为访问地址。
     *
     * @param key   OSS ID 或逗号分隔的 ID 字符串
     * @param other 额外参数
     * @return 访问地址
     */
    @Override
    public String translation(Object key, String other) {
        return remoteFileService.selectUrlByIds(key.toString());
    }

    /**
     * 批量将 OSS ID 翻译为访问地址。
     *
     * @param keys  OSS ID 集合
     * @param other 额外参数
     * @return OSS ID 与访问地址映射
     */
    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        Set<Long> ossIds = collectLongIds(keys);
        if (ossIds.isEmpty()) {
            return Map.of();
        }
        String idText = ossIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        Map<Long, String> ossUrls = new LinkedHashMap<>(StreamUtils.toMap(remoteFileService.selectByIds(idText), RemoteFile::getOssId, RemoteFile::getUrl));
        Map<Object, String> result = new LinkedHashMap<>(keys.size());
        for (Object key : keys) {
            result.put(key, buildValue(key, ossUrls));
        }
        return result;
    }

    /**
     * 根据原始键构建 OSS 地址翻译值。
     *
     * @param source  原始键
     * @param ossUrls OSS ID 与访问地址映射
     * @return OSS 访问地址
     */
    private String buildValue(Object source, Map<Long, String> ossUrls) {
        if (source instanceof String ids) {
            return joinMappedValues(ids, ossUrls::get);
        }
        return source == null ? null : ossUrls.get(Convert.toLong(source));
    }

}
