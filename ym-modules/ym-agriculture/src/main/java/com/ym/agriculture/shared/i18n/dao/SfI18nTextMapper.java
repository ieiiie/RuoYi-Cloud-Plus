package com.ym.agriculture.shared.i18n.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.shared.i18n.model.constants.I18nTranslationOrigin;
import com.ym.agriculture.shared.i18n.model.constants.I18nTranslationStatus;
import com.ym.agriculture.shared.i18n.model.entity.SfI18nText;
import com.ym.agriculture.shared.i18n.model.vo.SfI18nTextVo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/** 租户级翻译词条 Mapper。 */
@Mapper
public interface SfI18nTextMapper extends BaseMapperPlus<SfI18nText, SfI18nTextVo> {

    String EXACT_SOURCE_SQL = "(BINARY source_text &lt;=&gt; BINARY VALUES(source_text))";
    String PROMOTE_SQL = "(" + EXACT_SOURCE_SQL + " AND translation_status &lt;&gt; 'SUCCESS'"
        + " AND VALUES(translation_status) = 'SUCCESS')";

    /**
     * 按 tenant_id + locale + source_hash 原子登记词条。
     *
     * <p>已有成功或人工译文不被普通登记覆盖；已删除的精确原文词条会恢复为有效。
     * 摘要命中后不改写原文，服务层会在写入后再次精确校验以防止哈希冲突。</p>
     */
    @InterceptorIgnore(tenantLine = "true")
    @Insert({
        "<script>",
        "INSERT INTO sf_i18n_text (",
        "translation_id, tenant_id, locale, source_text, source_hash, translated_text,",
        "translation_status, translation_origin, retry_count, next_retry_time, lease_until, processing_token,",
        "provider, last_error_code, last_error_message, create_by, create_time, update_by, update_time, del_flag",
        ") VALUES",
        "<foreach collection='rows' item='row' separator=','>",
        "(#{row.translationId}, #{row.tenantId}, #{row.locale}, #{row.sourceText}, #{row.sourceHash},",
        "#{row.translatedText}, #{row.translationStatus}, #{row.translationOrigin}, #{row.retryCount},",
        "#{row.nextRetryTime}, #{row.leaseUntil}, #{row.processingToken}, #{row.provider},",
        "#{row.lastErrorCode}, #{row.lastErrorMessage}, #{row.createBy}, #{row.createTime},",
        "#{row.updateBy}, #{row.updateTime}, #{row.delFlag})",
        "</foreach>",
        "ON DUPLICATE KEY UPDATE",
        "del_flag = IF(" + EXACT_SOURCE_SQL + ", '0', del_flag),",
        "translated_text = IF(" + PROMOTE_SQL + ", VALUES(translated_text), translated_text),",
        "translation_origin = IF(" + PROMOTE_SQL + ", VALUES(translation_origin), translation_origin),",
        "retry_count = IF(" + PROMOTE_SQL + ", VALUES(retry_count), retry_count),",
        "next_retry_time = IF(" + PROMOTE_SQL + ", VALUES(next_retry_time), next_retry_time),",
        "lease_until = IF(" + PROMOTE_SQL + ", VALUES(lease_until), lease_until),",
        "processing_token = IF(" + PROMOTE_SQL + ", VALUES(processing_token), processing_token),",
        "provider = IF(" + PROMOTE_SQL + ", VALUES(provider), provider),",
        "last_error_code = IF(" + PROMOTE_SQL + ", VALUES(last_error_code), last_error_code),",
        "last_error_message = IF(" + PROMOTE_SQL + ", VALUES(last_error_message), last_error_message),",
        "update_by = IF(" + PROMOTE_SQL + ", VALUES(update_by), update_by),",
        "update_time = IF(" + PROMOTE_SQL + ", VALUES(update_time), update_time),",
        // MySQL 按从左到右计算 ON DUPLICATE KEY UPDATE；状态必须最后更新，
        // 否则后续 PROMOTE_SQL 会看到已变为 SUCCESS 而跳过其他列。
        "translation_status = IF(" + PROMOTE_SQL + ", VALUES(translation_status), translation_status)",
        "</script>"
    })
    int upsertPhraseBatch(@Param("rows") Collection<SfI18nText> rows);

    /** 按摘要批量读取词条，包含逻辑删除行，原文由服务层再做字节精确校验。 */
    @InterceptorIgnore(tenantLine = "true")
    @Select({
        "<script>",
        "SELECT * FROM sf_i18n_text",
        "WHERE tenant_id = #{tenantId} AND locale = #{locale}",
        "AND source_hash IN",
        "<foreach collection='sourceHashes' item='sourceHash' open='(' separator=',' close=')'>",
        "#{sourceHash}",
        "</foreach>",
        "</script>"
    })
    List<SfI18nText> selectBySourceHashesRaw(@Param("tenantId") String tenantId,
        @Param("locale") String locale, @Param("sourceHashes") Collection<String> sourceHashes);

    default List<SfI18nText> selectBySourceHashes(String tenantId, String locale,
        Collection<String> sourceHashes) {
        if (sourceHashes == null || sourceHashes.isEmpty()) {
            return List.of();
        }
        return selectBySourceHashesRaw(tenantId, locale, sourceHashes);
    }

    default SfI18nText selectActiveByTenantAndId(String tenantId, Long translationId) {
        return selectOne(Wrappers.<SfI18nText>lambdaQuery()
            .eq(SfI18nText::getTenantId, tenantId)
            .eq(SfI18nText::getTranslationId, translationId)
            .eq(SfI18nText::getDelFlag, SystemConstants.NORMAL));
    }

    default List<SfI18nText> selectPendingForWorker(Date now, int maxAttempts, int limit) {
        return selectList(Wrappers.<SfI18nText>lambdaQuery()
            .eq(SfI18nText::getDelFlag, SystemConstants.NORMAL)
            .lt(SfI18nText::getRetryCount, maxAttempts)
            .and(w -> w.eq(SfI18nText::getTranslationStatus, I18nTranslationStatus.PENDING)
                .isNotNull(SfI18nText::getNextRetryTime)
                .le(SfI18nText::getNextRetryTime, now)
                .or(p -> p.eq(SfI18nText::getTranslationStatus, I18nTranslationStatus.PROCESSING)
                    .isNotNull(SfI18nText::getLeaseUntil)
                    .lt(SfI18nText::getLeaseUntil, now)))
            .orderByAsc(SfI18nText::getNextRetryTime)
            .orderByAsc(SfI18nText::getUpdateTime)
            .last("limit " + Math.max(1, limit)));
    }

    default boolean claim(String tenantId, Long translationId, String expectedSourceHash, Date now,
        Date leaseUntil, String token, int maxAttempts) {
        LambdaUpdateWrapper<SfI18nText> update = Wrappers.<SfI18nText>lambdaUpdate()
            .eq(SfI18nText::getTenantId, tenantId)
            .eq(SfI18nText::getTranslationId, translationId)
            .eq(SfI18nText::getSourceHash, expectedSourceHash)
            .eq(SfI18nText::getDelFlag, SystemConstants.NORMAL)
            .lt(SfI18nText::getRetryCount, maxAttempts)
            .and(w -> w.eq(SfI18nText::getTranslationStatus, I18nTranslationStatus.PENDING)
                .isNotNull(SfI18nText::getNextRetryTime)
                .le(SfI18nText::getNextRetryTime, now)
                .or(p -> p.eq(SfI18nText::getTranslationStatus, I18nTranslationStatus.PROCESSING)
                    .isNotNull(SfI18nText::getLeaseUntil)
                    .lt(SfI18nText::getLeaseUntil, now)))
            .set(SfI18nText::getTranslationStatus, I18nTranslationStatus.PROCESSING)
            .set(SfI18nText::getProcessingToken, token)
            .set(SfI18nText::getLeaseUntil, leaseUntil)
            .set(SfI18nText::getNextRetryTime, null)
            .set(SfI18nText::getLastErrorCode, null)
            .set(SfI18nText::getLastErrorMessage, null)
            .setSql("retry_count = retry_count + 1")
            .set(SfI18nText::getUpdateTime, now);
        return update(null, update) > 0;
    }

    default int expireExhaustedLeases(Date now, int maxAttempts) {
        return update(null, Wrappers.<SfI18nText>lambdaUpdate()
            .eq(SfI18nText::getDelFlag, SystemConstants.NORMAL)
            .eq(SfI18nText::getTranslationStatus, I18nTranslationStatus.PROCESSING)
            .isNotNull(SfI18nText::getLeaseUntil)
            .lt(SfI18nText::getLeaseUntil, now)
            .ge(SfI18nText::getRetryCount, maxAttempts)
            .set(SfI18nText::getTranslationStatus, I18nTranslationStatus.FAILED)
            .set(SfI18nText::getProcessingToken, null)
            .set(SfI18nText::getLeaseUntil, null)
            .set(SfI18nText::getNextRetryTime, null)
            .set(SfI18nText::getLastErrorCode, "LEASE_EXPIRED")
            .set(SfI18nText::getLastErrorMessage, "翻译处理租约已过期且重试次数已耗尽")
            .set(SfI18nText::getUpdateTime, now));
    }

    default boolean markSuccess(String tenantId, Long translationId, String token, String sourceHash,
        String translatedText, String provider, Date now) {
        return update(null, Wrappers.<SfI18nText>lambdaUpdate()
            .eq(SfI18nText::getTenantId, tenantId)
            .eq(SfI18nText::getTranslationId, translationId)
            .eq(SfI18nText::getProcessingToken, token)
            .eq(SfI18nText::getSourceHash, sourceHash)
            .eq(SfI18nText::getTranslationStatus, I18nTranslationStatus.PROCESSING)
            .eq(SfI18nText::getDelFlag, SystemConstants.NORMAL)
            .set(SfI18nText::getTranslatedText, translatedText)
            .set(SfI18nText::getTranslationStatus, I18nTranslationStatus.SUCCESS)
            .set(SfI18nText::getTranslationOrigin, I18nTranslationOrigin.MACHINE)
            .set(SfI18nText::getProvider, provider)
            .set(SfI18nText::getProcessingToken, null)
            .set(SfI18nText::getLeaseUntil, null)
            .set(SfI18nText::getNextRetryTime, null)
            .set(SfI18nText::getLastErrorCode, null)
            .set(SfI18nText::getLastErrorMessage, null)
            .set(SfI18nText::getUpdateTime, now)) > 0;
    }

    default boolean markFailure(String tenantId, Long translationId, String token, String sourceHash, int attempt,
        int maxAttempts, boolean retryable, Date nextRetryTime, String errorCode, String errorMessage, Date now) {
        boolean terminal = !retryable || attempt >= maxAttempts;
        String status = terminal ? I18nTranslationStatus.FAILED : I18nTranslationStatus.PENDING;
        return update(null, Wrappers.<SfI18nText>lambdaUpdate()
            .eq(SfI18nText::getTenantId, tenantId)
            .eq(SfI18nText::getTranslationId, translationId)
            .eq(SfI18nText::getProcessingToken, token)
            .eq(SfI18nText::getSourceHash, sourceHash)
            .eq(SfI18nText::getTranslationStatus, I18nTranslationStatus.PROCESSING)
            .eq(SfI18nText::getDelFlag, SystemConstants.NORMAL)
            .set(SfI18nText::getTranslationStatus, status)
            .set(SfI18nText::getNextRetryTime, terminal ? null : nextRetryTime)
            .set(SfI18nText::getProcessingToken, null)
            .set(SfI18nText::getLeaseUntil, null)
            .set(SfI18nText::getLastErrorCode, errorCode)
            .set(SfI18nText::getLastErrorMessage, errorMessage)
            .set(SfI18nText::getUpdateTime, now)) > 0;
    }

    /** 人工校正唯一词条，原文摘要为必填乐观锁。 */
    default boolean correctTranslation(String tenantId, Long translationId, String expectedSourceHash,
        String translatedText, Long updateBy, Date now) {
        if (expectedSourceHash == null || expectedSourceHash.isBlank()) {
            return false;
        }
        return update(null, Wrappers.<SfI18nText>lambdaUpdate()
            .eq(SfI18nText::getTenantId, tenantId)
            .eq(SfI18nText::getTranslationId, translationId)
            .eq(SfI18nText::getSourceHash, expectedSourceHash)
            .eq(SfI18nText::getDelFlag, SystemConstants.NORMAL)
            .set(SfI18nText::getTranslatedText, translatedText)
            .set(SfI18nText::getTranslationStatus, I18nTranslationStatus.SUCCESS)
            .set(SfI18nText::getTranslationOrigin, I18nTranslationOrigin.MANUAL)
            .set(SfI18nText::getRetryCount, 0)
            .set(SfI18nText::getNextRetryTime, null)
            .set(SfI18nText::getLeaseUntil, null)
            .set(SfI18nText::getProcessingToken, null)
            .set(SfI18nText::getProvider, "manual")
            .set(SfI18nText::getLastErrorCode, null)
            .set(SfI18nText::getLastErrorMessage, null)
            .set(SfI18nText::getUpdateBy, updateBy)
            .set(SfI18nText::getUpdateTime, now)) > 0;
    }

    default boolean resetForRetry(String tenantId, Long translationId, Long updateBy, Date now) {
        return update(null, Wrappers.<SfI18nText>lambdaUpdate()
            .eq(SfI18nText::getTenantId, tenantId)
            .eq(SfI18nText::getTranslationId, translationId)
            .eq(SfI18nText::getDelFlag, SystemConstants.NORMAL)
            .set(SfI18nText::getTranslatedText, null)
            .set(SfI18nText::getTranslationStatus, I18nTranslationStatus.PENDING)
            .set(SfI18nText::getTranslationOrigin, I18nTranslationOrigin.MACHINE)
            .set(SfI18nText::getRetryCount, 0)
            .set(SfI18nText::getNextRetryTime, now)
            .set(SfI18nText::getLeaseUntil, null)
            .set(SfI18nText::getProcessingToken, null)
            .set(SfI18nText::getProvider, null)
            .set(SfI18nText::getLastErrorCode, null)
            .set(SfI18nText::getLastErrorMessage, null)
            .set(SfI18nText::getUpdateBy, updateBy)
            .set(SfI18nText::getUpdateTime, now)) > 0;
    }
}
