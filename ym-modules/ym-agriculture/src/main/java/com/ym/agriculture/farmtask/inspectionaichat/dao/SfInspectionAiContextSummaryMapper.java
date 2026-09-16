package com.ym.agriculture.farmtask.inspectionaichat.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.inspectionaichat.model.entity.SfInspectionAiContextSummary;
import org.apache.ibatis.annotations.Mapper;

/** 巡查照片 AI 摘要数据访问。 */
@Mapper
public interface SfInspectionAiContextSummaryMapper extends BaseMapperPlus<SfInspectionAiContextSummary, SfInspectionAiContextSummary> {
    /** 读取当前可用的最新摘要。 */
    default SfInspectionAiContextSummary selectLatestCompleted(String tenantId, Long conversationId) {
        return selectOne(Wrappers.<SfInspectionAiContextSummary>lambdaQuery()
            .eq(SfInspectionAiContextSummary::getTenantId, tenantId)
            .eq(SfInspectionAiContextSummary::getConversationId, conversationId)
            .eq(SfInspectionAiContextSummary::getGenerationStatus, "COMPLETED")
            .orderByDesc(SfInspectionAiContextSummary::getCoveredThroughMessageSeq)
            .last("LIMIT 1"));
    }
}
