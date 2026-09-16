package com.ym.agriculture.farmtask.inspectionaichat.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.inspectionaichat.model.entity.SfInspectionAiMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 巡查照片 AI 消息数据访问。 */
@Mapper
public interface SfInspectionAiMessageMapper extends BaseMapperPlus<SfInspectionAiMessage, SfInspectionAiMessage> {
    /** 查询会话内消息。 */
    default List<SfInspectionAiMessage> selectByConversation(String tenantId, Long conversationId) {
        return selectList(Wrappers.<SfInspectionAiMessage>lambdaQuery()
            .eq(SfInspectionAiMessage::getTenantId, tenantId)
            .eq(SfInspectionAiMessage::getConversationId, conversationId)
            .orderByAsc(SfInspectionAiMessage::getMessageSeq));
    }

    /** 批量查询会话消息，用于构建会话列表展示名称。 */
    default List<SfInspectionAiMessage> selectByConversationIds(String tenantId, List<Long> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) return List.of();
        return selectList(Wrappers.<SfInspectionAiMessage>lambdaQuery()
            .eq(SfInspectionAiMessage::getTenantId, tenantId)
            .in(SfInspectionAiMessage::getConversationId, conversationIds)
            .orderByAsc(SfInspectionAiMessage::getConversationId)
            .orderByAsc(SfInspectionAiMessage::getMessageSeq));
    }

    /** 查询同一用户的幂等请求。 */
    default SfInspectionAiMessage selectByRequestId(String tenantId, Long userId, String requestId) {
        return selectOne(Wrappers.<SfInspectionAiMessage>lambdaQuery()
            .eq(SfInspectionAiMessage::getTenantId, tenantId)
            .eq(SfInspectionAiMessage::getCreateBy, userId)
            .eq(SfInspectionAiMessage::getClientRequestId, requestId)
            .last("LIMIT 1"));
    }

    /** 查询指定用户问题当前显示的回答。 */
    default SfInspectionAiMessage selectLatestReply(String tenantId, Long userMessageId) {
        return selectOne(Wrappers.<SfInspectionAiMessage>lambdaQuery()
            .eq(SfInspectionAiMessage::getTenantId, tenantId)
            .eq(SfInspectionAiMessage::getReplyToMessageId, userMessageId)
            .ne(SfInspectionAiMessage::getGenerationStatus, "SUPERSEDED")
            .orderByDesc(SfInspectionAiMessage::getGenerationAttempt)
            .last("LIMIT 1"));
    }
}
