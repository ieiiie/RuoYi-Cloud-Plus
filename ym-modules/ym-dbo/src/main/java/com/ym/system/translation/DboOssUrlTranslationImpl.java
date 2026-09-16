package com.ym.system.translation;

import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.utils.StreamUtils;
import com.ym.common.translation.annotation.TranslationType;
import com.ym.common.translation.constant.TransConstant;
import com.ym.common.translation.core.TranslationInterface;
import com.ym.system.api.OssService;
import com.ym.system.api.domain.OssDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dbo 本地 OSS 地址翻译。
 */
@Component
@RequiredArgsConstructor
@TranslationType(type = TransConstant.OSS_ID_TO_URL)
public class DboOssUrlTranslationImpl implements TranslationInterface<String> {

    private final OssService ossService;

    @Override
    public String translation(Object key, String other) {
        return ossService.selectUrlByIds(key.toString());
    }

    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        Set<Long> ossIds = collectLongIds(keys);
        if (ossIds.isEmpty()) {
            return Map.of();
        }
        String idText = ossIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        Map<Long, String> ossUrls = StreamUtils.toMap(
            ossService.selectByIds(idText), OssDTO::getOssId, OssDTO::getUrl);
        Map<Object, String> result = new LinkedHashMap<>(keys.size());
        for (Object key : keys) {
            result.put(key, key instanceof String ids
                ? joinMappedValues(ids, ossUrls::get)
                : ossUrls.get(Convert.toLong(key)));
        }
        return result;
    }

}
