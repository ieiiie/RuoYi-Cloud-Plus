package com.ym.agriculture.farmtask.i18n.model.vo;

/**
 * 全租户 stask、农业引用数据及当前天气窗口业务文本预处理结果。
 *
 * @param tenantCount 参与预处理的租户数
 * @param resourceCount 扫描到的业务记录数
 * @param sourceTextCount 提取出的非空业务文本数，未按词条去重
 * @param uniquePhraseCount 本次扫描得到的租户级唯一词条数
 * @param createdPhraseCount 本次新登记或重新激活的词条数
 * @param existingPhraseCount 扫描前已存在的有效词条数
 */
public record SfStaskPretranslateVo(
    int tenantCount,
    long resourceCount,
    long sourceTextCount,
    long uniquePhraseCount,
    long createdPhraseCount,
    long existingPhraseCount
) {
}
