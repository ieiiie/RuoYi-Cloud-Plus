package com.ym.agriculture.farmtask.inspectionaichat.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.inspectionaichat.model.entity.SfInspectionAiConversation;
import org.apache.ibatis.annotations.Mapper;

/** 巡查照片 AI 会话数据访问。 */
@Mapper
public interface SfInspectionAiConversationMapper extends BaseMapperPlus<SfInspectionAiConversation, SfInspectionAiConversation> {
    /** 按租户读取会话。 */
    default SfInspectionAiConversation selectTenantById(String tenantId, Long conversationId) {
        return selectOne(Wrappers.<SfInspectionAiConversation>lambdaQuery()
            .eq(SfInspectionAiConversation::getTenantId, tenantId)
            .eq(SfInspectionAiConversation::getConversationId, conversationId)
            .last("LIMIT 1"));
    }
}
