package com.ym.common.translation.core.impl;

import cn.hutool.core.convert.Convert;
import lombok.AllArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import com.ym.common.core.constant.CacheNames;
import com.ym.common.core.utils.StreamUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.redis.utils.CacheUtils;
import com.ym.common.translation.annotation.TranslationType;
import com.ym.common.translation.constant.TransConstant;
import com.ym.common.translation.core.TranslationInterface;
import com.ym.system.api.RemoteUserService;
import com.ym.system.api.domain.vo.RemoteUserVo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 用户名翻译实现
 *
 * @author Lion Li
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NAME)
@ConditionalOnProperty(prefix = "ym.common.translation", name = "remote-system-enabled", havingValue = "true", matchIfMissing = true)
public class UserNameTranslationImpl implements TranslationInterface<String> {

    @DubboReference
    private RemoteUserService remoteUserService;

    /**
     * 将用户 ID 翻译为用户名。
     *
     * @param key   用户 ID
     * @param other 额外参数
     * @return 用户名
     */
    @Override
    public String translation(Object key, String other) {
        Long userId = Convert.toLong(key);
        String username = CacheUtils.get(CacheNames.SYS_USER_NAME, userId);
        if (StringUtils.isNotBlank(username)) {
            return username;
        }
        return remoteUserService.selectUserNameById(userId);
    }

    /**
     * 批量将用户 ID 翻译为用户名。
     *
     * @param keys  用户 ID 集合
     * @param other 额外参数
     * @return 用户 ID 与用户名映射
     */
    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        Set<Long> userIds = collectLongIds(keys);
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> userNames = new LinkedHashMap<>(StreamUtils.toMap(remoteUserService.selectListByIds(userIds), RemoteUserVo::getUserId, RemoteUserVo::getUserName));
        Map<Object, String> result = new LinkedHashMap<>(keys.size());
        for (Object key : keys) {
            result.put(key, buildValue(key, userNames));
        }
        return result;
    }

    /**
     * 根据原始键构建用户名翻译值。
     *
     * @param source    原始键
     * @param userNames 用户 ID 与用户名映射
     * @return 用户名
     */
    private String buildValue(Object source, Map<Long, String> userNames) {
        if (source instanceof String ids) {
            return joinMappedValues(ids, userNames::get);
        }
        return userNames.get(Convert.toLong(source));
    }

}
