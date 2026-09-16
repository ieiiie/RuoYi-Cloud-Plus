package com.ym.agriculture.farmtask.i18n.model.vo;

/**
 * 全租户农事字典维文预处理结果。
 *
 * @param tenantCount 参与预处理的有效租户数
 * @param dictCount 扫描到的有效农事字典记录数
 * @param sourceCount 提交幂等登记的非空可翻译候选资源数
 */
public record SfStaskFarmWorkDictPretranslateVo(
    int tenantCount,
    int dictCount,
    int sourceCount
) {
}
