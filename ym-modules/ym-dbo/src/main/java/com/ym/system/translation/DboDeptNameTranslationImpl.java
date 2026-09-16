package com.ym.system.translation;

import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import com.ym.common.translation.annotation.TranslationType;
import com.ym.common.translation.constant.TransConstant;
import com.ym.common.translation.core.TranslationInterface;
import com.ym.system.api.DeptService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dbo 本地部门名称翻译。
 */
@Component
@RequiredArgsConstructor
@TranslationType(type = TransConstant.DEPT_ID_TO_NAME)
public class DboDeptNameTranslationImpl implements TranslationInterface<String> {

    private final DeptService deptService;

    @Override
    public String translation(Object key, String other) {
        return deptService.selectDeptNameByIds(key.toString());
    }

    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        Set<Long> deptIds = collectLongIds(keys);
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> deptNames = deptService.selectDeptNamesByIds(deptIds);
        Map<Object, String> result = new LinkedHashMap<>(keys.size());
        for (Object key : keys) {
            result.put(key, key instanceof String ids
                ? joinMappedValues(ids, deptNames::get)
                : deptNames.get(Convert.toLong(key)));
        }
        return result;
    }

}
