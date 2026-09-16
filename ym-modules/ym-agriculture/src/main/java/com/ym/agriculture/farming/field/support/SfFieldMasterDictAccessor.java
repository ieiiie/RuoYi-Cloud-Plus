package com.ym.agriculture.farming.field.support;

import com.ym.common.core.utils.StringUtils;
import com.ym.system.api.RemoteDictService;
import com.ym.system.api.domain.vo.RemoteDictDataVo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 业务字典访问桥接，字典始终由平台系统服务管理。 */
@Component
public class SfFieldMasterDictAccessor {

    @DubboReference
    private RemoteDictService remoteDictService;

    public Map<String, String> getDictLabelMap(String dictType) {
        List<RemoteDictDataVo> rows = getDictDataList(dictType);
        return rows.stream()
            .filter(row -> StringUtils.isNotBlank(row.getDictValue()))
            .collect(Collectors.toMap(row -> row.getDictValue().trim(), RemoteDictDataVo::getDictLabel,
                (left, right) -> left));
    }

    public List<RemoteDictDataVo> getDictDataList(String dictType) {
        List<RemoteDictDataVo> rows = remoteDictService.selectDictDataByType(dictType);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
            .sorted(Comparator.comparing(RemoteDictDataVo::getDictSort,
                Comparator.nullsLast(Integer::compareTo)))
            .toList();
    }
}
