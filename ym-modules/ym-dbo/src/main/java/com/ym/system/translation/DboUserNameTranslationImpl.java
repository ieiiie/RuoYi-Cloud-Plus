package com.ym.system.translation;

import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.utils.StreamUtils;
import com.ym.common.translation.annotation.TranslationType;
import com.ym.common.translation.constant.TransConstant;
import com.ym.common.translation.core.TranslationInterface;
import com.ym.system.api.UserService;
import com.ym.system.api.domain.UserDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dbo 本地用户名翻译。
 */
@Component
@RequiredArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NAME)
public class DboUserNameTranslationImpl implements TranslationInterface<String> {

    private final UserService userService;

    @Override
    public String translation(Object key, String other) {
        return userService.selectUserNameById(Convert.toLong(key));
    }

    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        Set<Long> userIds = collectLongIds(keys);
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> userNames = StreamUtils.toMap(
            userService.selectListByIds(userIds), UserDTO::getUserId, UserDTO::getUserName);
        Map<Object, String> result = new LinkedHashMap<>(keys.size());
        for (Object key : keys) {
            result.put(key, key instanceof String ids
                ? joinMappedValues(ids, userNames::get)
                : userNames.get(Convert.toLong(key)));
        }
        return result;
    }

}
