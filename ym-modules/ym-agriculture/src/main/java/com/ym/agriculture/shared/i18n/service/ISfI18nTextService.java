package com.ym.agriculture.shared.i18n.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.shared.i18n.model.I18nResourceResolution;
import com.ym.agriculture.shared.i18n.model.I18nTextLookup;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.vo.SfI18nTextVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 智慧农业业务文本翻译服务。 */
public interface ISfI18nTextService {

    /**
     * 在当前业务事务中登记待翻译中文文本。
     *
     * @param tenantId 租户编号
     * @param sources  待翻译文本
     */
    void registerTexts(String tenantId, Collection<I18nTextSource> sources);

    /**
     * 登记待翻译文本；对于新建或已变更的待办，优先写入已确认的术语译文。
     *
     * <p>用于将农事字典的当前维文复制到任务快照。既有人工校对不会被覆盖。</p>
     *
     * @param tenantId 当前租户编号
     * @param sources 待登记的中文字段
     * @param preferredTranslations 资源字段与优先译文的映射
     */
    void registerTextsWithPreferredTranslations(String tenantId, Collection<I18nTextSource> sources,
        Map<I18nTextSource, String> preferredTranslations);

    /**
     * 按资源类型、字段和中文原文批量查询成功维文。
     *
     * @param tenantId    租户编号
     * @param lookups 类型化查询条件
     * @return 查询条件与成功译文的映射
     */
    Map<I18nTextLookup, String> resolveUyghurTexts(String tenantId, Collection<I18nTextLookup> lookups);

    /**
     * 按业务资源中的当前中文查询租户级成功维文。
     *
     * @param tenantId 租户编号
     * @param sources  展示中的原始资源字段
     * @return 原始资源字段与成功译文的映射
     */
    Map<I18nTextSource, String> resolveUyghurTextsByResources(String tenantId, Collection<I18nTextSource> sources);

    /**
     * 解析成功维文，并登记尚不存在的租户级词条。
     *
     * <p>原文未变化的待处理、处理中和失败记录不会被重复登记；失败记录只能通过显式重试恢复。</p>
     *
     * @param tenantId 租户编号
     * @param sources 展示中的原始资源字段
     * @return 成功译文及本次尝试登记或恢复的唯一词条数量
     */
    I18nResourceResolution resolveAndRegisterMissingUyghurTextsByResources(String tenantId,
        Collection<I18nTextSource> sources);

    /**
     * 手动保存维文校对，并校验客户端看到的原文版本。
     *
     * @param tenantId 租户编号
     * @param translationId 翻译主键
     * @param translatedText 维文译文
     * @param sourceHash 客户端读取时的原文 SHA-256，必填
     * @return 成功时返回 true；记录不存在或原文已变化时返回 false
     */
    boolean correct(String tenantId, Long translationId, String translatedText, String sourceHash);

    /**
     * 将失败或已完成记录重新置为待翻译。
     *
     * @param tenantId      租户编号
     * @param translationId 翻译主键
     * @return 成功时返回 true
     */
    boolean retry(String tenantId, Long translationId);

    /**
     * 查询租户翻译运维记录。
     *
     * @param tenantId 租户编号
     * @param status 状态，可为空
     * @param sourceKeyword 中文关键字，可为空
     * @return 翻译记录
     */
    List<SfI18nTextVo> list(String tenantId, String status, String sourceKeyword);

    /**
     * 在数据库中分页查询租户翻译运维记录。
     *
     * @param tenantId 租户编号
     * @param status 状态，可为空
     * @param sourceKeyword 中文关键字，可为空
     * @param pageQuery 分页参数
     * @return 分页翻译记录
     */
    PageResult<SfI18nTextVo> page(String tenantId, String status, String sourceKeyword, PageQuery pageQuery);

    /**
     * 处理一轮待翻译记录。
     *
     * @return 实际领取数量
     */
    int processPending();
}
