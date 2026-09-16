package com.ym.agriculture.farmtask.voice.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.voice.model.constants.StaskVoiceBroadcastStatus;
import com.ym.agriculture.farmtask.voice.model.entity.SfStaskVoiceBroadcast;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * stask 任务语音播报 Mapper。
 */
@Mapper
public interface SfStaskVoiceBroadcastMapper
    extends BaseMapperPlus<SfStaskVoiceBroadcast, SfStaskVoiceBroadcast> {

    default SfStaskVoiceBroadcast selectSuccessByOrderAndHash(String tenantId, Long orderId, List<String> languages,
        String textHash) {
        return selectOne(Wrappers.<SfStaskVoiceBroadcast>lambdaQuery()
            .eq(SfStaskVoiceBroadcast::getTenantId, tenantId)
            .eq(SfStaskVoiceBroadcast::getOrderId, orderId)
            .in(SfStaskVoiceBroadcast::getLanguage, languages)
            .eq(SfStaskVoiceBroadcast::getTextHash, textHash)
            .eq(SfStaskVoiceBroadcast::getStatus, StaskVoiceBroadcastStatus.SUCCESS)
            .last("limit 1"));
    }

    default SfStaskVoiceBroadcast selectByOrderAndHash(String tenantId, Long orderId, String language,
        String textHash) {
        return selectOne(Wrappers.<SfStaskVoiceBroadcast>lambdaQuery()
            .eq(SfStaskVoiceBroadcast::getTenantId, tenantId)
            .eq(SfStaskVoiceBroadcast::getOrderId, orderId)
            .eq(SfStaskVoiceBroadcast::getLanguage, language)
            .eq(SfStaskVoiceBroadcast::getTextHash, textHash)
            .last("limit 1"));
    }

    default SfStaskVoiceBroadcast selectLatestForOrder(String tenantId, Long orderId, List<String> languages,
        String textHash) {
        SfStaskVoiceBroadcast success = selectOne(Wrappers.<SfStaskVoiceBroadcast>lambdaQuery()
            .eq(SfStaskVoiceBroadcast::getTenantId, tenantId)
            .eq(SfStaskVoiceBroadcast::getOrderId, orderId)
            .in(SfStaskVoiceBroadcast::getLanguage, languages)
            .eq(SfStaskVoiceBroadcast::getTextHash, textHash)
            .eq(SfStaskVoiceBroadcast::getStatus, StaskVoiceBroadcastStatus.SUCCESS)
            .orderByDesc(SfStaskVoiceBroadcast::getUpdateTime)
            .last("limit 1"));
        if (success != null) {
            return success;
        }
        return selectOne(Wrappers.<SfStaskVoiceBroadcast>lambdaQuery()
            .eq(SfStaskVoiceBroadcast::getTenantId, tenantId)
            .eq(SfStaskVoiceBroadcast::getOrderId, orderId)
            .in(SfStaskVoiceBroadcast::getLanguage, languages)
            .eq(SfStaskVoiceBroadcast::getTextHash, textHash)
            .orderByDesc(SfStaskVoiceBroadcast::getUpdateTime)
            .last("limit 1"));
    }

    /**
     * 跨租户扫描待生成记录。
     *
     * <p>定时线程没有登录租户上下文，因此使用显式 SQL 和拦截器忽略注解，避免通用
     * {@code BaseMapper.selectList} 的 mapped statement 被租户插件收窄为空结果。</p>
    */
    @InterceptorIgnore(tenantLine = "true")
    @Results(id = "staskVoiceWorkerCandidateMap", value = {
        @Result(column = "broadcast_id", property = "broadcastId", id = true),
        @Result(column = "tenant_id", property = "tenantId")
    })
    @Select("""
        SELECT broadcast_id, tenant_id
        FROM sf_stask_voice_broadcast
        WHERE status <> 'SUCCESS'
          AND retry_count < #{maxAttempts}
          AND (
            status = 'QUEUED'
            OR (
              status = 'FAILED'
              AND (
                (retry_count = 1 AND update_time <= #{retryOneBefore})
                OR (retry_count = 2 AND update_time <= #{retryTwoBefore})
                OR (retry_count >= 3 AND update_time <= #{retryThreeBefore})
              )
            )
            OR (
              status IN ('PROCESSING', 'GENERATING')
              AND update_time < #{processingBefore}
            )
          )
        ORDER BY update_time ASC
        LIMIT #{limit}
        """)
    List<SfStaskVoiceBroadcast> selectPendingForWorker(@Param("processingBefore") Date processingBefore,
        @Param("retryOneBefore") Date retryOneBefore, @Param("retryTwoBefore") Date retryTwoBefore,
        @Param("retryThreeBefore") Date retryThreeBefore, @Param("maxAttempts") int maxAttempts,
        @Param("limit") int limit);

    /** 使用候选行携带的租户和主键进行跨租户原子领取。 */
    @InterceptorIgnore(tenantLine = "true")
    @Update("""
        UPDATE sf_stask_voice_broadcast
        SET status = 'PROCESSING',
            fail_reason = NULL,
            retry_count = retry_count + 1,
            update_time = #{now}
        WHERE broadcast_id = #{broadcastId}
          AND tenant_id = #{tenantId}
          AND retry_count < #{maxAttempts}
          AND (
            status = 'QUEUED'
            OR (
              status = 'FAILED'
              AND (
                (retry_count = 1 AND update_time <= #{retryOneBefore})
                OR (retry_count = 2 AND update_time <= #{retryTwoBefore})
                OR (retry_count >= 3 AND update_time <= #{retryThreeBefore})
              )
            )
            OR (
              status IN ('PROCESSING', 'GENERATING')
              AND update_time < #{processingBefore}
            )
          )
        """)
    int claimForProcessing(@Param("tenantId") String tenantId, @Param("broadcastId") Long broadcastId,
        @Param("processingBefore") Date processingBefore, @Param("retryOneBefore") Date retryOneBefore,
        @Param("retryTwoBefore") Date retryTwoBefore, @Param("retryThreeBefore") Date retryThreeBefore,
        @Param("maxAttempts") int maxAttempts, @Param("now") Date now);

    /**
     * 将已完成逐字段翻译的记录推进到语音生成阶段。
     *
     * <p>尝试次数同时充当无新增字段条件下的轻量租约令牌，避免旧 worker 覆盖新领取者。</p>
     */
    default boolean markGenerating(Long broadcastId, int attempt, String uyghurText, Date now) {
        LambdaUpdateWrapper<SfStaskVoiceBroadcast> update = Wrappers.<SfStaskVoiceBroadcast>lambdaUpdate()
            .eq(SfStaskVoiceBroadcast::getBroadcastId, broadcastId)
            .eq(SfStaskVoiceBroadcast::getStatus, StaskVoiceBroadcastStatus.PROCESSING)
            .eq(SfStaskVoiceBroadcast::getRetryCount, attempt)
            .set(SfStaskVoiceBroadcast::getStatus, StaskVoiceBroadcastStatus.GENERATING)
            .set(SfStaskVoiceBroadcast::getUyghurText, uyghurText)
            .set(SfStaskVoiceBroadcast::getUpdateTime, now);
        return update(null, update) > 0;
    }

    /** 使用当前尝试令牌写回成功结果。 */
    default boolean completeSuccess(Long broadcastId, int attempt, String uyghurText, Long ossId, String audioUrl,
        Date now) {
        LambdaUpdateWrapper<SfStaskVoiceBroadcast> update = Wrappers.<SfStaskVoiceBroadcast>lambdaUpdate()
            .eq(SfStaskVoiceBroadcast::getBroadcastId, broadcastId)
            .eq(SfStaskVoiceBroadcast::getStatus, StaskVoiceBroadcastStatus.GENERATING)
            .eq(SfStaskVoiceBroadcast::getRetryCount, attempt)
            .set(SfStaskVoiceBroadcast::getUyghurText, uyghurText)
            .set(SfStaskVoiceBroadcast::getOssId, ossId)
            .set(SfStaskVoiceBroadcast::getAudioUrl, audioUrl)
            .set(SfStaskVoiceBroadcast::getStatus, StaskVoiceBroadcastStatus.SUCCESS)
            .set(SfStaskVoiceBroadcast::getFailReason, null)
            .set(SfStaskVoiceBroadcast::getUpdateTime, now);
        return update(null, update) > 0;
    }

    /**
     * 使用当前尝试令牌写回失败结果；不可重试失败直接消耗全部尝试次数。
     */
    default boolean failAttempt(Long broadcastId, int attempt, int persistedAttempt, String stableReason, Date now) {
        LambdaUpdateWrapper<SfStaskVoiceBroadcast> update = Wrappers.<SfStaskVoiceBroadcast>lambdaUpdate()
            .eq(SfStaskVoiceBroadcast::getBroadcastId, broadcastId)
            .in(SfStaskVoiceBroadcast::getStatus, StaskVoiceBroadcastStatus.PROCESSING,
                StaskVoiceBroadcastStatus.GENERATING)
            .eq(SfStaskVoiceBroadcast::getRetryCount, attempt)
            .set(SfStaskVoiceBroadcast::getStatus, StaskVoiceBroadcastStatus.FAILED)
            .set(SfStaskVoiceBroadcast::getFailReason, stableReason)
            .set(SfStaskVoiceBroadcast::getRetryCount, persistedAttempt)
            .set(SfStaskVoiceBroadcast::getUpdateTime, now);
        return update(null, update) > 0;
    }

    /** 相同指纹的失败记录被业务再次触发时，从首次调用重新排队。 */
    default boolean resetForQueue(Long broadcastId, String sourceText, String voiceName, Date now) {
        LambdaUpdateWrapper<SfStaskVoiceBroadcast> update = Wrappers.<SfStaskVoiceBroadcast>lambdaUpdate()
            .eq(SfStaskVoiceBroadcast::getBroadcastId, broadcastId)
            .notIn(SfStaskVoiceBroadcast::getStatus, StaskVoiceBroadcastStatus.PROCESSING,
                StaskVoiceBroadcastStatus.GENERATING, StaskVoiceBroadcastStatus.SUCCESS)
            .set(SfStaskVoiceBroadcast::getSourceText, sourceText)
            .set(SfStaskVoiceBroadcast::getVoiceName, voiceName)
            .set(SfStaskVoiceBroadcast::getStatus, StaskVoiceBroadcastStatus.QUEUED)
            .set(SfStaskVoiceBroadcast::getFailReason, null)
            .set(SfStaskVoiceBroadcast::getRetryCount, 0)
            .set(SfStaskVoiceBroadcast::getOssId, null)
            .set(SfStaskVoiceBroadcast::getAudioUrl, null)
            .set(SfStaskVoiceBroadcast::getUpdateTime, now);
        return update(null, update) > 0;
    }

    /**
     * 将最后一次调用中崩溃遗留的过期租约收敛为终态，避免永久卡在处理中。
     */
    default int failExpiredExhausted(Date processingBefore, int maxAttempts, Date now, String stableReason) {
        LambdaUpdateWrapper<SfStaskVoiceBroadcast> update = Wrappers.<SfStaskVoiceBroadcast>lambdaUpdate()
            .in(SfStaskVoiceBroadcast::getStatus, StaskVoiceBroadcastStatus.PROCESSING,
                StaskVoiceBroadcastStatus.GENERATING)
            .ge(SfStaskVoiceBroadcast::getRetryCount, maxAttempts)
            .lt(SfStaskVoiceBroadcast::getUpdateTime, processingBefore)
            .set(SfStaskVoiceBroadcast::getStatus, StaskVoiceBroadcastStatus.FAILED)
            .set(SfStaskVoiceBroadcast::getFailReason, stableReason)
            .set(SfStaskVoiceBroadcast::getUpdateTime, now);
        return update(null, update);
    }
}
