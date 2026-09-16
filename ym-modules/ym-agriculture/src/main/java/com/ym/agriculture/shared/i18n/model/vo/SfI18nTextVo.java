package com.ym.agriculture.shared.i18n.model.vo;

import com.ym.agriculture.shared.i18n.model.entity.SfI18nText;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.util.Date;

/** 业务文本翻译出参。 */
@Data
@AutoMapper(target = SfI18nText.class)
public class SfI18nTextVo {

    /** 翻译主键。 */
    private Long translationId;

    /** 租户编号。 */
    private String tenantId;

    /** 目标语言。 */
    private String locale;

    /** 中文原文。 */
    private String sourceText;

    /** 中文原文 SHA-256，用于人工校对的乐观校验。 */
    private String sourceHash;

    /** 维文译文。 */
    private String translatedText;

    /** 翻译状态。 */
    private String translationStatus;

    /** 翻译来源。 */
    private String translationOrigin;

    /** 已尝试次数。 */
    private Integer retryCount;

    /** 最近失败错误码。 */
    private String lastErrorCode;

    /** 最近失败摘要。 */
    private String lastErrorMessage;

    /** 下次重试时间。 */
    private Date nextRetryTime;

    /** 更新时间。 */
    private Date updateTime;
}
