package com.ym.common.translation.core.impl;

import cn.hutool.core.convert.Convert;
import lombok.AllArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import com.ym.common.core.constant.CacheNames;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.redis.utils.CacheUtils;
import com.ym.common.translation.annotation.TranslationType;
import com.ym.common.translation.constant.TransConstant;
import com.ym.common.translation.core.TranslationInterface;
import com.ym.system.api.RemoteUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.*;

/**
 * 用户昵称翻译实现
 *
 * @author may
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NICKNAME)
@ConditionalOnProperty(prefix = "ym.common.translation", name = "remote-system-enabled", havingValue = "true", matchIfMissing = true)
public class NicknameTranslationImpl implements TranslationInterface<String> {

    @DubboReference
    private RemoteUserService remoteUserService;

    /**
     * 将用户 ID 或 ID 集合翻译为用户昵称。
     *
     * @param key   用户 ID 或逗号分隔的 ID 字符串
     * @param other 额外参数
     * @return 用户昵称
     */
    @Override
    public String translation(Object key, String other) {
        if (key instanceof Long id) {
            String nickname = CacheUtils.get(CacheNames.SYS_NICKNAME, key);
            if (StringUtils.isNotBlank(nickname)) {
                return nickname;
            }
            return remoteUserService.selectNicknameById(id);
        } else if (key instanceof String ids) {
            List<String> list = new ArrayList<>();
            for (Long id : StringUtils.splitTo(ids, Convert::toLong)) {
                String nickname = CacheUtils.get(CacheNames.SYS_NICKNAME, id);
                if (StringUtils.isNotBlank(nickname)) {
                    list.add(nickname);
                } else {
                    list.add(remoteUserService.selectNicknameById(id));
                }
            }
            return StringUtils.joinComma(list);
        }
        return null;
    }

    /**
     * 批量将用户 ID 翻译为用户昵称。
     *
     * @param keys  用户 ID 集合
     * @param other 额外参数
     * @return 用户 ID 与用户昵称映射
     */
    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        Set<Long> userIds = collectLongIds(keys);
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> userNames = remoteUserService.selectUserNicksByIds(userIds);
        Map<Object, String> result = new LinkedHashMap<>(keys.size());
        for (Object key : keys) {
            result.put(key, buildValue(key, userNames));
        }
        return result;
    }

    /**
     * 根据原始键构建用户昵称翻译值。
     *
     * @param source    原始键
     * @param userNames 用户 ID 与用户昵称映射
     * @return 用户昵称
     */
    private String buildValue(Object source, Map<Long, String> userNames) {
        if (source instanceof String ids) {
            return joinMappedValues(ids, userNames::get);
        }
        return source == null ? null : userNames.get(Convert.toLong(source));
    }

}
