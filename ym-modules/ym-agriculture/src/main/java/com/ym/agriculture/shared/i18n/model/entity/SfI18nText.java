package com.ym.agriculture.shared.i18n.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;

/**
 * 智慧农业业务文本翻译，表 {@code sf_i18n_text}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_i18n_text")
public class SfI18nText extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 翻译主键。 */
    @TableId("translation_id")
    private Long translationId;

    /** 目标语言。 */
    private String locale;

    /** 中文原文快照。 */
    private String sourceText;

    /** 中文原文 SHA-256。 */
    private String sourceHash;

    /** 目标语言译文。 */
    private String translatedText;

    /** 处理状态。 */
    private String translationStatus;

    /** 译文来源。 */
    private String translationOrigin;

    /** 已尝试次数。 */
    private Integer retryCount;

    /** 下次自动重试时间。 */
    private Date nextRetryTime;

    /** Worker 租约截止时间。 */
    private Date leaseUntil;

    /** Worker 领取令牌。 */
    private String processingToken;

    /** 翻译提供商。 */
    private String provider;

    /** 最近失败错误码。 */
    private String lastErrorCode;

    /** 最近失败摘要。 */
    private String lastErrorMessage;

    /** 删除标志：0 存在，1 删除。 */
    @TableLogic
    private String delFlag;
}
