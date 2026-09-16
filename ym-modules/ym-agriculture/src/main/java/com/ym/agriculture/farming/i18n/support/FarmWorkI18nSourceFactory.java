package com.ym.agriculture.farming.i18n.support;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 农事字典翻译资源提取器。
 */
public final class FarmWorkI18nSourceFactory {

    private FarmWorkI18nSourceFactory() {
    }

    /**
     * 生成一个农事字典节点的可翻译中文字段。
     *
     * @param row 农事字典节点
     * @return 翻译资源
     */
    public static List<I18nTextSource> sources(SfFarmWorkDict row) {
        if (row == null || row.getDictId() == null) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        add(sources, row.getDictId(), "dictName", row.getDictName());
        add(sources, row.getDictId(), "remark", row.getRemark());
        return sources;
    }

    private static void add(List<I18nTextSource> sources, Long dictId, String fieldKey, String sourceText) {
        if (StringUtils.isNotBlank(sourceText)) {
            sources.add(new I18nTextSource(I18nResourceType.FARM_WORK_DICT, dictId, fieldKey, sourceText));
        }
    }
}
